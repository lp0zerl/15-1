package ru.hogwarts.school;

interface RecommendationRule {
    Optional<Recommendation> check(String userId);
}
