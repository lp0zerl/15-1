// ПОЛНАЯ РЕАЛИЗАЦИЯ РЕКОМЕНДАТЕЛЬНОЙ СИСТЕМЫ БАНКА - ЕДИНЫЙ ФАЙЛ

package ru.example.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

// ===== DTO КЛАССЫ =====

class RecommendationResponse {
	private String userId;
	private List<Recommendation> recommendations = new ArrayList<>();

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

class StatsResponse {
	private List<RuleStat> stats;

	public StatsResponse() {}
	public StatsResponse(List<RuleStat> stats) {
		this.stats = stats;
	}

	public List<RuleStat> getStats() { return stats; }
	public void setStats(List<RuleStat> stats) { this.stats = stats; }
}

class RuleStat {
	private String ruleId;
	private Long count;

	public RuleStat() {}
	public RuleStat(String ruleId, Long count) {
		this.ruleId = ruleId;
		this.count = count;
	}

	public String getRuleId() { return ruleId; }
	public void setRuleId(String ruleId) { this.ruleId = ruleId; }
	public Long getCount() { return count; }
	public void setCount(Long count) { this.count = count; }
}

class ServiceInfo {
	private String name;
	private String version;

	public ServiceInfo() {}
	public ServiceInfo(String name, String version) {
		this.name = name;
		this.version = version;
	}

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getVersion() { return version; }
	public void setVersion(String version) { this.version = version; }
}

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

class UserInfo {
	private String id;
	private String firstName;
	private String lastName;

	public UserInfo(String id, String firstName, String lastName) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public String getId() { return id; }
	public String getFirstName() { return firstName; }
	public String getLastName() { return lastName; }
}

// ===== МОДЕЛИ БАЗЫ ДАННЫХ =====

@Entity
@Table(name = "dynamic_rules")
class DynamicRule {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "product_name", nullable = false)
	private String productName;

	@Column(name = "product_id", nullable = false, unique = true)
	private String productId;

	@Column(name = "product_text", length = 2000)
	private String productText;

	@Convert(converter = RuleQueryListConverter.class)
	@Column(name = "rule_data", length = 4000)
	private List<RuleQuery> rule;

	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	private Date createdAt;

	@Column(name = "updated_at")
	@Temporal(TemporalType.TIMESTAMP)
	private Date updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = new Date();
		updatedAt = new Date();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = new Date();
	}

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
	public Date getCreatedAt() { return createdAt; }
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
	public Date getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

@Entity
@Table(name = "rule_statistics")
class RuleStatistic {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "rule_id", nullable = false)
	private Long ruleId;

	@Column(name = "execution_count")
	private Long executionCount = 0L;

	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	private Date createdAt;

	@Column(name = "updated_at")
	@Temporal(TemporalType.TIMESTAMP)
	private Date updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = new Date();
		updatedAt = new Date();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = new Date();
	}

	public RuleStatistic() {}
	public RuleStatistic(Long ruleId) {
		this.ruleId = ruleId;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Long getRuleId() { return ruleId; }
	public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
	public Long getExecutionCount() { return executionCount; }
	public void setExecutionCount(Long executionCount) { this.executionCount = executionCount; }
	public Date getCreatedAt() { return createdAt; }
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
	public Date getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

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

// ===== РЕПОЗИТОРИИ =====

@Repository
interface DynamicRuleRepository extends JpaRepository<DynamicRule, Long> {
	Optional<DynamicRule> findByProductId(String productId);
	void deleteByProductId(String productId);
}

@Repository
interface RuleStatisticRepository extends JpaRepository<RuleStatistic, Long> {
	Optional<RuleStatistic> findByRuleId(Long ruleId);
	List<RuleStatistic> findAll();
}

@Repository
class UserRepository {
	private final JdbcTemplate jdbcTemplate;

	public UserRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<UserInfo> findUsersByName(String firstName, String lastName) {
		String sql = "SELECT DISTINCT u.id, u.first_name, u.last_name FROM users u WHERE u.first_name LIKE ? AND u.last_name LIKE ?";
		return jdbcTemplate.query(sql, (rs, rowNum) ->
						new UserInfo(rs.getString("id"), rs.getString("first_name"), rs.getString("last_name")),
				firstName + "%", lastName + "%");
	}

	public Optional<UserInfo> findUserById(String userId) {
		String sql = "SELECT id, first_name, last_name FROM users WHERE id = ?";
		try {
			UserInfo user = jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
							new UserInfo(rs.getString("id"), rs.getString("first_name"), rs.getString("last_name")),
					userId);
			return Optional.ofNullable(user);
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}

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

// ===== СЕРВИСЫ =====

interface RecommendationRule {
	Optional<Recommendation> check(String userId);
}

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

@Service
class TopSavingRule implements RecommendationRule {
	private static final String PRODUCT_ID = "59efc529-2fff-41af-baff-90ccd7402925";
	private static final String PRODUCT_NAME = "Top Saving";
	private static final String PRODUCT_DESCRIPTION = "Откройте свою собственную «Копилку» с нашим банком! «Копилка» — это уникальный банковский инструмент, который поможет вам легко и удобно накапливать деньги на важные цели.";

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
			return new RecommendationResponse(userId, new ArrayList<>());
		}

		List<Recommendation> recommendations = new ArrayList<>();

		recommendations.addAll(getStaticRecommendations(userId));
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
				.filter(rule -> dynamicRuleService.evaluateRule(userId, rule))
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

@Service
class TelegramBotService extends TelegramLongPollingBot {
	private final RecommendationService recommendationService;
	private final UserRepository userRepository;

	@Value("${telegram.bot.token:test_token}")
	private String botToken;

	@Value("${telegram.bot.username:test_bot}")
	private String botUsername;

	public TelegramBotService(RecommendationService recommendationService, UserRepository userRepository) {
		this.recommendationService = recommendationService;
		this.userRepository = userRepository;
	}

	@Override
	public String getBotToken() {
		return botToken;
	}

	@Override
	public String getBotUsername() {
		return botUsername;
	}

	@Override
	public void onUpdateReceived(Update update) {
		if (update.hasMessage() && update.getMessage().hasText()) {
			String messageText = update.getMessage().getText();
			Long chatId = update.getMessage().getChatId();

			if (messageText.equals("/start")) {
				sendHelpMessage(chatId);
			} else if (messageText.startsWith("/recommend ")) {
				handleRecommendCommand(chatId, messageText);
			} else {
				sendUnknownCommandMessage(chatId);
			}
		}
	}

	private void handleRecommendCommand(Long chatId, String messageText) {
		try {
			String[] parts = messageText.split(" ", 2);
			if (parts.length < 2) {
				sendMessage(chatId, "Пожалуйста, укажите имя пользователя: /recommend Имя Фамилия");
				return;
			}

			String username = parts[1].trim();
			String[] nameParts = username.split(" ");

			if (nameParts.length != 2) {
				sendMessage(chatId, "Пожалуйста, укажите имя и фамилию через пробел: /recommend Иван Иванов");
				return;
			}

			String firstName = nameParts[0];
			String lastName = nameParts[1];

			List<UserInfo> users = userRepository.findUsersByName(firstName, lastName);

			if (users.isEmpty()) {
				sendMessage(chatId, "Пользователь не найден");
			} else if (users.size() > 1) {
				sendMessage(chatId, "Найдено несколько пользователей. Пожалуйста, уточните запрос.");
			} else {
				UserInfo user = users.get(0);
				RecommendationResponse response = recommendationService.getRecommendations(user.getId());
				sendRecommendations(chatId, user, response);
			}

		} catch (Exception e) {
			sendMessage(chatId, "Произошла ошибка при обработке запроса");
		}
	}

	private void sendRecommendations(Long chatId, UserInfo user, RecommendationResponse response) {
		StringBuilder message = new StringBuilder();
		message.append("Здравствуйте, ").append(user.getFirstName()).append(" ").append(user.getLastName()).append("!\n\n");

		if (response.getRecommendations().isEmpty()) {
			message.append("К сожалению, у нас пока нет персональных рекомендаций для вас.");
		} else {
			message.append("Новые продукты для вас:\n\n");

			for (Recommendation recommendation : response.getRecommendations()) {
				message.append("• ").append(recommendation.getName()).append("\n");
				message.append("  ").append(recommendation.getText()).append("\n\n");
			}
		}

		sendMessage(chatId, message.toString());
	}

	private void sendHelpMessage(Long chatId) {
		String helpText = "Добро пожаловать в банк «Стар»! 🏦\n\n" +
				"Доступные команды:\n" +
				"/start - показать это сообщение\n" +
				"/recommend Имя Фамилия - получить персональные рекомендации\n\n" +
				"Пример: /recommend Иван Иванов";
		sendMessage(chatId, helpText);
	}

	private void sendUnknownCommandMessage(Long chatId) {
		sendMessage(chatId, "Неизвестная команда. Используйте /start для справки.");
	}

	private void sendMessage(Long chatId, String text) {
		SendMessage message = new SendMessage();
		message.setChatId(chatId.toString());
		message.setText(text);

		try {
			execute(message);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}
}

// ===== КОНТРОЛЛЕРЫ =====

@RestController
@RequestMapping("/api")
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

@RestController
@RequestMapping("/api/rule")
class RuleController {
	private final DynamicRuleRepository dynamicRuleRepository;
	private final RuleStatisticRepository statisticRepository;

	public RuleController(DynamicRuleRepository dynamicRuleRepository, RuleStatisticRepository statisticRepository) {
		this.dynamicRuleRepository = dynamicRuleRepository;
		this.statisticRepository = statisticRepository;
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

		RuleStatistic statistic = new RuleStatistic(savedRule.getId());
		statisticRepository.save(statistic);

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
		Optional<DynamicRule> rule = dynamicRuleRepository.findByProductId(productId);
		if (rule.isPresent()) {
			statisticRepository.findByRuleId(rule.get().getId())
					.ifPresent(statisticRepository::delete);
			dynamicRuleRepository.deleteByProductId(productId);
		}
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/stats")
	public ResponseEntity<StatsResponse> getStats() {
		List<RuleStatistic> statistics = statisticRepository.findAll();

		List<RuleStat> stats = statistics.stream()
				.map(stat -> new RuleStat(stat.getRuleId().toString(), stat.getExecutionCount()))
				.collect(Collectors.toList());

		List<DynamicRule> allRules = dynamicRuleRepository.findAll();
		for (DynamicRule rule : allRules) {
			boolean hasStat = stats.stream()
					.anyMatch(stat -> stat.getRuleId().equals(rule.getId().toString()));
			if (!hasStat) {
				stats.add(new RuleStat(rule.getId().toString(), 0L));
			}
		}

		return ResponseEntity.ok(new StatsResponse(stats));
	}
}

@RestController
@RequestMapping("/api/management")
class ManagementController {
	private final BankRepository bankRepository;
	private final BuildProperties buildProperties;

	public ManagementController(BankRepository bankRepository, BuildProperties buildProperties) {
		this.bankRepository = bankRepository;
		this.buildProperties = buildProperties;
	}

	@PostMapping("/clear-caches")
	public ResponseEntity<Void> clearCaches() {
		bankRepository.clearAllCaches();
		return ResponseEntity.ok().build();
	}

	@GetMapping("/info")
	public ResponseEntity<ServiceInfo> getServiceInfo() {
		ServiceInfo info = new ServiceInfo(
				buildProperties.getName(),
				buildProperties.getVersion()
		);
		return ResponseEntity.ok(info);
	}
}

// ===== КОНФИГУРАЦИЯ =====

@Configuration
@EnableJpaRepositories(basePackages = "ru.example.bank")
@EntityScan(basePackages = "ru.example.bank")
class DatabaseConfig {

	@Primary
	@Bean(name = "defaultDataSource")
	public DataSource defaultDataSource() {
		return DataSourceBuilder.create()
				.url("jdbc:h2:mem:bankdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
				.driverClassName("org.h2.Driver")
				.username("sa")
				.password("")
				.build();
	}

	@Bean(name = "dynamicRuleDataSource")
	public DataSource dynamicRuleDataSource() {
		return DataSourceBuilder.create()
				.url("jdbc:h2:mem:rulesdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
				.driverClassName("org.h2.Driver")
				.username("sa")
				.password("")
				.build();
	}

	@Primary
	@Bean(name = "jdbcTemplate")
	public JdbcTemplate jdbcTemplate() {
		return new JdbcTemplate(defaultDataSource());
	}

	@Bean(name = "dynamicRuleEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean dynamicRuleEntityManagerFactory() {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dynamicRuleDataSource());
		em.setPackagesToScan("ru.example.bank");

		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(vendorAdapter);

		HashMap<String, Object> properties = new HashMap<>();
		properties.put("hibernate.hbm2ddl.auto", "create-drop");
		properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		properties.put("hibernate.show_sql", "true");

		em.setJpaPropertyMap(properties);

		return em;
	}

	@Bean(name = "dynamicRuleTransactionManager")
	public PlatformTransactionManager dynamicRuleTransactionManager() {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(dynamicRuleEntityManagerFactory().getObject());
		return transactionManager;
	}
}

@Configuration
class TelegramBotConfig {

	@Bean
	public TelegramBotsApi telegramBotsApi(TelegramBotService telegramBotService) throws TelegramApiException {
		TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
		botsApi.registerBot(telegramBotService);
		return botsApi;
	}
}

// ===== ГЛАВНЫЙ КЛАСС ПРИЛОЖЕНИЯ =====

@SpringBootApplication
@EnableCaching
public class BankApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankApplication.class, args);
	}

	@Bean
	public BuildProperties buildProperties() {
		return new BuildProperties(new Properties() {{
			setProperty("name", "bank-recommendation-system");
			setProperty("version", "1.0.0");
		}});
	}
}

// ===== ИНИЦИАЛИЗАЦИЯ БАЗЫ ДАННЫХ =====

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

// ===== APPLICATION.PROPERTIES КАК СТРОКОВАЯ КОНСТАНТА =====

class AppProperties {
	public static final String[] PROPERTIES = {
			"spring.application.name=bank-recommendation-system",
			"server.port=8080",
			"spring.datasource.url=jdbc:h2:mem:bankdb",
			"spring.datasource.driver-class-name=org.h2.Driver",
			"spring.datasource.username=sa",
			"spring.datasource.password=",
			"spring.datasource.dynamic-rules.url=jdbc:h2:mem:rulesdb",
			"spring.datasource.dynamic-rules.driver-class-name=org.h2.Driver",
			"spring.datasource.dynamic-rules.username=sa",
			"spring.datasource.dynamic-rules.password=",
			"spring.jpa.hibernate.ddl-auto=create-drop",
			"spring.jpa.show-sql=true",
			"spring.h2.console.enabled=true",
			"spring.h2.console.path=/h2-console",
			"telegram.bot.token=test_token",
			"telegram.bot.username=test_bot",
			"logging.level.ru.example.bank=DEBUG"
	};
}