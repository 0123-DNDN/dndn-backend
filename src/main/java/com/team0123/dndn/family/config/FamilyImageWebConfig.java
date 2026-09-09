package com.team0123.dndn.family.config;

import com.team0123.dndn.family.service.FamilyImageStorage;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FamilyImageWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resourceLocation =
                FamilyImageStorage.UPLOAD_DIRECTORY.toUri().toString();
        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }

        registry.addResourceHandler("/uploads/family/**")
                .addResourceLocations(resourceLocation);
    }
}
