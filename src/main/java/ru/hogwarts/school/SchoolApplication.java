package com.example.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.persistence.*;
import javax.sql.DataSource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

// ==================== 1. MAIN APPLICATION ====================
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class RecommendationApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecommendationApplication.class, args);
    }
}

// ==================== 2. CONFIGURATION CLASSES ====================
@Configuration
class AppConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .maximumSize(10000));
        return cacheManager;
    }
}

@Configuration
@EnableTransactionManagement
class DatabaseConfig {

    @Primary
    @Bean(name = "knowledgeDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.knowledge")
    public DataSource knowledgeDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "rulesDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.rules")
    public DataSource rulesDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "knowledgeEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean knowledgeEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(knowledgeDataSource());
        em.setPackagesToScan("com.example.recommendation");
        em.setPersistenceUnitName("knowledge");

        Properties props = new Properties();
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        em.setJpaProperties(props);

        return em;
    }

    @Bean(name = "rulesEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean rulesEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(rulesDataSource());
        em.setPackagesToScan("com.example.recommendation");
        em.setPersistenceUnitName("rules");

        Properties props = new Properties();
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        em.setJpaProperties(props);

        return em;
    }

    @Primary
    @Bean(name = "knowledgeTransactionManager")
    public PlatformTransactionManager knowledgeTransactionManager() {
        return new JpaTransactionManager(knowledgeEntityManagerFactory().getObject());
    }

    @Bean(name = "rulesTransactionManager")
    public PlatformTransactionManager rulesTransactionManager() {
        return new JpaTransactionManager(rulesEntityManagerFactory().getObject());
    }
}

// ==================== 3. ENTITY CLASSES ====================
@Entity
@Table(name = "user_transactions")
class UserTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "product_type", nullable = false)
    private String productType;
    @Column(name = "transaction_type", nullable = false)
    private String transactionType;
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    public UserTransaction() {}
    public UserTransaction(UUID userId, String productType, String transactionType, BigDecimal amount, LocalDateTime transactionDate) {
        this.userId = userId;
        this.productType = productType;
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionDate = transactionDate;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
}

@Entity
@Table(name = "user_profiles")
class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name", nullable = false)
    private String lastName;
    @Column(name = "username", nullable = false, unique = true)
    private String username;
    @Column(name = "telegram_id")
    private String telegramId;

    public UserProfile() {}
    public UserProfile(String firstName, String lastName, String username) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getTelegramId() { return telegramId; }
    public void setTelegramId(String telegramId) { this.telegramId = telegramId; }
    public String getFullName() { return firstName + " " + lastName; }
}

enum QueryType {
    USER_OF,
    ACTIVE_USER_OF,
    TRANSACTION_SUM_COMPARE,
    TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW
}

@Entity
@Table(name = "dynamic_rules")
class RuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "product_name", nullable = false)
    private String productName;
    @Column(name = "product_id", nullable = false)
    private String productId;
    @Column(name = "product_text", nullable = false, columnDefinition = "TEXT")
    private String productText;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "rule")
    private List<RuleQueryEntity> queries = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "rule")
    private List<RuleStatisticEntity> statistics = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductText() { return productText; }
    public void setProductText(String productText) { this.productText = productText; }
    public List<RuleQueryEntity> getQueries() { return queries; }
    public void setQueries(List<RuleQueryEntity> queries) {
        this.queries = queries;
        if (queries != null) queries.forEach(q -> q.setRule(this));
    }
    public List<RuleStatisticEntity> getStatistics() { return statistics; }
    public void setStatistics(List<RuleStatisticEntity> statistics) {
        this.statistics = statistics;
        if (statistics != null) statistics.forEach(s -> s.setRule(this));
    }
}

@Entity
@Table(name = "rule_queries")
class RuleQueryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "query_type", nullable = false) @Enumerated(EnumType.STRING)
    private QueryType queryType;
    @Column(name = "arguments", nullable = false, columnDefinition = "TEXT")
    private String arguments;
    @Column(name = "negate", nullable = false)
    private boolean negate;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rule_id", nullable = false)
    private RuleEntity rule;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public QueryType getQueryType() { return queryType; }
    public void setQueryType(QueryType queryType) { this.queryType = queryType; }
    public String getArguments() { return arguments; }
    public void setArguments(String arguments) { this.arguments = arguments; }
    public boolean isNegate() { return negate; }
    public void setNegate(boolean negate) { this.negate = negate; }
    public RuleEntity getRule() { return rule; }
    public void setRule(RuleEntity rule) { this.rule = rule; }
    public List<String> getArgumentsAsList() {
        return arguments == null || arguments.isEmpty() ? Collections.emptyList() : Arrays.asList(arguments.split(","));
    }
    public void setArgumentsFromList(List<String> args) {
        this.arguments = args == null || args.isEmpty() ? "" : String.join(",", args);
    }
}

@Entity
@Table(name = "rule_statistics")
class RuleStatisticEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rule_id", nullable = false)
    private RuleEntity rule;
    @Column(name = "count", nullable = false)
    private Long count = 0L;

    public RuleStatisticEntity() {}
    public RuleStatisticEntity(RuleEntity rule) { this.rule = rule; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RuleEntity getRule() { return rule; }
    public void setRule(RuleEntity rule) { this.rule = rule; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
    public void increment() { this.count++; }
    @PreRemove public void preRemove() {
        if (rule != null && rule.getStatistics() != null) rule.getStatistics().remove(this);
    }
}

// ==================== 4. REPOSITORIES ====================
@Repository interface UserKnowledgeRepository extends JpaRepository<UserTransaction, UUID> {
    @Query("SELECT COUNT(DISTINCT t.id) FROM UserTransaction t WHERE t.userId = :userId AND t.productType = :productType")
    Long countTransactionsByUserAndProductType(@Param("userId") UUID userId, @Param("productType") String productType);
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM UserTransaction t WHERE t.userId = :userId AND t.productType = :productType AND t.transactionType = :transactionType")
    BigDecimal sumTransactionsByType(@Param("userId") UUID userId, @Param("productType") String productType, @Param("transactionType") String transactionType);
    @Query("SELECT COUNT(DISTINCT t.id) > 0 FROM UserTransaction t WHERE t.userId = :userId AND t.productType = :productType")
    boolean existsByUserAndProductType(@Param("userId") UUID userId, @Param("productType") String productType);
}

@Repository interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByUsername(String username);
    List<UserProfile> findByFirstNameAndLastName(String firstName, String lastName);
    Optional<UserProfile> findByTelegramId(String telegramId);
}

@Repository interface DynamicRuleRepository extends JpaRepository<RuleEntity, Long> {}

@Repository interface RuleStatisticRepository extends JpaRepository<RuleStatisticEntity, Long> {
    Optional<RuleStatisticEntity> findByRuleId(Long ruleId);
    @Modifying @Transactional @Query("UPDATE RuleStatisticEntity r SET r.count = r.count + 1 WHERE r.rule.id = :ruleId")
    void incrementCount(@Param("ruleId") Long ruleId);
    @Modifying @Transactional @Query("DELETE FROM RuleStatisticEntity r WHERE r.rule.id = :ruleId")
    void deleteByRuleId(@Param("ruleId") Long ruleId);
}

class DynamicRuleRequest {
    private String product_name; private UUID product_id; private String product_text; private List<RuleQueryDto> rule;
    public String getProduct_name() { return product_name; } public void setProduct_name(String product_name) { this.product_name = product_name; }
    public UUID getProduct_id() { return product_id; } public void setProduct_id(UUID product_id) { this.product_id = product_id; }
    public String getProduct_text() { return product_text; } public void setProduct_text(String product_text) { this.product_text = product_text; }
    public List<RuleQueryDto> getRule() { return rule; } public void setRule(List<RuleQueryDto> rule) { this.rule = rule; }
}

class RuleQueryDto {
    private final String query; private final List<String> arguments; private final boolean negate;
    @JsonCreator public RuleQueryDto(@JsonProperty("query") String query, @JsonProperty("arguments") List<String> arguments, @JsonProperty("negate") boolean negate) {
        this.query = query; this.arguments = arguments; this.negate = negate;
    }
    public String getQuery() { return query; } public List<String> getArguments() { return arguments; } public boolean isNegate() { return negate; }
}

class DynamicRuleResponse {
    private Long id; private String product_name; private UUID product_id; private String product_text; private List<RuleQueryDto> rule;
    public DynamicRuleResponse() {}
    public DynamicRuleResponse(Long id, String product_name, UUID product_id, String product_text, List<RuleQueryDto> rule) {
        this.id = id; this.product_name = product_name; this.product_id = product_id; this.product_text = product_text; this.rule = rule;
    }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getProduct_name() { return product_name; } public void setProduct_name(String product_name) { this.product_name = product_name; }
    public UUID getProduct_id() { return product_id; } public void setProduct_id(UUID product_id) { this.product_id = product_id; }
    public String getProduct_text() { return product_text; } public void setProduct_text(String product_text) { this.product_text = product_text; }
    public List<RuleQueryDto> getRule() { return rule; } public void setRule(List<RuleQueryDto> rule) { this.rule = rule; }
}

class RulesListResponse { private List<DynamicRuleResponse> data;
    public RulesListResponse() {} public RulesListResponse(List<DynamicRuleResponse> data) { this.data = data; }
    public List<DynamicRuleResponse> getData() { return data; } public void setData(List<DynamicRuleResponse> data) { this.data = data; }
}

class RuleStatsDto { private Long rule_id; private Long count;
    public RuleStatsDto() {} public RuleStatsDto(Long rule_id, Long count) { this.rule_id = rule_id; this.count = count; }
    public Long getRule_id() { return rule_id; } public void setRule_id(Long rule_id) { this.rule_id = rule_id; }
    public Long getCount() { return count; } public void setCount(Long count) { this.count = count; }
}

class RuleStatsResponse { private List<RuleStatsDto> stats;
    public RuleStatsResponse() {} public RuleStatsResponse(List<RuleStatsDto> stats) { this.stats = stats; }
    public List<RuleStatsDto> getStats() { return stats; } public void setStats(List<RuleStatsDto> stats) { this.stats = stats; }
}

class ProductRecommendation {
    private UUID productId; private String productName; private String recommendationText;
    public ProductRecommendation() {}
    public ProductRecommendation(UUID productId, String productName, String recommendationText) {
        this.productId = productId; this.productName = productName; this.recommendationText = recommendationText;
    }
    public UUID getProductId() { return productId; } public void setProductId(UUID productId) { this.productId = productId; }
    public String getProductName() { return productName; } public void setProductName(String productName) { this.productName = productName; }
    public String getRecommendationText() { return recommendationText; } public void setRecommendationText(String recommendationText) { this.recommendationText = recommendationText; }
    @Override public String toString() { return String.format("• %s: %s", productName, recommendationText); }
}

// ==================== 6. SERVICES ====================
@Service class DynamicRuleService {
    private final DynamicRuleRepository ruleRepository; private final RuleStatisticService statisticService;
    public DynamicRuleService(DynamicRuleRepository ruleRepository, RuleStatisticService statisticService) {
        this.ruleRepository = ruleRepository; this.statisticService = statisticService;
    }

    @Transactional(transactionManager = "rulesTransactionManager")
    public DynamicRuleResponse createRule(DynamicRuleRequest request) {
        RuleEntity entity = new RuleEntity(); entity.setProductName(request.getProduct_name());
        entity.setProductId(request.getProduct_id().toString()); entity.setProductText(request.getProduct_text());
        List<RuleQueryEntity> queries = request.getRule().stream().map(dto -> {
            RuleQueryEntity queryEntity = new RuleQueryEntity(); queryEntity.setQueryType(QueryType.valueOf(dto.getQuery()));
            queryEntity.setArgumentsFromList(dto.getArguments()); queryEntity.setNegate(dto.isNegate()); queryEntity.setRule(entity);
            return queryEntity; }).collect(Collectors.toList());
        entity.setQueries(queries); RuleEntity saved = ruleRepository.save(entity);
        statisticService.createStatisticForRule(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true, transactionManager = "rulesTransactionManager")
    public List<DynamicRuleResponse> getAllRules() {
        return ruleRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(transactionManager = "rulesTransactionManager") public void deleteRule(Long id) { ruleRepository.deleteById(id); }
    @Transactional(readOnly = true, transactionManager = "rulesTransactionManager") public RuleEntity findRuleById(Long id) { return ruleRepository.findById(id).orElse(null); }

    private DynamicRuleResponse toResponse(RuleEntity entity) {
        List<RuleQueryDto> queries = entity.getQueries().stream()
                .map(query -> new RuleQueryDto(query.getQueryType().name(), query.getArgumentsAsList(), query.isNegate()))
                .collect(Collectors.toList());
        return new DynamicRuleResponse(entity.getId(), entity.getProductName(), UUID.fromString(entity.getProductId()), entity.getProductText(), queries);
    }
}

@Service class RuleStatisticService {
    private final RuleStatisticRepository statisticRepository; private final DynamicRuleRepository ruleRepository;
    public RuleStatisticService(RuleStatisticRepository statisticRepository, DynamicRuleRepository ruleRepository) {
        this.statisticRepository = statisticRepository; this.ruleRepository = ruleRepository;
    }

    @Transactional(transactionManager = "rulesTransactionManager") public void createStatisticForRule(RuleEntity rule) {
        RuleStatisticEntity statistic = new RuleStatisticEntity(rule); statisticRepository.save(statistic);
    }

    @Transactional(transactionManager = "rulesTransactionManager") public void incrementRuleStatistic(Long ruleId) {
        statisticRepository.incrementCount(ruleId);
    }

    @Transactional(readOnly = true, transactionManager = "rulesTransactionManager")
    public RuleStatsResponse getAllStatistics() {
        List<RuleEntity> allRules = ruleRepository.findAll();
        List<RuleStatsDto> stats = allRules.stream().map(rule -> {
            Long count = statisticRepository.findByRuleId(rule.getId()).map(RuleStatisticEntity::getCount).orElse(0L);
            return new RuleStatsDto(rule.getId(), count);
        }).collect(Collectors.toList());
        return new RuleStatsResponse(stats);
    }

    @Transactional(transactionManager = "rulesTransactionManager") public void deleteStatisticsForRule(Long ruleId) {
        statisticRepository.deleteByRuleId(ruleId);
    }
}

@Service class RuleEvaluationService {
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

@Service class EnhancedRecommendationService {
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

@Service class UserRecommendationService {
    private final UserProfileRepository userProfileRepository; private final EnhancedRecommendationService recommendationService;
    public UserRecommendationService(UserProfileRepository userProfileRepository, EnhancedRecommendationService recommendationService) {
        this.userProfileRepository = userProfileRepository; this.recommendationService = recommendationService;
    }

    public Optional<UserProfile> findUserByUsername(String username) { return userProfileRepository.findByUsername(username); }
    public List<UserProfile> findUsersByName(String firstName, String lastName) { return userProfileRepository.findByFirstNameAndLastName(firstName, lastName); }

    public String getRecommendationsForUsername(String username) {
        Optional<UserProfile> userOpt = findUserByUsername(username);
        if (userOpt.isEmpty()) return "Пользователь не найден";
        UserProfile user = userOpt.get(); List<ProductRecommendation> recommendations = recommendationService.getRecommendations(user.getId());
        if (recommendations.isEmpty()) return String.format("Здравствуйте %s\n\nНет новых рекомендаций для вас.", user.getFullName());
        StringBuilder response = new StringBuilder(); response.append(String.format("Здравствуйте %s\n\n", user.getFullName()));
        response.append("Новые продукты для вас:\n"); for (ProductRecommendation rec : recommendations) response.append(rec.toString()).append("\n");
        return response.toString();
    }
}

// ==================== 7. TELEGRAM BOT ====================
@Component class TelegramBot extends TelegramLongPollingBot {
    private final String botToken = "test_bot_token"; private final String botUsername = "test_recommendation_bot";
    private final UserRecommendationService userRecommendationService;
    public TelegramBot(UserRecommendationService userRecommendationService) { this.userRecommendationService = userRecommendationService; }
    @Override public String getBotToken() { return botToken; } @Override public String getBotUsername() { return botUsername; }

    @Override public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage(); String text = message.getText(); Long chatId = message.getChatId();
            if (text.startsWith("/start")) sendHelpMessage(chatId);
            else if (text.startsWith("/recommend")) handleRecommendCommand(chatId, text);
            else sendHelpMessage(chatId);
        }
    }

    private void handleRecommendCommand(Long chatId, String text) {
        try { String[] parts = text.split(" ", 2);
            if (parts.length < 2) { sendMessage(chatId, "Использование: /recommend username"); return; }
            String username = parts[1].trim(); String recommendations = userRecommendationService.getRecommendationsForUsername(username);
            sendMessage(chatId, recommendations);
        } catch (Exception e) { sendMessage(chatId, "Произошла ошибка при обработке запроса"); e.printStackTrace(); }
    }

    private void sendHelpMessage(Long chatId) {
        String helpText = """
                Привет! Я бот для получения финансовых рекомендаций.
                
                Доступные команды:
                /start - показать это сообщение
                /recommend <username> - получить рекомендации для пользователя
                
                Пример:
                /recommend ivanov
                """;
        sendMessage(chatId, helpText);
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(); message.setChatId(chatId.toString()); message.setText(text);
        try { execute(message); } catch (TelegramApiException e) { e.printStackTrace(); }
    }
}

@Configuration class TelegramBotConfig {
    @Bean public TelegramBotsApi telegramBotsApi(TelegramBot telegramBot) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class); botsApi.registerBot(telegramBot); return botsApi;
    }
}

// ==================== 8. CONTROLLERS ====================
@RestController @RequestMapping("/rule") class DynamicRuleController {
    private final DynamicRuleService dynamicRuleService; public DynamicRuleController(DynamicRuleService dynamicRuleService) { this.dynamicRuleService = dynamicRuleService; }

    @PostMapping public ResponseEntity<DynamicRuleResponse> createRule(@Valid @RequestBody DynamicRuleRequest request) {
        DynamicRuleResponse response = dynamicRuleService.createRule(request); return ResponseEntity.ok(response);
    }

    @GetMapping public ResponseEntity<RulesListResponse> getAllRules() {
        List<DynamicRuleResponse> rules = dynamicRuleService.getAllRules(); return ResponseEntity.ok(new RulesListResponse(rules));
    }

    @DeleteMapping("/{id}") public ResponseEntity<Void> deleteRule(@PathVariable Long id) { dynamicRuleService.deleteRule(id); return ResponseEntity.noContent().build(); }
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) { return ResponseEntity.badRequest().body(e.getMessage()); }
}

@RestController @RequestMapping("/rule") class RuleStatsController {
    private final RuleStatisticService ruleStatisticService; public RuleStatsController(RuleStatisticService ruleStatisticService) { this.ruleStatisticService = ruleStatisticService; }
    @GetMapping("/stats") public ResponseEntity<RuleStatsResponse> getRuleStatistics() { RuleStatsResponse stats = ruleStatisticService.getAllStatistics(); return ResponseEntity.ok(stats); }
}

@RestController @RequestMapping("/recommendation") class RecommendationController {
    private final EnhancedRecommendationService recommendationService; public RecommendationController(EnhancedRecommendationService recommendationService) { this.recommendationService = recommendationService; }
    @GetMapping("/{userId}") public List<ProductRecommendation> getRecommendations(@PathVariable UUID userId) { return recommendationService.getRecommendations(userId); }
}

@RestController @RequestMapping("/management") class ManagementController {
    private final EnhancedRecommendationService recommendationService; public ManagementController(EnhancedRecommendationService recommendationService) { this.recommendationService = recommendationService; }
    @PostMapping("/clear-caches") public ResponseEntity<String> clearCaches() { recommendationService.clearAllCaches(); return ResponseEntity.ok("All caches cleared successfully"); }
    @GetMapping("/info") public ResponseEntity<Map<String, String>> getServiceInfo() { Map<String, String> info = new HashMap<>(); info.put("name", "recommendation-service"); info.put("version", "1.0.0"); return ResponseEntity.ok(info); }
}

// ==================== 9. DATA INITIALIZER ====================
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
