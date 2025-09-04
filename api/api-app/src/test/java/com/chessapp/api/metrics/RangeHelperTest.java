package com.chessapp.api.metrics;

import com.chessapp.api.metrics.service.RangeHelper;
import com.chessapp.api.metrics.service.RangeParams;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RangeHelperTest {
    @Test
    void mapRange() {
        RangeParams p = RangeHelper.mapRange("2h");
        assertTrue(p.end() > p.start());
        assertEquals("60s", p.step());
    }
}
