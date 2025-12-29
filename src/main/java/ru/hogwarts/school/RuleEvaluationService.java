package ru.hogwarts.school;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
class RuleEvaluationService {
    private final UserKnowledgeRepository userKnowledgeRepository;
    public RuleEvaluationService(UserKnowledgeRepository userKnowledgeRepository) { this.userKnowledgeRepository = userKnowledgeRepository; }

    public boolean evaluateQuery(UUID userId, RuleQueryDto query) {
        boolean result = switch (query.getQuery()) {
            case "USER_OF" -> checkUserOf(userId, query);
            case "ACTIVE_USER_OF" -> checkActiveUserOf(userId, query);
            case "TRANSACTION_SUM_COMPARE" -> checkTransactionSumCompare(userId, query);
            case "TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW" -> checkTransactionSumCompareDepositWithdraw(userId, query);
            default -> throw new IllegalArgumentException("Unknown query type: " + query.getQuery());
        };
        return query.isNegate() != result;
    }

    private boolean checkUserOf(UUID userId, RuleQueryDto query) {
        String productType = query.getArguments().get(0);
        return userKnowledgeRepository.existsByUserAndProductType(userId, productType);
    }

    private boolean checkActiveUserOf(UUID userId, RuleQueryDto query) {
        String productType = query.getArguments().get(0);
        Long count = userKnowledgeRepository.countTransactionsByUserAndProductType(userId, productType);
        return count != null && count >= 5;
    }

    private boolean checkTransactionSumCompare(UUID userId, RuleQueryDto query) {
        List<String> args = query.getArguments(); String productType = args.get(0); String transactionType = args.get(1);
        String operator = args.get(2); BigDecimal constant = new BigDecimal(args.get(3));
        BigDecimal sum = userKnowledgeRepository.sumTransactionsByType(userId, productType, transactionType);
        return compareValues(sum, operator, constant);
    }

    private boolean checkTransactionSumCompareDepositWithdraw(UUID userId, RuleQueryDto query) {
        List<String> args = query.getArguments(); String productType = args.get(0); String operator = args.get(1);
        BigDecimal depositSum = userKnowledgeRepository.sumTransactionsByType(userId, productType, "DEPOSIT");
        BigDecimal withdrawSum = userKnowledgeRepository.sumTransactionsByType(userId, productType, "WITHDRAW");
        return compareValues(depositSum, operator, withdrawSum);
    }

    private boolean compareValues(BigDecimal value1, String operator, BigDecimal value2) {
        switch (operator) {
            case ">": return value1.compareTo(value2) > 0;
            case "<": return value1.compareTo(value2) < 0;
            case "=": return value1.compareTo(value2) == 0;
            case ">=": return value1.compareTo(value2) >= 0;
            case "<=": return value1.compareTo(value2) <= 0;
            default: throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }
}