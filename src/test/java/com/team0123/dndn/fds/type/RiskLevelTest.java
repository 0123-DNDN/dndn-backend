package com.team0123.dndn.fds.type;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class RiskLevelTest {
    @Test void scoreBoundaries() {
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(14));
        assertEquals(RiskLevel.CAUTION, RiskLevel.fromScore(15));
        assertEquals(RiskLevel.CAUTION, RiskLevel.fromScore(24));
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(25));
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(34));
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(35));
    }
}
