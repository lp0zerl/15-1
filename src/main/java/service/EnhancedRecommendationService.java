package service;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import repository.UserKnowledgeRepository;
import ru.hogwarts.school.DynamicRuleResponse;
import ru.hogwarts.school.ProductRecommendation;
import ru.hogwarts.school.RuleQueryDto;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
class EnhancedRecommendationService {
    private final DynamicRuleService dynamicRuleService; private final RuleEvaluationService ruleEvaluationService;
    private final RuleStatisticService ruleStatisticService; private final CacheManager cacheManager;
    private final Cache<String, Boolean> userOfCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).maximumSize(10000).build();
    private final Cache<String, Boolean> activeUserOfCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).maximumSize(10000).build();
    private final Cache<String, BigDecimal> transactionSumCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).maximumSize(10000).build();

    public EnhancedRecommendationService(DynamicRuleService dynamicRuleService, RuleEvaluationService ruleEvaluationService,
                                         RuleStatisticService ruleStatisticService, CacheManager cacheManager) {
        this.dynamicRuleService = dynamicRuleService; this.ruleEvaluationService = ruleEvaluationService;
        this.ruleStatisticService = ruleStatisticService; this.cacheManager = cacheManager;
    }

    @Cacheable(value = "userRecommendations", key = "#userId")
    public List<ProductRecommendation> getRecommendations(UUID userId) {
        List<ProductRecommendation> recommendations = new ArrayList<>();
        recommendations.addAll(getFixedRecommendations(userId));
        List<DynamicRuleResponse> dynamicRules = dynamicRuleService.getAllRules();
        for (DynamicRuleResponse rule : dynamicRules) {
            if (evaluateRule(userId, rule)) {
                recommendations.add(new ProductRecommendation(rule.getProduct_id(), rule.getProduct_name(), rule.getProduct_text()));
                ruleStatisticService.incrementRuleStatistic(rule.getId());
            }
        }
        return recommendations;
    }

    private List<ProductRecommendation> getFixedRecommendations(UUID userId) {
        List<ProductRecommendation> fixed = new ArrayList<>();
        fixed.add(new ProductRecommendation(UUID.fromString("11111111-1111-1111-1111-111111111111"), "Стандартная карта", "Базовая карта для ежедневных расходов"));
        return fixed;
    }

    private boolean evaluateRule(UUID userId, DynamicRuleResponse rule) {
        for (RuleQueryDto query : rule.getRule()) {
            if (!evaluateQueryWithCache(userId, query)) return false;
        }
        return true;
    }

    private boolean evaluateQueryWithCache(UUID userId, RuleQueryDto query) {
        boolean result = switch (query.getQuery()) {
            case "USER_OF" -> checkUserOfWithCache(userId, query);
            case "ACTIVE_USER_OF" -> checkActiveUserOfWithCache(userId, query);
            case "TRANSACTION_SUM_COMPARE" -> checkTransactionSumCompareWithCache(userId, query);
            case "TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW" -> checkTransactionSumCompareDepositWithdrawWithCache(userId, query);
            default -> ruleEvaluationService.evaluateQuery(userId, query);
        };
        return query.isNegate() != result;
    }

    private boolean checkUserOfWithCache(UUID userId, RuleQueryDto query) {
        String productType = query.getArguments().get(0); String cacheKey = userId + "_USER_OF_" + productType;
        return userOfCache.get(cacheKey, key -> ruleEvaluationService.evaluateQuery(userId, new RuleQueryDto("USER_OF", Collections.singletonList(productType), false)));
    }

    private boolean checkActiveUserOfWithCache(UUID userId, RuleQueryDto query) {
        String productType = query.getArguments().get(0); String cacheKey = userId + "_ACTIVE_USER_OF_" + productType;
        return activeUserOfCache.get(cacheKey, key -> ruleEvaluationService.evaluateQuery(userId, new RuleQueryDto("ACTIVE_USER_OF", Collections.singletonList(productType), false)));
    }

    private boolean checkTransactionSumCompareWithCache(UUID userId, RuleQueryDto query) {
        List<String> args = query.getArguments(); String productType = args.get(0); String transactionType = args.get(1);
        String cacheKey = userId + "_SUM_" + productType + "_" + transactionType;
        BigDecimal sum = transactionSumCache.get(cacheKey, key -> getUserKnowledgeRepository().sumTransactionsByType(userId, productType, transactionType));
        String operator = args.get(2); BigDecimal constant = new BigDecimal(args.get(3));
        return compareValues(sum, operator, constant);
    }

    private boolean checkTransactionSumCompareDepositWithdrawWithCache(UUID userId, RuleQueryDto query) {
        List<String> args = query.getArguments(); String productType = args.get(0);
        String depositKey = userId + "_SUM_" + productType + "_DEPOSIT"; String withdrawKey = userId + "_SUM_" + productType + "_WITHDRAW";
        BigDecimal depositSum = transactionSumCache.get(depositKey, key -> getUserKnowledgeRepository().sumTransactionsByType(userId, productType, "DEPOSIT"));
        BigDecimal withdrawSum = transactionSumCache.get(withdrawKey, key -> getUserKnowledgeRepository().sumTransactionsByType(userId, productType, "WITHDRAW"));
        String operator = args.get(1); return compareValues(depositSum, operator, withdrawSum);
    }

    private UserKnowledgeRepository getUserKnowledgeRepository() { return null; }

    private boolean compareValues(BigDecimal value1, String operator, BigDecimal value2) {
        switch (operator) {
            case ">": return value1.compareTo(value2) > 0; case "<": return value1.compareTo(value2) < 0;
            case "=": return value1.compareTo(value2) == 0; case ">=": return value1.compareTo(value2) >= 0;
            case "<=": return value1.compareTo(value2) <= 0; default: throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    public void clearAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> { if (cacheManager.getCache(cacheName) != null) cacheManager.getCache(cacheName).clear(); });
        userOfCache.invalidateAll(); activeUserOfCache.invalidateAll(); transactionSumCache.invalidateAll();
    }
}