package com.worldcup.Back.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("✅ Backend está activo");
    }

    @PostMapping
    public ResponseEntity<String> healthPost() {
        return ResponseEntity.ok("✅ POST al backend funciona correctamente");
    }
}
