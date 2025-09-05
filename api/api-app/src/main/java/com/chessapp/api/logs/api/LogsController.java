package com.chessapp.api.logs.api;

import com.chessapp.api.logs.api.dto.*;
import com.chessapp.api.metrics.service.ObsProxyClient;
import com.chessapp.api.metrics.service.RangeHelper;
import com.chessapp.api.metrics.service.RangeParams;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/v1/logs")
public class LogsController {

    private final ObsProxyClient client;
    public LogsController(ObsProxyClient client) { this.client = client; }

    @GetMapping("/training/{runId}")
    public LogListDto training(@PathVariable String runId,
                               @RequestParam(defaultValue="2h") String range,
                               @RequestParam(defaultValue="500") int limit,
                               @RequestParam(defaultValue="backward") String direction) {
        RangeParams rp = RangeHelper.mapRange(range);
        String q = "{app=\"trainer\",run_id=\""+runId+"\"}";
        Map<String,Object> resp;
        try {
            resp = client.lokiRange(q, rp.start(), rp.end(), limit, direction);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "upstream error");
        }
        List<LogItemDto> items = new ArrayList<>();
        Map<String,Object> data = (Map<String,Object>) resp.getOrDefault("data", Map.of());
        List<Map<String,Object>> result = (List<Map<String,Object>>) data.getOrDefault("result", List.of());
        for (Map<String,Object> stream : result) {
            Map<String,Object> streamMeta = (Map<String,Object>) stream.getOrDefault("stream", Map.of());
            String level = String.valueOf(streamMeta.getOrDefault("level", "INFO"));
            List<List<Object>> values = (List<List<Object>>) stream.getOrDefault("values", List.of());
            for (List<Object> v : values) {
                String tsStr = String.valueOf(v.get(0));
                long ns = Long.parseLong(tsStr);
                String msg = String.valueOf(v.get(1));
                items.add(new LogItemDto(Instant.ofEpochMilli(ns/1_000_000).toString(), level, msg));
            }
        }
        return new LogListDto(items);
    }

    @GetMapping("/app")
    public LogListDto appLogs(@RequestParam(name = "app", defaultValue = "api") String app,
                               @RequestParam(defaultValue="1h") String range,
                               @RequestParam(defaultValue="500") int limit,
                               @RequestParam(defaultValue="backward") String direction) {
        RangeParams rp = RangeHelper.mapRange(range);
        // Assumes logs are labeled with {app="<name>"}
        String q = "{app=\"" + app + "\"}";
        Map<String,Object> resp;
        try {
            resp = client.lokiRange(q, rp.start(), rp.end(), limit, direction);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "upstream error");
        }
        List<LogItemDto> items = new ArrayList<>();
        Map<String,Object> data = (Map<String,Object>) resp.getOrDefault("data", Map.of());
        List<Map<String,Object>> result = (List<Map<String,Object>>) data.getOrDefault("result", List.of());
        for (Map<String,Object> stream : result) {
            Map<String,Object> streamMeta = (Map<String,Object>) stream.getOrDefault("stream", Map.of());
            String level = String.valueOf(streamMeta.getOrDefault("level", "INFO"));
            List<List<Object>> values = (List<List<Object>>) stream.getOrDefault("values", List.of());
            for (List<Object> v : values) {
                String tsStr = String.valueOf(v.get(0));
                long ns = Long.parseLong(tsStr);
                String msg = String.valueOf(v.get(1));
                items.add(new LogItemDto(Instant.ofEpochMilli(ns/1_000_000).toString(), level, msg));
            }
        }
        return new LogListDto(items);
    }}\r\n
