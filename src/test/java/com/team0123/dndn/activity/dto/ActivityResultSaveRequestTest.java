package com.team0123.dndn.activity.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActivityResultSaveRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void negativeScoreFailsValidation() {
        ActivityResultSaveRequest request = new ActivityResultSaveRequest(
                -1, null, null, null
        );

        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void negativeStepCountFailsValidation() {
        ActivityResultSaveRequest request = new ActivityResultSaveRequest(
                null, -1, null, null
        );

        assertEquals(1, validator.validate(request).size());
    }
}
