package ru.hogwarts.school;


class BankRepository {
    private final JdbcTemplate jdbcTemplate;

    public BankRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean usesProductType(String userId, String productType) {
        String sql = """
            SELECT COUNT(*) > 0
            FROM transactions t
            JOIN products p ON t.product_id = p.id
            WHERE t.user_id = ? AND p.type = ?
            """;
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, userId, productType));
    }

    public BigDecimal getTotalDepositsByProductType(String userId, String productType) {
        String sql = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM transactions t
            JOIN products p ON t.product_id = p.id
            WHERE t.user_id = ? AND p.type = ? AND t.operation_type = 'DEPOSIT'
            """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, userId, productType);
    }

    public BigDecimal getTotalExpensesByProductType(String userId, String productType) {
        String sql = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM transactions t
            JOIN products p ON t.product_id = p.id
            WHERE t.user_id = ? AND p.type = ? AND t.operation_type = 'EXPENSE'
            """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, userId, productType);
    }

    public BigDecimal getTotalSavingDeposits(String userId) {
        return getTotalDepositsByProductType(userId, "SAVING");
    }

    public BigDecimal getTotalDebitDeposits(String userId) {
        return getTotalDepositsByProductType(userId, "DEBIT");
    }

    public BigDecimal getTotalDebitExpenses(String userId) {
        return getTotalExpensesByProductType(userId, "DEBIT");
    }

    public boolean userExists(String userId) {
        String sql = "SELECT COUNT(*) > 0 FROM transactions WHERE user_id = ?";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, userId));
    }
}