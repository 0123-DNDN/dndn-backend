package com.team0123.dndn.voice.controller;

import com.team0123.dndn.auth.JwtTokenProvider;
import com.team0123.dndn.config.SecurityConfig;
import com.team0123.dndn.voice.service.VoiceAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoiceAnalysisController.class)
@Import({
        SecurityConfig.class,
        VoiceAnalysisControllerTest.TestWebSecurityConfiguration.class
})
class VoiceAnalysisControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestWebSecurityConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VoiceAnalysisService voiceAnalysisService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void requestWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/voice/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"오늘 산책했어요.\"}"))
                .andExpect(status().isUnauthorized());
    }
}
