package ru.hogwarts.school;

@Service
class RecommendationService {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    private final List<RecommendationRule> recommendationRules;
    private final BankRepository bankRepository;

    public RecommendationService(List<RecommendationRule> recommendationRules, BankRepository bankRepository) {
        this.recommendationRules = recommendationRules;
        this.bankRepository = bankRepository;
    }

    public RecommendationResponse getRecommendations(String userId) {
        logger.info("Getting recommendations for user: {}", userId);

        if (!bankRepository.userExists(userId)) {
            logger.warn("User not found: {}", userId);
            return new RecommendationResponse(userId, List.of());
        }

        List<Recommendation> recommendations = recommendationRules.stream()
                .map(rule -> rule.check(userId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        logger.info("Found {} recommendations for user: {}", recommendations.size(), userId);
        return new RecommendationResponse(userId, recommendations);
    }
}