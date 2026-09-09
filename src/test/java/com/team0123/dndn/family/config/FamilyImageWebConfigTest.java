package com.team0123.dndn.family.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FamilyImageWebConfigTest {

    @Test
    void mapsFamilyUploadWebPathToLocalDirectory() {
        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration =
                mock(ResourceHandlerRegistration.class);
        when(registry.addResourceHandler("/uploads/family/**"))
                .thenReturn(registration);
        when(registration.addResourceLocations(any(String[].class)))
                .thenReturn(registration);

        new FamilyImageWebConfig().addResourceHandlers(registry);

        verify(registry).addResourceHandler("/uploads/family/**");
        ArgumentCaptor<String[]> locationCaptor =
                ArgumentCaptor.forClass(String[].class);
        verify(registration).addResourceLocations(locationCaptor.capture());
        assertEquals(1, locationCaptor.getValue().length);
        assertTrue(locationCaptor.getValue()[0].startsWith("file:"));
        assertTrue(locationCaptor.getValue()[0].endsWith("/"));
    }
}
