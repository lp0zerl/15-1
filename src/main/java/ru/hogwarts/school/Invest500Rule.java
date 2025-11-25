package ru.hogwarts.school;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
class Invest500Rule implements RecommendationRule {
    private static final String PRODUCT_ID = "147f6a0f-3b91-413b-ab99-87f081d60d5a";
    private static final String PRODUCT_NAME = "Invest 500";
    private static final String PRODUCT_DESCRIPTION = "Откройте свой путь к успеху с индивидуальным инвестиционным счетом (ИИС) от нашего банка! Воспользуйтесь налоговыми льготами и начните инвестировать с умом.";

    private final BankRepository bankRepository;

    public Invest500Rule(BankRepository bankRepository) {
        this.bankRepository = bankRepository;
    }

    @Override
    public Optional<Recommendation> check(String userId) {
        boolean usesDebit = bankRepository.usesProductType(userId, "DEBIT");
        boolean notUsesInvest = !bankRepository.usesProductType(userId, "INVEST");
        BigDecimal savingDeposits = bankRepository.getTotalSavingDeposits(userId);
        boolean savingDepositsOver1000 = savingDeposits.compareTo(new BigDecimal("1000")) > 0;

        if (usesDebit && notUsesInvest && savingDepositsOver1000) {
            return Optional.of(new Recommendation(PRODUCT_NAME, PRODUCT_ID, PRODUCT_DESCRIPTION));
        }
        return Optional.empty();
    }
}