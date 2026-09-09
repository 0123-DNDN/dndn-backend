package com.team0123.dndn.family.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FamilyPostCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validRequestPassesValidation() {
        assertTrue(validator.validate(validRequest()).isEmpty());
    }

    @Test
    void missingRelationshipIdFailsValidation() {
        FamilyPostCreateRequest request = new FamilyPostCreateRequest(
                null,
                "가족 소식"
        );

        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void tooLongMessageFailsValidation() {
        FamilyPostCreateRequest request = new FamilyPostCreateRequest(
                1L,
                "가".repeat(501)
        );

        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void blankMessageFailsValidation() {
        FamilyPostCreateRequest request = new FamilyPostCreateRequest(
                1L,
                "   "
        );

        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void nullMessagePassesValidation() {
        FamilyPostCreateRequest request = new FamilyPostCreateRequest(
                1L,
                null
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    private FamilyPostCreateRequest validRequest() {
        return new FamilyPostCreateRequest(
                1L,
                "가족 소식"
        );
    }
}
