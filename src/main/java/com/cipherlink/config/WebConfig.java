package com.cipherlink.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Spring Boot only auto-serves static files from the classpath (static/, public/).
 * Story images/videos are saved to a real filesystem folder (media.stories-path),
 * so without this handler, /media/stories/** has no backing route at all and
 * every story image request 404s, regardless of what URL the frontend builds.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${media.stories-path:./media/stories}")
    private String storiesPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(storiesPath).toAbsolutePath().normalize().toString();
        registry.addResourceHandler("/media/stories/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
