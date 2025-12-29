package ru.hogwarts.school;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/management") class ManagementController {
    private final EnhancedRecommendationService recommendationService; public ManagementController(EnhancedRecommendationService recommendationService) { this.recommendationService = recommendationService; }
    @PostMapping("/clear-caches") public ResponseEntity<String> clearCaches() { recommendationService.clearAllCaches(); return ResponseEntity.ok("All caches cleared successfully"); }
    @GetMapping("/info") public ResponseEntity<Map<String, String>> getServiceInfo() { Map<String, String> info = new HashMap<>(); info.put("name", "recommendation-service"); info.put("version", "1.0.0"); return ResponseEntity.ok(info); }
}
