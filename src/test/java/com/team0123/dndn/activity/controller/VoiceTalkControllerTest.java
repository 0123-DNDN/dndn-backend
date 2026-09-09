package com.team0123.dndn.activity.controller;

import com.team0123.dndn.activity.dto.VoiceTalkAnswerRequest;
import com.team0123.dndn.activity.service.VoiceTalkService;
import com.team0123.dndn.auth.JwtTokenProvider;
import com.team0123.dndn.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoiceTalkController.class)
@Import({
        SecurityConfig.class,
        VoiceTalkControllerTest.TestWebSecurityConfiguration.class
})
class VoiceTalkControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestWebSecurityConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VoiceTalkService voiceTalkService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void unauthenticatedStartReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/activities/voice-talk/sessions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blankAnswerReturnsBadRequest() throws Exception {
        when(jwtTokenProvider.validateToken("token")).thenReturn(true);
        when(jwtTokenProvider.getUserId("token")).thenReturn(1L);
        when(jwtTokenProvider.getRole("token")).thenReturn("SENIOR");

        mockMvc.perform(post(
                        "/api/activities/voice-talk/sessions/10/answers"
                )
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void voiceConditionIsBoundToAnswerRequest() throws Exception {
        when(jwtTokenProvider.validateToken("token")).thenReturn(true);
        when(jwtTokenProvider.getUserId("token")).thenReturn(1L);
        when(jwtTokenProvider.getRole("token")).thenReturn("SENIOR");

        mockMvc.perform(post(
                        "/api/activities/voice-talk/sessions/10/answers"
                )
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "오늘 산책했어요",
                                  "voiceCondition": {
                                    "speechDurationMs": 4000,
                                    "speechRate": 3.25,
                                    "avgPauseDurationMs": 1200,
                                    "longPauseCount": 2
                                  }
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<VoiceTalkAnswerRequest> requestCaptor =
                ArgumentCaptor.forClass(VoiceTalkAnswerRequest.class);
        verify(voiceTalkService).answer(
                eq(1L),
                eq(10L),
                requestCaptor.capture()
        );
        VoiceTalkAnswerRequest request = requestCaptor.getValue();
        assertEquals("오늘 산책했어요", request.text());
        assertEquals(4000L, request.voiceCondition().speechDurationMs());
        assertEquals("3.25", request.voiceCondition().speechRate().toPlainString());
        assertEquals(1200L, request.voiceCondition().avgPauseDurationMs());
        assertEquals(2, request.voiceCondition().longPauseCount());
    }

    @Test
    void negativeNestedVoiceMetricReturnsBadRequest() throws Exception {
        when(jwtTokenProvider.validateToken("token")).thenReturn(true);
        when(jwtTokenProvider.getUserId("token")).thenReturn(1L);
        when(jwtTokenProvider.getRole("token")).thenReturn("SENIOR");

        mockMvc.perform(post(
                        "/api/activities/voice-talk/sessions/10/answers"
                )
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "답변",
                                  "voiceCondition": {
                                    "speechDurationMs": -1,
                                    "speechRate": 3.25,
                                    "avgPauseDurationMs": 1200,
                                    "longPauseCount": 2
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
