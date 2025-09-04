package com.chessapp.api.metrics.service;

import java.util.Map;

public interface ObsProxyClient {
    Map<String, Object> promRange(String query, long start, long end, String step);
    Map<String, Object> lokiRange(String query, long start, long end, int limit, String direction);
}
