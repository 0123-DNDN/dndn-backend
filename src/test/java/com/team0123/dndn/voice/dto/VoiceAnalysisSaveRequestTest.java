package com.team0123.dndn.voice.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoiceAnalysisSaveRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void textOnlyRequestPassesValidation() {
        assertViolationCount(
                new VoiceAnalysisSaveRequest("오늘 산책했어요.", null, null, null),
                0
        );
    }

    @Test
    void blankTextFailsValidation() {
        assertViolationCount(new VoiceAnalysisSaveRequest(" ", 1.0, 1.0, 1.0), 1);
    }

    @Test
    void nullTextFailsValidation() {
        assertViolationCount(new VoiceAnalysisSaveRequest(null, 1.0, 1.0, 1.0), 1);
    }

    @Test
    void negativeRecordingDurationFailsValidation() {
        assertViolationCount(new VoiceAnalysisSaveRequest("테스트", -0.1, 1.0, 1.0), 1);
    }

    @Test
    void negativeResponseTimeFailsValidation() {
        assertViolationCount(new VoiceAnalysisSaveRequest("테스트", 1.0, -0.1, 1.0), 1);
    }

    @Test
    void negativeSpeechRateFailsValidation() {
        assertViolationCount(new VoiceAnalysisSaveRequest("테스트", 1.0, 1.0, -0.1), 1);
    }

    private void assertViolationCount(VoiceAnalysisSaveRequest request, int count) {
        Set<ConstraintViolation<VoiceAnalysisSaveRequest>> violations =
                validator.validate(request);

        assertEquals(count, violations.size());
    }
}
