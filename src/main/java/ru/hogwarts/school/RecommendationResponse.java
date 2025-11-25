package ru.hogwarts.school;

import java.util.ArrayList;
import java.util.List;

class RecommendationResponse {
    private String userId;
    private List<ru.example.bank.Recommendation> recommendations = new ArrayList<>();

    public RecommendationResponse() {}
    public RecommendationResponse(String userId, List<ru.example.bank.Recommendation> recommendations) {
        this.userId = userId;
        this.recommendations = recommendations;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<ru.example.bank.Recommendation> getRecommendations() { return recommendations; }
    public void setRecommendations(List<ru.example.bank.Recommendation> recommendations) { this.recommendations = recommendations; }
}