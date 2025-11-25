package ru.hogwarts.school;

class RecommendationServiceTest {
    @Mock
    private BankRepository bankRepository;
    @Mock
    private RecommendationRule rule1;
    @Mock
    private RecommendationRule rule2;
    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    void getRecommendations_WhenUserExists_ShouldReturnRecommendations() {
        String userId = "test-user";
        Recommendation recommendation = new Recommendation("Test Product", "test-id", "Test description");

        when(bankRepository.userExists(userId)).thenReturn(true);
        when(rule1.check(userId)).thenReturn(Optional.of(recommendation));
        when(rule2.check(userId)).thenReturn(Optional.empty());

        RecommendationResponse response = recommendationService.getRecommendations(userId);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(1, response.getRecommendations().size());
        assertEquals(recommendation, response.getRecommendations().get(0));
    }

    @Test
    void getRecommendations_WhenUserNotExists_ShouldReturnEmptyList() {
        String userId = "non-existent-user";
        when(bankRepository.userExists(userId)).thenReturn(false);

        RecommendationResponse response = recommendationService.getRecommendations(userId);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertTrue(response.getRecommendations().isEmpty());
    }
}