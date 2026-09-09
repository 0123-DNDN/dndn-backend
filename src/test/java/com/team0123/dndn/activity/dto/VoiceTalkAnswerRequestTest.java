package com.team0123.dndn.activity.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoiceTalkAnswerRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void blankTextFailsValidation() {
        assertEquals(
                1,
                validator.validate(new VoiceTalkAnswerRequest(" ")).size()
        );
    }
}
