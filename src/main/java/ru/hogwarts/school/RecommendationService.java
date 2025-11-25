package ru.hogwarts.school;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
class RecommendationService {
    private final List<RecommendationRule> staticRecommendationRules;
    private final BankRepository bankRepository;
    private final DynamicRuleService dynamicRuleService;
    private final DynamicRuleRepository dynamicRuleRepository;

    public RecommendationService(List<RecommendationRule> staticRecommendationRules,
                                 BankRepository bankRepository,
                                 DynamicRuleService dynamicRuleService,
                                 DynamicRuleRepository dynamicRuleRepository) {
        this.staticRecommendationRules = staticRecommendationRules;
        this.bankRepository = bankRepository;
        this.dynamicRuleService = dynamicRuleService;
        this.dynamicRuleRepository = dynamicRuleRepository;
    }

    public RecommendationResponse getRecommendations(String userId) {
        if (!bankRepository.userExists(userId)) {
            return new RecommendationResponse(userId, new ArrayList<>());
        }

        List<Recommendation> recommendations = new ArrayList<>();

        recommendations.addAll(getStaticRecommendations(userId));
        recommendations.addAll(getDynamicRecommendations(userId));

        return new RecommendationResponse(userId, recommendations);
    }

    private List<Recommendation> getStaticRecommendations(String userId) {
        return staticRecommendationRules.stream()
                .map(rule -> rule.check(userId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<Recommendation> getDynamicRecommendations(String userId) {
        List<DynamicRule> dynamicRules = dynamicRuleRepository.findAll();

        return dynamicRules.stream()
                .filter(rule -> dynamicRuleService.evaluateRule(userId, rule))
                .map(this::convertToRecommendation)
                .collect(Collectors.toList());
    }

    private Recommendation convertToRecommendation(DynamicRule dynamicRule) {
        return new Recommendation(
                dynamicRule.getProductName(),
                dynamicRule.getProductId(),
                dynamicRule.getProductText()
        );
    }
}