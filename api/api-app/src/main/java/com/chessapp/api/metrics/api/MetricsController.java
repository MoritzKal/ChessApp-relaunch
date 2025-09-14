package com.chessapp.api.metrics.api;

import com.chessapp.api.common.dto.*;
import com.chessapp.api.metrics.service.ObsProxyClient;
import com.chessapp.api.training.service.TrainingService;
import com.chessapp.api.metrics.service.RangeHelper;
import com.chessapp.api.metrics.service.RangeParams;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1/metrics")
public class MetricsController {

    private final ObsProxyClient client;
    private final TrainingService trainingService;
    public MetricsController(ObsProxyClient client, TrainingService trainingService) {
        this.client = client;
        this.trainingService = trainingService;
    }

    private Series toSeries(Map<String,Object> prom, String label) {
        @SuppressWarnings("unchecked")
        Map<String,Object> data = (Map<String,Object>) prom.getOrDefault("data", Map.of());
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> res = (List<Map<String,Object>>) data.getOrDefault("result", List.of());
        List<TimeSeriesPoint> pts = new ArrayList<>();
        if (!res.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<List<Object>> values = (List<List<Object>>) res.get(0).getOrDefault("values", List.of());
            for (List<Object> v : values) {
                long ts = ((Number) v.get(0)).longValue();
                double val = Double.parseDouble(String.valueOf(v.get(1)));
                pts.add(new TimeSeriesPoint(ts, val));
            }
        }
        return new Series(label, pts);
    }

    private double lastValue(Map<String,Object> prom) {
        @SuppressWarnings("unchecked")
        Map<String,Object> data = (Map<String,Object>) prom.getOrDefault("data", Map.of());
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> res = (List<Map<String,Object>>) data.getOrDefault("result", List.of());
        if (!res.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<List<Object>> values = (List<List<Object>>) res.get(0).getOrDefault("values", List.of());
            if (!values.isEmpty()) {
                List<Object> last = values.get(values.size()-1);
                return Double.parseDouble(String.valueOf(last.get(1)));
            }
        }
        return 0.0;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        // simple stubbed health metrics
        return Map.of(
                "status", "ok",
                "pingMs", 0,
                "errorRate", 0.0
        );
    }

    @GetMapping("/throughput")
    public SeriesResponse throughput(@RequestParam(required = false) String runId,
                                     @RequestParam(defaultValue = "24h") String range) {
        RangeParams rp = RangeHelper.mapRange(range);
        String q;
        if (runId != null) {
            // it/s = rate(count) / rate(sum); avoid NaN/Inf with clamp_min on denominator
            q = "( rate(chs_training_step_duration_seconds_count{run_id=\\\"" + runId + "\\\"}[5m]) ) / clamp_min(rate(chs_training_step_duration_seconds_sum{run_id=\\\"" + runId + "\\\"}[5m]), 1e-9)";
        } else {
            q = "sum( rate(chs_training_step_duration_seconds_count[5m]) ) / clamp_min(sum(rate(chs_training_step_duration_seconds_sum[5m])), 1e-9)";
        }
        Series s = toSeries(client.promRange(q, rp.start(), rp.end(), rp.step()), "throughput_it_per_sec");
        return new SeriesResponse(List.of(s));
    }

    @GetMapping("/training/{runId}")
    public SeriesResponse training(@PathVariable String runId,
                                   @RequestParam String m,
                                   @RequestParam(defaultValue = "24h") String range) {
        RangeParams rp = RangeHelper.mapRange(range);
        List<Series> out = new ArrayList<>();
        for (String metric : m.split(",")) {
            String q = null;
            String label = null;
            switch (metric.trim()) {
                case "loss" -> { q = "avg_over_time(chs_training_loss{run_id=\\\""+runId+"\\\"}[5m])"; label = "loss"; }
                case "val_acc" -> { q = "avg_over_time(chs_training_val_accuracy{run_id=\\\""+runId+"\\\"}[5m])"; label = "val_acc"; }
                default -> { /* unrecognized metric: skip */ }
            }
            if (q == null || label == null) {
                continue;
            }
            Series s = toSeries(client.promRange(q, rp.start(), rp.end(), rp.step()), label);
            if (s.points().isEmpty()) {
                // Fallback: single data point from training status when Prometheus has no data
                try {
                    java.util.UUID rid = java.util.UUID.fromString(runId);
                    Map<String, Object> st = trainingService.getStatus(rid);
                    Object metrics = st.get("metrics");
                    Object at = st.get("updatedAt");
                    if (metrics instanceof Map<?,?> mm && at != null) {
                        Object val = mm.get(label);
                        if (val instanceof Number) {
                            long ts = java.time.Instant.parse(String.valueOf(at)).getEpochSecond();
                            s = new Series(label, java.util.List.of(new TimeSeriesPoint(ts, ((Number) val).doubleValue())));
                        }
                    }
                } catch (Exception ignored) { /* keep empty series */ }
            }
            out.add(s);
        }
        return new SeriesResponse(out);
    }

    @GetMapping("/utilization")
    public SeriesResponse util(@RequestParam(required=false) String runId,
                               @RequestParam(defaultValue="24h") String range) {
        RangeParams rp = RangeHelper.mapRange(range);
        List<Series> out = new ArrayList<>();
        String cpuQ = "avg_over_time(node_cpu_seconds_total{mode!=\"idle\"}[5m])";
        out.add(toSeries(client.promRange(cpuQ, rp.start(), rp.end(), rp.step()), "cpu_pct"));
        if (runId != null) {
            String gpuQ = "avg_over_time(chs_gpu_utilization_pct{run_id=\""+runId+"\"}[5m])";
            String memQ = "avg_over_time(chs_gpu_mem_used_pct{run_id=\""+runId+"\"}[5m])";
            out.add(toSeries(client.promRange(gpuQ, rp.start(), rp.end(), rp.step()), "gpu_pct"));
            out.add(toSeries(client.promRange(memQ, rp.start(), rp.end(), rp.step()), "vram_pct"));
        } else {
            out.add(new Series("gpu_pct", List.of()));
            out.add(new Series("vram_pct", List.of()));
        }
        return new SeriesResponse(out);
    }

    @GetMapping("/latency")
    public SingleValueDto latency(@RequestParam int p) {
        String q = "histogram_quantile(0." + p + ", sum by (le) (rate(http_server_requests_seconds_bucket[5m]))) * 1000";
        long end = java.time.Instant.now().getEpochSecond();
        long start = end - 300;
        double v = lastValue(client.promRange(q, start, end, "300s"));
        return new SingleValueDto(v, "ms");
    }

    @GetMapping("/mps")
    public SeriesResponse mps(@RequestParam(defaultValue="24h") String range) {
        RangeParams rp = RangeHelper.mapRange(range);
        String q = "sum(rate(chs_moves_total[1m]))";
        Series s = toSeries(client.promRange(q, rp.start(), rp.end(), rp.step()), "mps");
        return new SeriesResponse(List.of(s));
    }

    @GetMapping("/rps")
    public SeriesResponse rps(@RequestParam(defaultValue="24h") String range) {
        RangeParams rp = RangeHelper.mapRange(range);
        String q = "sum(rate(http_server_requests_seconds_count[5m]))";
        Series s = toSeries(client.promRange(q, rp.start(), rp.end(), rp.step()), "rps");
        return new SeriesResponse(List.of(s));
    }

    @GetMapping("/error_rate")
    public SeriesResponse errorRate(@RequestParam(defaultValue="24h") String range) {
        RangeParams rp = RangeHelper.mapRange(range);
        String q = "sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])) / clamp_min(sum(rate(http_server_requests_seconds_count[5m])), 1e-9)";
        Series s = toSeries(client.promRange(q, rp.start(), rp.end(), rp.step()), "error_rate");
        return new SeriesResponse(List.of(s));
    }

    @GetMapping("/elo")
    public SeriesResponse elo(@RequestParam(defaultValue="30d") String range) {
        RangeParams rp = RangeHelper.mapRange(range);
        String q = "avg_over_time(chs_engine_elo[1h])";
        Series s = toSeries(client.promRange(q, rp.start(), rp.end(), rp.step()), "elo");
        return new SeriesResponse(List.of(s));
    }
}
