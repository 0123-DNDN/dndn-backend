package com.team0123.dndn.activity.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceTalkOpeningQuestionsTest {

    @Test
    void selectsQuestionFromThirtySixOpeningQuestions() {
        VoiceTalkOpeningQuestions questions = new VoiceTalkOpeningQuestions();

        assertEquals(36, questions.allQuestions().size());
        for (int count = 0; count < 100; count++) {
            assertTrue(questions.contains(questions.randomQuestion()));
        }
    }
}
