package com.erp.erpbackend.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dklkkb6k9");
        config.put("api_key", "161487663498373");
        config.put("api_secret", "rFggPsOLOmhGvnNb3UaDy30WtRE");
        // IMPORTANT: resource_type=raw lets us upload PDFs, docs, etc.
        return new Cloudinary(config);
    }
}
