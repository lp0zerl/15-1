package data;

import dto.RuleQueryDto;
import repository.UserKnowledgeRepository;
import repository.UserProfileRepository;
import request.DynamicRuleRequest;
import ru.hogwarts.school.UserProfile;
import ru.hogwarts.school.UserTransaction;
import service.DynamicRuleService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component class DataInitializer implements org.springframework.boot.CommandLineRunner {
    private final UserProfileRepository userProfileRepository; private final UserKnowledgeRepository userKnowledgeRepository;
    private final DynamicRuleService dynamicRuleService;
    public DataInitializer(UserProfileRepository userProfileRepository, UserKnowledgeRepository userKnowledgeRepository, DynamicRuleService dynamicRuleService) {
        this.userProfileRepository = userProfileRepository; this.userKnowledgeRepository = userKnowledgeRepository; this.dynamicRuleService = dynamicRuleService;
    }

    @Override public void run(String... args) {
        // Создание тестовых пользователей
        UserProfile user1 = new UserProfile("Иван", "Иванов", "ivanov");
        UserProfile user2 = new UserProfile("Петр", "Петров", "petrov");
        UserProfile user3 = new UserProfile("Мария", "Сидорова", "sidorova");
        userProfileRepository.saveAll(Arrays.asList(user1, user2, user3));

        // Создание тестовых транзакций для Иванова
        UUID userId = user1.getId();
        List<UserTransaction> transactions = Arrays.asList(
                new UserTransaction(userId, "DEBIT", "DEPOSIT", new BigDecimal("150000"), LocalDateTime.now().minusDays(10)),
                new UserTransaction(userId, "DEBIT", "DEPOSIT", new BigDecimal("50000"), LocalDateTime.now().minusDays(5)),
                new UserTransaction(userId, "DEBIT", "WITHDRAW", new BigDecimal("30000"), LocalDateTime.now().minusDays(3)),
                new UserTransaction(userId, "DEBIT", "WITHDRAW", new BigDecimal("20000"), LocalDateTime.now().minusDays(2)),
                new UserTransaction(userId, "CREDIT", "DEPOSIT", new BigDecimal("100000"), LocalDateTime.now().minusDays(1))
        );
        userKnowledgeRepository.saveAll(transactions);

        // Создание динамического правила
        DynamicRuleRequest ruleRequest = new DynamicRuleRequest();
        ruleRequest.setProduct_name("Простой кредит");
        ruleRequest.setProduct_id(UUID.fromString("ab138afb-f3ba-4a93-b74f-0fcee86d447f"));
        ruleRequest.setProduct_text("Рекомендуем простой кредит на выгодных условиях");
        List<RuleQueryDto> queries = Arrays.asList(
                new RuleQueryDto("USER_OF", Arrays.asList("CREDIT"), true),
                new RuleQueryDto("TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW", Arrays.asList("DEBIT", ">"), false),
                new RuleQueryDto("TRANSACTION_SUM_COMPARE", Arrays.asList("DEBIT", "DEPOSIT", ">", "100000"), false)
        );
        ruleRequest.setRule(queries);

        try { dynamicRuleService.createRule(ruleRequest); System.out.println("✓ Динамическое правило создано"); }
        catch (Exception e) { System.out.println("⚠ Правило уже существует или ошибка: " + e.getMessage()); }

        System.out.println("======================================");
        System.out.println("✅ Тестовые данные инициализированы!");
        System.out.println("👤 Тестовый пользователь: ivanov");
        System.out.println("🔑 ID пользователя: " + userId);
        System.out.println("🤖 Telegram бот готов к работе");
        System.out.println("🌐 Сервер запущен на http://localhost:8080");
        System.out.println("======================================");
        System.out.println("\n📋 Доступные API эндпоинты:");
        System.out.println("  POST   /rule                  - Создать правило");
        System.out.println("  GET    /rule                  - Получить все правила");
        System.out.println("  GET    /rule/stats            - Статистика правил");
        System.out.println("  DELETE /rule/{id}             - Удалить правило");
        System.out.println("  GET    /recommendation/{id}   - Рекомендации по ID");
        System.out.println("  POST   /management/clear-caches - Очистить кеш");
        System.out.println("  GET    /management/info       - Информация о сервисе");
        System.out.println("\n🤖 Команды Telegram бота:");
        System.out.println("  /start                       - Помощь");
        System.out.println("  /recommend ivanov           - Рекомендации для пользователя");
        System.out.println("======================================");
    }
}
