package com.team0123.dndn.family.controller;

import com.team0123.dndn.auth.JwtTokenProvider;
import com.team0123.dndn.config.SecurityConfig;
import com.team0123.dndn.family.dto.FamilyPostCreateRequest;
import com.team0123.dndn.family.service.FamilyPostService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FamilyPostController.class)
@Import({
        SecurityConfig.class,
        FamilyPostControllerTest.TestWebSecurityConfiguration.class
})
class FamilyPostControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestWebSecurityConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FamilyPostService familyPostService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(multipart("/api/family-posts")
                        .file(image())
                        .param("relationshipId", "1")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void todayWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/family-posts/today"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidCreateRequestReturnsBadRequest() throws Exception {
        mockAuthentication("GUARDIAN");

        mockMvc.perform(multipart("/api/family-posts")
                        .file(image())
                        .header("Authorization", "Bearer token")
                        .param("relationshipId", "1")
                        .param("message", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingImageReturnsBadRequest() throws Exception {
        mockAuthentication("GUARDIAN");

        mockMvc.perform(multipart("/api/family-posts")
                        .header("Authorization", "Bearer token")
                        .param("relationshipId", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void multipartFieldsAndImageAreBoundForCreate() throws Exception {
        mockAuthentication("GUARDIAN");

        mockMvc.perform(multipart("/api/family-posts")
                        .file(image())
                        .header("Authorization", "Bearer token")
                        .param("relationshipId", "10")
                        .param("message", "가족 소식"))
                .andExpect(status().isOk());

        ArgumentCaptor<FamilyPostCreateRequest> requestCaptor =
                ArgumentCaptor.forClass(FamilyPostCreateRequest.class);
        ArgumentCaptor<MultipartFile> imageCaptor =
                ArgumentCaptor.forClass(MultipartFile.class);
        verify(familyPostService).create(
                eq(1L),
                requestCaptor.capture(),
                imageCaptor.capture()
        );
        assertEquals(10L, requestCaptor.getValue().relationshipId());
        assertEquals("가족 소식", requestCaptor.getValue().message());
        assertEquals("image/jpeg", imageCaptor.getValue().getContentType());
    }

    private void mockAuthentication(String role) {
        when(jwtTokenProvider.validateToken("token")).thenReturn(true);
        when(jwtTokenProvider.getUserId("token")).thenReturn(1L);
        when(jwtTokenProvider.getRole("token")).thenReturn(role);
    }

    private MockMultipartFile image() {
        return new MockMultipartFile(
                "image",
                "family.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );
    }
}
