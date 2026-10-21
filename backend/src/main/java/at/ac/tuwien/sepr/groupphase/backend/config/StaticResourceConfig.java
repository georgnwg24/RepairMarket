package at.ac.tuwien.sepr.groupphase.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${chat.upload.base-path}")
    private String uploadBasePath;

    @Value("${chat.upload.base-url}")
    private String uploadBaseUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
            .addResourceHandler(uploadBaseUrl + "/**")
            .addResourceLocations("file:" + uploadBasePath + "/");
    }
}
