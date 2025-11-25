package ru.hogwarts.school;


import java.util.Optional;

interface RecommendationRule {
    Optional<Recommendation> check(String userId);
}