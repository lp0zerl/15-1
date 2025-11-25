package ru.hogwarts.school;

class Invest500RuleTest {
    @Mock
    private BankRepository bankRepository;
    @InjectMocks
    private Invest500Rule invest500Rule;

    @Test
    void check_WhenAllConditionsMet_ShouldReturnRecommendation() {
        String userId = "test-user";
        when(bankRepository.usesProductType(userId, "DEBIT")).thenReturn(true);
        when(bankRepository.usesProductType(userId, "INVEST")).thenReturn(false);
        when(bankRepository.getTotalSavingDeposits(userId)).thenReturn(new BigDecimal("1500"));

        Optional<Recommendation> result = invest500Rule.check(userId);

        assertTrue(result.isPresent());
        assertEquals("Invest 500", result.get().getName());
        assertEquals("147f6a0f-3b91-413b-ab99-87f081d60d5a", result.get().getId());
    }
}