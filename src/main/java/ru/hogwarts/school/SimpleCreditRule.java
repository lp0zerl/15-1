package ru.hogwarts.school;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
class SimpleCreditRule implements RecommendationRule {
    private static final String PRODUCT_ID = "ab138afb-f3ba-4a93-b74f-0fcee86d447f";
    private static final String PRODUCT_NAME = "Простой кредит";
    private static final String PRODUCT_DESCRIPTION = "Откройте мир выгодных кредитов с нами! Ищете способ быстро и без лишних хлопот получить нужную сумму? Тогда наш выгодный кредит — именно то, что вам нужно!";

    private final BankRepository bankRepository;

    public SimpleCreditRule(BankRepository bankRepository) {
        this.bankRepository = bankRepository;
    }

    @Override
    public Optional<Recommendation> check(String userId) {
        boolean notUsesCredit = !bankRepository.usesProductType(userId, "CREDIT");
        BigDecimal debitDeposits = bankRepository.getTotalDebitDeposits(userId);
        BigDecimal debitExpenses = bankRepository.getTotalDebitExpenses(userId);
        boolean depositsGreaterThanExpenses = debitDeposits.compareTo(debitExpenses) > 0;
        boolean expensesOver100k = debitExpenses.compareTo(new BigDecimal("100000")) > 0;

        if (notUsesCredit && depositsGreaterThanExpenses && expensesOver100k) {
            return Optional.of(new Recommendation(PRODUCT_NAME, PRODUCT_ID, PRODUCT_DESCRIPTION));
        }
        return Optional.empty();
    }
}