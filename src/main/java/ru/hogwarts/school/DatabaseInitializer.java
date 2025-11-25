package ru.hogwarts.school;

import org.springframework.jdbc.core.JdbcTemplate;

@Component
class DatabaseInitializer {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        // Создание таблиц для банковских данных
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(255) PRIMARY KEY,
                first_name VARCHAR(255),
                last_name VARCHAR(255)
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS products (
                id VARCHAR(255) PRIMARY KEY,
                name VARCHAR(255),
                type VARCHAR(50)
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS transactions (
                id VARCHAR(255) PRIMARY KEY,
                user_id VARCHAR(255),
                product_id VARCHAR(255),
                operation_type VARCHAR(50),
                amount DECIMAL(15,2),
                transaction_date TIMESTAMP
            )
        """);

        // Добавление тестовых данных
        initTestData();
    }

    private void initTestData() {
        // Тестовые пользователи
        jdbcTemplate.update("INSERT INTO users (id, first_name, last_name) VALUES (?, ?, ?)",
                "cd515076-5d8a-44be-930e-8d4fcb79f42d", "Иван", "Иванов");
        jdbcTemplate.update("INSERT INTO users (id, first_name, last_name) VALUES (?, ?, ?)",
                "d4a4d619-9a0c-4fc5-b0cb-76c49409546b", "Петр", "Петров");
        jdbcTemplate.update("INSERT INTO users (id, first_name, last_name) VALUES (?, ?, ?)",
                "1f9b149c-6577-448a-bc94-16bea229b71a", "Сергей", "Сергеев");

        // Тестовые продукты
        jdbcTemplate.update("INSERT INTO products (id, name, type) VALUES (?, ?, ?)",
                "prod1", "Дебетовая карта", "DEBIT");
        jdbcTemplate.update("INSERT INTO products (id, name, type) VALUES (?, ?, ?)",
                "prod2", "Кредитная карта", "CREDIT");
        jdbcTemplate.update("INSERT INTO products (id, name, type) VALUES (?, ?, ?)",
                "prod3", "Инвестиционный счет", "INVEST");
        jdbcTemplate.update("INSERT INTO products (id, name, type) VALUES (?, ?, ?)",
                "prod4", "Сберегательный счет", "SAVING");

        // Тестовые транзакции для пользователя 1 (Invest 500)
        jdbcTemplate.update("""
            INSERT INTO transactions (id, user_id, product_id, operation_type, amount, transaction_date)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)""",
                "t1", "cd515076-5d8a-44be-930e-8d4fcb79f42d", "prod1", "DEPOSIT", 50000);
        jdbcTemplate.update("""
            INSERT INTO transactions (id, user_id, product_id, operation_type, amount, transaction_date)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)""",
                "t2", "cd515076-5d8a-44be-930e-8d4fcb79f42d", "prod4", "DEPOSIT", 1500);

        // Тестовые транзакции для пользователя 2 (Top Saving)
        jdbcTemplate.update("""
            INSERT INTO transactions (id, user_id, product_id, operation_type, amount, transaction_date)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)""",
                "t3", "d4a4d619-9a0c-4fc5-b0cb-76c49409546b", "prod1", "DEPOSIT", 60000);
        jdbcTemplate.update("""
            INSERT INTO transactions (id, user_id, product_id, operation_type, amount, transaction_date)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)""",
                "t4", "d4a4d619-9a0c-4fc5-b0cb-76c49409546b", "prod1", "WITHDRAW", 40000);

        // Тестовые транзакции для пользователя 3 (Простой кредит)
        jdbcTemplate.update("""
            INSERT INTO transactions (id, user_id, product_id, operation_type, amount, transaction_date)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)""",
                "t5", "1f9b149c-6577-448a-bc94-16bea229b71a", "prod1", "DEPOSIT", 80000);
        jdbcTemplate.update("""
            INSERT INTO transactions (id, user_id, product_id, operation_type, amount, transaction_date)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)""",
                "t6", "1f9b149c-6577-448a-bc94-16bea229b71a", "prod1", "WITHDRAW", 150000);
    }
}