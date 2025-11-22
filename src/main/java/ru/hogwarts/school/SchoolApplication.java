// ЕДИНЫЙ ФАЙЛ - ПОЛНАЯ РЕАЛИЗАЦИЯ РЕКОМЕНДАТЕЛЬНОЙ СИСТЕМЫ БАНКА

// 1. ВСЕ МОДЕЛИ ДАННЫХ В ОДНОМ ФАЙЛЕ
package ru.example.bank;

import javax.persistence.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.*;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.math.BigDecimal;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

// DTO для ответа рекомендаций
class RecommendationResponse {
	private String userId;
	private List<Recommendation> recommendations;

	public RecommendationResponse() {}
	public RecommendationResponse(String userId, List<Recommendation> recommendations) {
		this.userId = userId;
		this.recommendations = recommendations;
	}

	public String getUserId() { return userId; }
	public void setUserId(String userId) { this.userId = userId; }
	public List<Recommendation> getRecommendations() { return recommendations; }
	public void setRecommendations(List<Recommendation> recommendations) { this.recommendations = recommendations; }
}

// Модель рекомендации
class Recommendation {
	private String name;
	private String id;
	private String text;

	public Recommendation() {}
	public Recommendation(String name, String id, String text) {
		this.name = name;
		this.id = id;
		this.text = text;
	}

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	public String getText() { return text; }
	public void setText(String text) { this.text = text; }
}

// Модель динамического правила
@Entity
@Table(name = "dynamic_rules")
class DynamicRule {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String productName;
	private String productId;

	@Column(length = 2000)
	private String productText;

	@Convert(converter = RuleQueryListConverter.class)
	@Column(length = 4000)
	private List<RuleQuery> rule;

	public DynamicRule() {}
	public DynamicRule(String productName, String productId, String productText, List<RuleQuery> rule) {
		this.productName = productName;
		this.productId = productId;
		this.productText = productText;
		this.rule = rule;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getProductName() { return productName; }
	public void setProductName(String productName) { this.productName = productName; }
	public String getProductId() { return productId; }
	public void setProductId(String productId) { this.productId = productId; }
	public String getProductText() { return productText; }
	public void setProductText(String productText) { this.productText = productText; }
	public List<RuleQuery> getRule() { return rule; }
	public void setRule(List<RuleQuery> rule) { this.rule = rule; }
}

// Модель запроса правила
class RuleQuery {
	private String query;
	private List<String> arguments;
	private boolean negate;

	public RuleQuery() {}
	public RuleQuery(String query, List<String> arguments, boolean negate) {
		this.query = query;
		this.arguments = arguments;
		this.negate = negate;
	}

	public String getQuery() { return query; }
	public void setQuery(String query) { this.query = query; }
	public List<String> getArguments() { return arguments; }
	public void setArguments(List<String> arguments) { this.arguments = arguments; }
	public boolean isNegate() { return negate; }
	public void setNegate(boolean negate) { this.negate = negate; }
}

// Конвертер для списка RuleQuery
@Converter
class RuleQueryListConverter implements AttributeConverter<List<RuleQuery>, String> {
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(List<RuleQuery> ruleQueries) {
		try {
			return objectMapper.writeValueAsString(ruleQueries);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error converting rule queries to JSON", e);
		}
	}

	@Override
	public List<RuleQuery> convertToEntityAttribute(String dbData) {
		try {
			return objectMapper.readValue(dbData,
					objectMapper.getTypeFactory().constructCollectionType(List.class, RuleQuery.class));
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error converting JSON to rule queries", e);
		}
	}
}

// DTO для API правил
class CreateRuleRequest {
	private String productName;
	private String productId;
	private String productText;
	private List<RuleQuery> rule;

	public CreateRuleRequest() {}

	public String getProductName() { return productName; }
	public void setProductName(String productName) { this.productName = productName; }
	public String getProductId() { return productId; }
	public void setProductId(String productId) { this.productId = productId; }
	public String getProductText() { return productText; }
	public void setProductText(String productText) { this.productText = productText; }
	public List<RuleQuery> getRule() { return rule; }
	public void setRule(List<RuleQuery> rule) { this.rule = rule; }
}

class RuleResponse {
	private Long id;
	private String productName;
	private String productId;
	private String productText;
	private List<RuleQuery> rule;

	public RuleResponse() {}
	public RuleResponse(Long id, String productName, String productId, String productText, List<RuleQuery> rule) {
		this.id = id;
		this.productName = productName;
		this.productId = productId;
		this.productText = productText;
		this.rule = rule;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getProductName() { return productName; }
	public void setProductName(String productName) { this.productName = productName; }
	public String getProductId() { return productId; }
	public void setProductId(String productId) { this.productId = productId; }
	public String getProductText() { return productText; }
	public void setProductText(String productText) { this.productText = productText; }
	public List<RuleQuery> getRule() { return rule; }
	public void setRule(List<RuleQuery> rule) { this.rule = rule; }
}

class RulesListResponse {
	private List<RuleResponse> data;

	public RulesListResponse() {}
	public RulesListResponse(List<RuleResponse> data) {
		this.data = data;
	}

	public List<RuleResponse> getData() { return data; }
	public void setData(List<RuleResponse> data) { this.data = data; }
}

// 2. РЕПОЗИТОРИИ
// Репозиторий для динамических правил
@Repository
interface DynamicRuleRepository extends JpaRepository<DynamicRule, Long> {
	Optional<DynamicRule> findByProductId(String productId);
	void deleteByProductId(String productId);
}

// Репозиторий банковских данных с кешированием
@Repository
class BankRepository {
	private final JdbcTemplate jdbcTemplate;

	// Кеши с Caffeine
	private final Cache<String, Boolean> userOfCache;
	private final Cache<String, Boolean> activeUserOfCache;
	private final Cache<String, BigDecimal> transactionSumCache;
	private final Cache<String, Boolean> depositWithdrawCompareCache;

	public BankRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;

		// Инициализация кешей
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

// 3. СЕРВИСЫ
// Интерфейс статических правил
interface RecommendationRule {
	Optional<Recommendation> check(String userId);
}

// Статические правила рекомендаций
@Component
class Invest500Rule implements RecommendationRule {
	private static final String PRODUCT_ID = "147f6a0f-3b91-413b-ab99-87f081d60d5a";
	private static final String PRODUCT_NAME = "Invest 500";
	private static final String PRODUCT_DESCRIPTION = "Откройте свой путь к успеху с индивидуальным инвестиционным счетом (ИИС) от нашего банка!";

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

@Component
class TopSavingRule implements RecommendationRule {
	private static final String PRODUCT_ID = "59efc529-2fff-41af-baff-90ccd7402925";
	private static final String PRODUCT_NAME = "Top Saving";
	private static final String PRODUCT_DESCRIPTION = "Откройте свою собственную «Копилку» с нашим банком!";

	private final BankRepository bankRepository;

	public TopSavingRule(BankRepository bankRepository) {
		this.bankRepository = bankRepository;
	}

	@Override
	public Optional<Recommendation> check(String userId) {
		boolean usesDebit = bankRepository.usesProductType(userId, "DEBIT");
		BigDecimal debitDeposits = bankRepository.getTotalDebitDeposits(userId);
		BigDecimal savingDeposits = bankRepository.getTotalSavingDeposits(userId);
		boolean depositsOver50k = debitDeposits.compareTo(new BigDecimal("50000")) >= 0 ||
				savingDeposits.compareTo(new BigDecimal("50000")) >= 0;
		BigDecimal debitExpenses = bankRepository.getTotalDebitExpenses(userId);
		boolean depositsGreaterThanExpenses = debitDeposits.compareTo(debitExpenses) > 0;

		if (usesDebit && depositsOver50k && depositsGreaterThanExpenses) {
			return Optional.of(new Recommendation(PRODUCT_NAME, PRODUCT_ID, PRODUCT_DESCRIPTION));
		}
		return Optional.empty();
	}
}

@Component
class SimpleCreditRule implements RecommendationRule {
	private static final String PRODUCT_ID = "ab138afb-f3ba-4a93-b74f-0fcee86d447f";
	private static final String PRODUCT_NAME = "Простой кредит";
	private static final String PRODUCT_DESCRIPTION = "Откройте мир выгодных кредитов с нами!";

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

// Сервис для выполнения динамических правил
@Service
class DynamicRuleService {
	private final BankRepository bankRepository;

	public DynamicRuleService(BankRepository bankRepository) {
		this.bankRepository = bankRepository;
	}

	public boolean evaluateRule(String userId, List<RuleQuery> ruleQueries) {
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
}

// Основной сервис рекомендаций
@Service
class RecommendationService {
	private final List<RecommendationRule> staticRecommendationRules;
	private final BankRepository bankRepository;
	private final DynamicRuleService dynamicRuleService;
	private final DynamicRuleRepository dynamicRuleRepository;

	public RecommendationService(List<RecommendationRule> staticRecommendationRules,
								 BankRepository bankRepository,
								 DynamicRuleService dynamicRuleService,
								 DynamicRuleRepository dynamicRuleRepository) {
		this.staticRecommendationRules = staticRecommendationRules;
		this.bankRepository = bankRepository;
		this.dynamicRuleService = dynamicRuleService;
		this.dynamicRuleRepository = dynamicRuleRepository;
	}

	public RecommendationResponse getRecommendations(String userId) {
		if (!bankRepository.userExists(userId)) {
			return new RecommendationResponse(userId, List.of());
		}

		List<Recommendation> recommendations = new ArrayList<>();

		// Статические правила
		recommendations.addAll(getStaticRecommendations(userId));

		// Динамические правила
		recommendations.addAll(getDynamicRecommendations(userId));

		return new RecommendationResponse(userId, recommendations);
	}

	private List<Recommendation> getStaticRecommendations(String userId) {
		return staticRecommendationRules.stream()
				.map(rule -> rule.check(userId))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toList());
	}

	private List<Recommendation> getDynamicRecommendations(String userId) {
		List<DynamicRule> dynamicRules = dynamicRuleRepository.findAll();

		return dynamicRules.stream()
				.filter(rule -> dynamicRuleService.evaluateRule(userId, rule.getRule()))
				.map(this::convertToRecommendation)
				.collect(Collectors.toList());
	}

	private Recommendation convertToRecommendation(DynamicRule dynamicRule) {
		return new Recommendation(
				dynamicRule.getProductName(),
				dynamicRule.getProductId(),
				dynamicRule.getProductText()
		);
	}
}

// 4. КОНТРОЛЛЕРЫ
// Контроллер рекомендаций
@RestController
class RecommendationController {
	private final RecommendationService recommendationService;

	public RecommendationController(RecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@GetMapping("/recommendation/{userId}")
	public RecommendationResponse getRecommendations(@PathVariable String userId) {
		return recommendationService.getRecommendations(userId);
	}
}

// Контроллер управления правилами
@RestController
@RequestMapping("/rule")
class RuleController {
	private final DynamicRuleRepository dynamicRuleRepository;

	public RuleController(DynamicRuleRepository dynamicRuleRepository) {
		this.dynamicRuleRepository = dynamicRuleRepository;
	}

	@PostMapping
	public ResponseEntity<RuleResponse> createRule(@RequestBody CreateRuleRequest request) {
		DynamicRule dynamicRule = new DynamicRule(
				request.getProductName(),
				request.getProductId(),
				request.getProductText(),
				request.getRule()
		);

		DynamicRule savedRule = dynamicRuleRepository.save(dynamicRule);

		RuleResponse response = new RuleResponse(
				savedRule.getId(),
				savedRule.getProductName(),
				savedRule.getProductId(),
				savedRule.getProductText(),
				savedRule.getRule()
		);

		return ResponseEntity.ok(response);
	}

	@GetMapping
	public ResponseEntity<RulesListResponse> getAllRules() {
		List<DynamicRule> rules = dynamicRuleRepository.findAll();

		List<RuleResponse> ruleResponses = rules.stream()
				.map(rule -> new RuleResponse(
						rule.getId(),
						rule.getProductName(),
						rule.getProductId(),
						rule.getProductText(),
						rule.getRule()
				))
				.collect(Collectors.toList());

		return ResponseEntity.ok(new RulesListResponse(ruleResponses));
	}

	@DeleteMapping("/{productId}")
	public ResponseEntity<Void> deleteRule(@PathVariable String productId) {
		dynamicRuleRepository.deleteByProductId(productId);
		return ResponseEntity.noContent().build();
	}
}

// 5. КОНФИГУРАЦИЯ
// Конфигурация баз данных
@Configuration
class DatabaseConfig {

	@Primary
	@Bean(name = "defaultDataSource")
	@ConfigurationProperties("spring.datasource")
	public DataSource defaultDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name = "dynamicRuleDataSource")
	@ConfigurationProperties("spring.datasource.dynamic-rules")
	public DataSource dynamicRuleDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Primary
	@Bean(name = "jdbcTemplate")
	public JdbcTemplate jdbcTemplate(@Qualifier("defaultDataSource") DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean(name = "dynamicRuleJdbcTemplate")
	public JdbcTemplate dynamicRuleJdbcTemplate(@Qualifier("dynamicRuleDataSource") DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean(name = "entityManagerFactory")
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(
			EntityManagerFactoryBuilder builder,
			@Qualifier("dynamicRuleDataSource") DataSource dataSource) {
		return builder
				.dataSource(dataSource)
				.packages("ru.example.bank")
				.persistenceUnit("dynamicRules")
				.build();
	}

	@Bean(name = "transactionManager")
	public PlatformTransactionManager transactionManager(
			@Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}
}

// 6. ГЛАВНЫЙ КЛАСС ПРИЛОЖЕНИЯ
@SpringBootApplication
@EnableCaching
public class BankApplication {
	public static void main(String[] args) {
		SpringApplication.run(BankApplication.class, args);
	}
}

// 7. APPLICATION.PROPERTIES (в виде строковых констант для единого файла)
class ApplicationProperties {
	public static final String[] PROPERTIES = {
			"spring.application.name=bank-recommendation-service",
			"server.port=8080",
			"spring.datasource.url=jdbc:h2:file:./transaction;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE",
			"spring.datasource.driver-class-name=org.h2.Driver",
			"spring.datasource.username=sa",
			"spring.datasource.password=",
			"spring.datasource.dynamic-rules.url=jdbc:postgresql://localhost:5432/bank_recommendations",
			"spring.datasource.dynamic-rules.driver-class-name=org.postgresql.Driver",
			"spring.datasource.dynamic-rules.username=postgres",
			"spring.datasource.dynamic-rules.password=password",
			"spring.jpa.hibernate.ddl-auto=validate",
			"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
			"spring.jpa.show-sql=true",
			"spring.liquibase.enabled=true",
			"spring.cache.type=caffeine",
			"spring.cache.caffeine.spec=maximumSize=10000,expireAfterWrite=3600s",
			"spring.h2.console.enabled=true",
			"springfox.documentation.swagger-ui.enabled=true",
			"logging.level.ru.example.bank=DEBUG"
	};
}

// 8. POM.XML (в виде комментария для справки)
/*
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>
        <dependency>
            <groupId>org.liquibase</groupId>
            <artifactId>liquibase-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-boot-starter</artifactId>
            <version>3.0.0</version>
        </dependency>
    </dependencies>
</project>
*/

// 9. ТЕСТЫ (основные)
class RecommendationServiceTest {
	// Тест проверки существования пользователя
	void testUserExists() {
		BankRepository repo = new BankRepository(new JdbcTemplate());
		// Тестовая логика
	}

	// Тест статических правил
	void testStaticRules() {
		Invest500Rule rule = new Invest500Rule(new BankRepository(new JdbcTemplate()));
		// Тестовая логика
	}

	// Тест динамических правил
	void testDynamicRules() {
		DynamicRuleService service = new DynamicRuleService(new BankRepository(new JdbcTemplate()));
		// Тестовая логика
	}
}

// 10. ПРИМЕРЫ ИСПОЛЬЗОВАНИЯ API
class ApiExamples {
	// Пример запроса на создание правила
	void createRuleExample() {
		CreateRuleRequest request = new CreateRuleRequest();
		request.setProductName("Новый продукт");
		request.setProductId("uuid-123");
		request.setProductText("Описание нового продукта");

		List<RuleQuery> rules = Arrays.asList(
				new RuleQuery("USER_OF", Arrays.asList("DEBIT"), false),
				new RuleQuery("TRANSACTION_SUM_COMPARE",
						Arrays.asList("DEBIT", "DEPOSIT", ">", "50000"), false)
		);
		request.setRule(rules);

		// Отправка POST /rule
	}

	// Пример ответа рекомендаций
	void recommendationExample() {
		RecommendationResponse response = new RecommendationResponse(
				"user-123",
				Arrays.asList(
						new Recommendation("Invest 500", "uuid-1", "Описание Invest 500"),
						new Recommendation("Top Saving", "uuid-2", "Описание Top Saving")
				)
		);
	}
}