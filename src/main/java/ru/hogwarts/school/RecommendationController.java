package ru.hogwarts.school;

class RecommendationController {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/recommendation/{userId}")
    public RecommendationResponse getRecommendations(@PathVariable String userId) {
        logger.info("Received recommendation request for user: {}", userId);
        return recommendationService.getRecommendations(userId);
    }
}