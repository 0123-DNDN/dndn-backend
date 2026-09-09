package com.team0123.dndn.activity.dto;

import com.team0123.dndn.ai.dto.AiVoiceConditionRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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

    @Test
    void nestedVoiceConditionIsValidated() {
        VoiceTalkAnswerRequest request = new VoiceTalkAnswerRequest(
                "답변",
                new AiVoiceConditionRequest(
                        -1L,
                        BigDecimal.ONE,
                        100L,
                        0
                )
        );

        assertEquals(1, validator.validate(request).size());
    }
}
