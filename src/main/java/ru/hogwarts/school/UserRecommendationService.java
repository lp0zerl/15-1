package ru.hogwarts.school;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
class UserRecommendationService {
    private final UserProfileRepository userProfileRepository; private final EnhancedRecommendationService recommendationService;
    public UserRecommendationService(UserProfileRepository userProfileRepository, EnhancedRecommendationService recommendationService) {
        this.userProfileRepository = userProfileRepository; this.recommendationService = recommendationService;
    }

    public Optional<UserProfile> findUserByUsername(String username) { return userProfileRepository.findByUsername(username); }
    public List<UserProfile> findUsersByName(String firstName, String lastName) { return userProfileRepository.findByFirstNameAndLastName(firstName, lastName); }

    public String getRecommendationsForUsername(String username) {
        Optional<UserProfile> userOpt = findUserByUsername(username);
        if (userOpt.isEmpty()) return "Пользователь не найден";
        UserProfile user = userOpt.get(); List<ProductRecommendation> recommendations = recommendationService.getRecommendations(user.getId());
        if (recommendations.isEmpty()) return String.format("Здравствуйте %s\n\nНет новых рекомендаций для вас.", user.getFullName());
        StringBuilder response = new StringBuilder(); response.append(String.format("Здравствуйте %s\n\n", user.getFullName()));
        response.append("Новые продукты для вас:\n"); for (ProductRecommendation rec : recommendations) response.append(rec.toString()).append("\n");
        return response.toString();
    }
}