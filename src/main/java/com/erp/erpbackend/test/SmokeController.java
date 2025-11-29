package com.erp.erpbackend.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SmokeController {

    @GetMapping("/api/assignments/health")
    public String health() {
        return "{\"status\":\"OK\",\"message\":\"assignments route reachable\"}";
    }
}
