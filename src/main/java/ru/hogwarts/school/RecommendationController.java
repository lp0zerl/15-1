package ru.hogwarts.school;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/recommendation") class RecommendationController {
    private final EnhancedRecommendationService recommendationService; public RecommendationController(EnhancedRecommendationService recommendationService) { this.recommendationService = recommendationService; }
    @GetMapping("/{userId}") public List<ProductRecommendation> getRecommendations(@PathVariable UUID userId) { return recommendationService.getRecommendations(userId); }
}
