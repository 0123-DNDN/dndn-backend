package com.team0123.dndn.family.controller;

import com.team0123.dndn.auth.JwtTokenProvider;
import com.team0123.dndn.config.SecurityConfig;
import com.team0123.dndn.family.service.FamilyReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FamilyReportController.class)
@Import({
        SecurityConfig.class,
        FamilyReportControllerTest.TestWebSecurityConfiguration.class
})
class FamilyReportControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestWebSecurityConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FamilyReportService familyReportService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void requestWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/family/reports/weekly")
                        .param("weekStart", "2026-09-07"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passesJwtUserIdAndWeekStartToService() throws Exception {
        mockAuthentication();

        mockMvc.perform(get("/api/family/reports/weekly")
                        .header("Authorization", "Bearer token")
                        .param("weekStart", "2026-09-07"))
                .andExpect(status().isOk());

        verify(familyReportService).getWeeklyReport(
                1L,
                LocalDate.of(2026, 9, 7)
        );
    }

    @Test
    void malformedWeekStartReturnsBadRequest() throws Exception {
        mockAuthentication();

        mockMvc.perform(get("/api/family/reports/weekly")
                        .header("Authorization", "Bearer token")
                        .param("weekStart", "2026/09/07"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonGuardianResponseIsForbidden() throws Exception {
        mockAuthentication();
        when(familyReportService.getWeeklyReport(
                1L,
                LocalDate.of(2026, 9, 7)
        )).thenThrow(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "보호자만 가족 리포트를 조회할 수 있습니다."
        ));

        mockMvc.perform(get("/api/family/reports/weekly")
                        .header("Authorization", "Bearer token")
                        .param("weekStart", "2026-09-07"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(
                        "보호자만 가족 리포트를 조회할 수 있습니다."
                ));
    }

    @Test
    void voiceReportPassesJwtUserIdAndDateRangeToService() throws Exception {
        mockAuthentication();

        mockMvc.perform(get("/api/family/reports/voice")
                        .header("Authorization", "Bearer token")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-09"))
                .andExpect(status().isOk());

        verify(familyReportService).getVoiceReport(
                1L,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 9)
        );
    }

    @Test
    void malformedVoiceReportDateReturnsBadRequest() throws Exception {
        mockAuthentication();

        mockMvc.perform(get("/api/family/reports/voice")
                        .header("Authorization", "Bearer token")
                        .param("startDate", "2026/09/01")
                        .param("endDate", "2026-09-09"))
                .andExpect(status().isBadRequest());
    }

    private void mockAuthentication() {
        when(jwtTokenProvider.validateToken("token")).thenReturn(true);
        when(jwtTokenProvider.getUserId("token")).thenReturn(1L);
        when(jwtTokenProvider.getRole("token")).thenReturn("GUARDIAN");
    }
}
