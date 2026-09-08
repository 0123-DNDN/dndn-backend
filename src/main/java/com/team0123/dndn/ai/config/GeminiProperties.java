package com.team0123.dndn.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Gemini API 연결에 필요한 설정값을 관리합니다.
 * 환경변수 연결:
 * GEMINI_API_KEY -> gemini.api-key
 * GEMINI_MODEL   -> gemini.model
 * API Key는 소스 코드나 application.yml에 직접 작성하지 않습니다.
 */
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {
    // Google AI Studio에서 발급받은 Gemini API Key
    private String apiKey;
    /**
     * 기본 Gemini 모델입니다
     * 환경변수 GEMINI_MODEL을 설정하면 다른 모델로 변경할 수 있습니다.
     */
    private String model = "gemini-3.1-flash-lite";
    // Gemini REST API의 기본 주소
    private String baseUrl = "https://generativelanguage.googleapis.com";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}