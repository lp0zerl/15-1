package ru.hogwarts.school;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Repository
class BankRepository {
    private final JdbcTemplate jdbcTemplate;

    private final Cache<String, Boolean> userOfCache;
    private final Cache<String, Boolean> activeUserOfCache;
    private final Cache<String, BigDecimal> transactionSumCache;
    private final Cache<String, Boolean> depositWithdrawCompareCache;

    public BankRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

        this.userOfCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();

        this.activeUserOfCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();

        this.transactionSumCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();

        this.depositWithdrawCompareCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }

    public void clearAllCaches() {
        userOfCache.invalidateAll();
        activeUserOfCache.invalidateAll();
        transactionSumCache.invalidateAll();
        depositWithdrawCompareCache.invalidateAll();
    }

    public boolean usesProductType(String userId, String productType) {
        String cacheKey = userId + ":" + productType;
        return userOfCache.get(cacheKey, key -> {
            String sql = "SELECT COUNT(*) > 0 FROM transactions t JOIN products p ON t.product_id = p.id WHERE t.user_id = ? AND p.type = ?";
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, userId, productType));
        });
    }

    public boolean isActiveUserOf(String userId, String productType) {
        String cacheKey = userId + ":" + productType;
        return activeUserOfCache.get(cacheKey, key -> {
            String sql = "SELECT COUNT(*) >= 5 FROM transactions t JOIN products p ON t.product_id = p.id WHERE t.user_id = ? AND p.type = ?";
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, userId, productType));
        });
    }

    public BigDecimal getTransactionSum(String userId, String productType, String operationType) {
        String cacheKey = userId + ":" + productType + ":" + operationType;
        return transactionSumCache.get(cacheKey, key -> {
            String sql = "SELECT COALESCE(SUM(t.amount), 0) FROM transactions t JOIN products p ON t.product_id = p.id WHERE t.user_id = ? AND p.type = ? AND t.operation_type = ?";
            return jdbcTemplate.queryForObject(sql, BigDecimal.class, userId, productType, operationType);
        });
    }

    public boolean compareDepositWithdraw(String userId, String productType, String comparisonOperator) {
        String cacheKey = userId + ":" + productType + ":" + comparisonOperator;
        return depositWithdrawCompareCache.get(cacheKey, key -> {
            BigDecimal depositSum = getTransactionSum(userId, productType, "DEPOSIT");
            BigDecimal withdrawSum = getTransactionSum(userId, productType, "WITHDRAW");

            switch (comparisonOperator) {
                case ">": return depositSum.compareTo(withdrawSum) > 0;
                case "<": return depositSum.compareTo(withdrawSum) < 0;
                case "=": return depositSum.compareTo(withdrawSum) == 0;
                case ">=": return depositSum.compareTo(withdrawSum) >= 0;
                case "<=": return depositSum.compareTo(withdrawSum) <= 0;
                default: throw new IllegalArgumentException("Unknown comparison operator: " + comparisonOperator);
            }
        });
    }

    public BigDecimal getTotalSavingDeposits(String userId) {
        return getTransactionSum(userId, "SAVING", "DEPOSIT");
    }

    public BigDecimal getTotalDebitDeposits(String userId) {
        return getTransactionSum(userId, "DEBIT", "DEPOSIT");
    }

    public BigDecimal getTotalDebitExpenses(String userId) {
        return getTransactionSum(userId, "DEBIT", "WITHDRAW");
    }

    public boolean userExists(String userId) {
        String sql = "SELECT COUNT(*) > 0 FROM transactions WHERE user_id = ?";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, userId));
    }
}
