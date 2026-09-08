package com.team0123.dndn.activity.controller;

import com.team0123.dndn.activity.service.ActivityService;
import com.team0123.dndn.auth.JwtTokenProvider;
import com.team0123.dndn.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@WebMvcTest(ActivityController.class)
@Import({
        SecurityConfig.class,
        ActivityControllerTest.TestWebSecurityConfiguration.class
})
class ActivityControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestWebSecurityConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void requestWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/activities/today"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void negativeScoreReturnsBadRequest() throws Exception {
        mockAuthentication();

        mockMvc.perform(post("/api/activities/1/results")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativeStepCountReturnsBadRequest() throws Exception {
        mockAuthentication();

        mockMvc.perform(post("/api/activities/3/results")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stepCount\":-1}"))
                .andExpect(status().isBadRequest());
    }

    private void mockAuthentication() {
        when(jwtTokenProvider.validateToken("token")).thenReturn(true);
        when(jwtTokenProvider.getUserId("token")).thenReturn(1L);
        when(jwtTokenProvider.getRole("token")).thenReturn("SENIOR");
    }
}
