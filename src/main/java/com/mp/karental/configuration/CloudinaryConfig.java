package com.mp.karental.configuration;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class CloudinaryConfig {
    @Value("${cloud.cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloud.cloudinary.api-key}")
    private String apiKey;

    @Value("${cloud.cloudinary.api-secret}")
    private String apiSecret;
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }
}
