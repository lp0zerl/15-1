package ru.hogwarts.school;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
class DynamicRuleService {
    private final BankRepository bankRepository;
    private final RuleStatisticRepository statisticRepository;

    public DynamicRuleService(BankRepository bankRepository, RuleStatisticRepository statisticRepository) {
        this.bankRepository = bankRepository;
        this.statisticRepository = statisticRepository;
    }

    public boolean evaluateRule(String userId, DynamicRule dynamicRule) {
        boolean result = evaluateRuleQueries(userId, dynamicRule.getRule());

        if (result) {
            updateRuleStatistics(dynamicRule.getId());
        }

        return result;
    }

    private boolean evaluateRuleQueries(String userId, List<RuleQuery> ruleQueries) {
        for (RuleQuery query : ruleQueries) {
            boolean result = evaluateQuery(userId, query);
            if (!result) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateQuery(String userId, RuleQuery query) {
        boolean result;

        switch (query.getQuery()) {
            case "USER_OF":
                result = evaluateUserOf(userId, query.getArguments());
                break;
            case "ACTIVE_USER_OF":
                result = evaluateActiveUserOf(userId, query.getArguments());
                break;
            case "TRANSACTION_SUM_COMPARE":
                result = evaluateTransactionSumCompare(userId, query.getArguments());
                break;
            case "TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW":
                result = evaluateDepositWithdrawCompare(userId, query.getArguments());
                break;
            default:
                throw new IllegalArgumentException("Unknown query type: " + query.getQuery());
        }

        return query.isNegate() != result;
    }

    private boolean evaluateUserOf(String userId, List<String> arguments) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("USER_OF query requires exactly 1 argument");
        }
        String productType = arguments.get(0);
        return bankRepository.usesProductType(userId, productType);
    }

    private boolean evaluateActiveUserOf(String userId, List<String> arguments) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("ACTIVE_USER_OF query requires exactly 1 argument");
        }
        String productType = arguments.get(0);
        return bankRepository.isActiveUserOf(userId, productType);
    }

    private boolean evaluateTransactionSumCompare(String userId, List<String> arguments) {
        if (arguments.size() != 4) {
            throw new IllegalArgumentException("TRANSACTION_SUM_COMPARE query requires exactly 4 arguments");
        }

        String productType = arguments.get(0);
        String operationType = arguments.get(1);
        String comparisonOperator = arguments.get(2);
        BigDecimal comparisonValue = new BigDecimal(arguments.get(3));

        BigDecimal transactionSum = bankRepository.getTransactionSum(userId, productType, operationType);

        switch (comparisonOperator) {
            case ">": return transactionSum.compareTo(comparisonValue) > 0;
            case "<": return transactionSum.compareTo(comparisonValue) < 0;
            case "=": return transactionSum.compareTo(comparisonValue) == 0;
            case ">=": return transactionSum.compareTo(comparisonValue) >= 0;
            case "<=": return transactionSum.compareTo(comparisonValue) <= 0;
            default: throw new IllegalArgumentException("Unknown comparison operator: " + comparisonOperator);
        }
    }

    private boolean evaluateDepositWithdrawCompare(String userId, List<String> arguments) {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException("TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW query requires exactly 2 arguments");
        }

        String productType = arguments.get(0);
        String comparisonOperator = arguments.get(1);

        return bankRepository.compareDepositWithdraw(userId, productType, comparisonOperator);
    }

    @Transactional
    protected void updateRuleStatistics(Long ruleId) {
        RuleStatistic statistic = statisticRepository.findByRuleId(ruleId)
                .orElse(new RuleStatistic(ruleId));

        statistic.setExecutionCount(statistic.getExecutionCount() + 1);
        statisticRepository.save(statistic);
    }
}