package com.chessapp.api.logs.api;

import com.chessapp.api.logs.api.dto.LogItemDto;
import com.chessapp.api.logs.api.dto.LogListDto;
import com.chessapp.api.metrics.service.ObsProxyClient;
import com.chessapp.api.metrics.service.RangeHelper;
import com.chessapp.api.metrics.service.RangeParams;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/logs")
public class LogsController {

    private final ObsProxyClient client;

    public LogsController(ObsProxyClient client) {
        this.client = client;
    }

    @GetMapping("/training/{runId}")
    public LogListDto training(@PathVariable String runId,
                               @RequestParam(defaultValue = "2h") String range,
                               @RequestParam(defaultValue = "500") int limit,
                               @RequestParam(defaultValue = "backward") String direction) {
        // promtail labels 'component' from JSON logs; ML uses component="training"
        String query = "{component=\"training\",run_id=\"" + runId + "\"}";
        return fetch(query, range, limit, direction);
    }

    @GetMapping("/app")
    public LogListDto appLogs(@RequestParam(name = "app", defaultValue = "api") String app,
                              @RequestParam(defaultValue = "1h") String range,
                              @RequestParam(defaultValue = "500") int limit,
                              @RequestParam(defaultValue = "backward") String direction) {
        String query = "{app=\"" + app + "\"}";
        return fetch(query, range, limit, direction);
    }

    private LogListDto fetch(String query, String range, int limit, String direction) {
        RangeParams rp = RangeHelper.mapRange(range);
        Map<String, Object> resp;
        try {
            resp = client.lokiRange(query, rp.start(), rp.end(), limit, direction);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "upstream error");
        }
        return toListDto(resp);
    }

    private static LogListDto toListDto(Map<String, Object> resp) {
        List<LogItemDto> items = new ArrayList<>();
        Object dataObj = (resp != null ? resp.get("data") : null);
        Map<?, ?> data = (dataObj instanceof Map<?, ?>) ? (Map<?, ?>) dataObj : Map.of();
        Object resultObj = data.containsKey("result") ? data.get("result") : List.of();
        List<?> result = (resultObj instanceof List<?>) ? (List<?>) resultObj : List.of();
        for (Object streamObj : result) {
            if (!(streamObj instanceof Map<?, ?>)) continue;
            Map<?, ?> stream = (Map<?, ?>) streamObj;
            Object metaObj = stream.containsKey("stream") ? stream.get("stream") : Map.of();
            Map<?, ?> streamMeta = (metaObj instanceof Map<?, ?>) ? (Map<?, ?>) metaObj : Map.of();
            Object valuesObj = stream.containsKey("values") ? stream.get("values") : List.of();
            List<?> values = (valuesObj instanceof List<?>) ? (List<?>) valuesObj : List.of();
            String level = String.valueOf(streamMeta.containsKey("level") ? streamMeta.get("level") : "INFO");
            for (Object one : values) {
                if (!(one instanceof List<?>)) continue;
                List<?> v = (List<?>) one;
                if (v.size() < 2) continue;
                String tsStr = String.valueOf(v.get(0));
                long ns;
                try {
                    ns = Long.parseLong(tsStr);
                } catch (Exception ex) {
                    continue;
                }
                String msg = String.valueOf(v.get(1));
                items.add(new LogItemDto(Instant.ofEpochMilli(ns / 1_000_000).toString(), level, msg));
            }
        }
        return new LogListDto(items);
    }
}
