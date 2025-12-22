package com.example.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// ==================== 1. ГЛАВНОЕ ПРИЛОЖЕНИЕ ====================
@SpringBootApplication
@EntityScan(basePackages = "com.example.recommendation")
@EnableJpaRepositories(basePackages = "com.example.recommendation")
@ComponentScan(basePackages = "com.example.recommendation")
public class RecommendationApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecommendationApplication.class, args);
        System.out.println("🚀 Приложение запущено на http://localhost:8080");
        System.out.println("📚 Swagger UI: http://localhost:8080/swagger-ui.html");
        System.out.println("🐘 PostgreSQL: jdbc:postgresql://localhost:5432/recommendation_db");
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:3000", "http://localhost:8080")
                        .allowedMethods("*")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**")
                .build();
    }
}

// ==================== 2. КОНСТАНТЫ ====================
class AppConstants {
    public static final String API_PREFIX = "/api/v1";

    // Сообщения
    public static final String ERROR_NOT_FOUND = "Ресурс не найден";
    public static final String ERROR_VALIDATION = "Ошибка валидации";
    public static final String SUCCESS_CREATED = "Создано успешно";
    public static final String SUCCESS_UPDATED = "Обновлено успешно";
    public static final String SUCCESS_DELETED = "Удалено успешно";

    // Типы продуктов
    public static final List<String> PRODUCT_TYPES = Arrays.asList("DEBIT", "CREDIT", "INVEST", "SAVING");

    // Типы транзакций
    public static final List<String> TRANSACTION_TYPES = Arrays.asList("DEPOSIT", "WITHDRAW");

    // Типы запросов правил
    public static final List<String> QUERY_TYPES = Arrays.asList(
            "USER_OF", "ACTIVE_USER_OF",
            "TRANSACTION_SUM_COMPARE", "TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW"
    );

    // Операторы сравнения
    public static final List<String> COMPARISON_OPERATORS = Arrays.asList(">", "<", "=", ">=", "<=");
}

// ==================== 3. СУЩНОСТИ (ENTITIES) ====================

// Пользователь
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_phone", columnList = "phone")
})
class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TransactionEntity> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RecommendationEntity> recommendations = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        registrationDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<TransactionEntity> getTransactions() { return transactions; }
    public void setTransactions(List<TransactionEntity> transactions) { this.transactions = transactions; }
    public List<RecommendationEntity> getRecommendations() { return recommendations; }
    public void setRecommendations(List<RecommendationEntity> recommendations) { this.recommendations = recommendations; }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}

// Продукт
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_type", columnList = "type"),
        @Index(name = "idx_product_active", columnList = "active")
})
class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "min_balance", precision = 19, scale = 2)
    private BigDecimal minBalance;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TransactionEntity> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RuleEntity> rules = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RecommendationEntity> recommendations = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getMinBalance() { return minBalance; }
    public void setMinBalance(BigDecimal minBalance) { this.minBalance = minBalance; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public List<TransactionEntity> getTransactions() { return transactions; }
    public void setTransactions(List<TransactionEntity> transactions) { this.transactions = transactions; }
    public List<RuleEntity> getRules() { return rules; }
    public void setRules(List<RuleEntity> rules) { this.rules = rules; }
    public List<RecommendationEntity> getRecommendations() { return recommendations; }
    public void setRecommendations(List<RecommendationEntity> recommendations) { this.recommendations = recommendations; }
}

// Транзакция
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_user_id", columnList = "user_id"),
        @Index(name = "idx_transaction_product_id", columnList = "product_id"),
        @Index(name = "idx_transaction_date", columnList = "transaction_date"),
        @Index(name = "idx_transaction_type", columnList = "type")
})
class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "status", length = 20)
    private String status = "COMPLETED";

    @PrePersist
    protected void onCreate() {
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public ProductEntity getProduct() { return product; }
    public void setProduct(ProductEntity product) { this.product = product; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

// Правило (для рекомендаций)
@Entity
@Table(name = "rules", indexes = {
        @Index(name = "idx_rule_product_id", columnList = "product_id"),
        @Index(name = "idx_rule_active", columnList = "active")
})
class RuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "product_text", nullable = false, length = 500)
    private String productText;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RuleQueryEntity> queries = new ArrayList<>();

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RuleStatisticEntity> statistics = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public ProductEntity getProduct() { return product; }
    public void setProduct(ProductEntity product) { this.product = product; }
    public String getProductText() { return productText; }
    public void setProductText(String productText) { this.productText = productText; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public List<RuleQueryEntity> getQueries() { return queries; }
    public void setQueries(List<RuleQueryEntity> queries) { this.queries = queries; }
    public List<RuleStatisticEntity> getStatistics() { return statistics; }
    public void setStatistics(List<RuleStatisticEntity> statistics) { this.statistics = statistics; }
}

// Запрос правила
@Entity
@Table(name = "rule_queries", indexes = {
        @Index(name = "idx_rule_query_rule_id", columnList = "rule_id")
})
class RuleQueryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private RuleEntity rule;

    @Column(name = "query_type", nullable = false, length = 50)
    private String queryType;

    @Column(name = "arguments", nullable = false, columnDefinition = "TEXT")
    private String arguments;

    @Column(name = "negate", nullable = false)
    private boolean negate = false;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RuleEntity getRule() { return rule; }
    public void setRule(RuleEntity rule) { this.rule = rule; }
    public String getQueryType() { return queryType; }
    public void setQueryType(String queryType) { this.queryType = queryType; }
    public String getArguments() { return arguments; }
    public void setArguments(String arguments) { this.arguments = arguments; }
    public boolean isNegate() { return negate; }
    public void setNegate(boolean negate) { this.negate = negate; }

    public List<String> getArgumentsAsList() {
        if (arguments == null || arguments.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(arguments.split(","));
    }

    public void setArgumentsFromList(List<String> args) {
        if (args == null || args.isEmpty()) {
            this.arguments = "";
        } else {
            this.arguments = String.join(",", args);
        }
    }
}

// Статистика правила
@Entity
@Table(name = "rule_statistics", indexes = {
        @Index(name = "idx_rule_stat_rule_id", columnList = "rule_id")
})
class RuleStatisticEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private RuleEntity rule;

    @Column(name = "count", nullable = false)
    private Long count = 0L;

    @Column(name = "last_triggered")
    private LocalDateTime lastTriggered;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RuleEntity getRule() { return rule; }
    public void setRule(RuleEntity rule) { this.rule = rule; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
    public LocalDateTime getLastTriggered() { return lastTriggered; }
    public void setLastTriggered(LocalDateTime lastTriggered) { this.lastTriggered = lastTriggered; }

    public void increment() {
        this.count++;
        this.lastTriggered = LocalDateTime.now();
    }
}

// Рекомендация
@Entity
@Table(name = "recommendations", indexes = {
        @Index(name = "idx_recommendation_user_id", columnList = "user_id"),
        @Index(name = "idx_recommendation_product_id", columnList = "product_id"),
        @Index(name = "idx_recommendation_created", columnList = "created_date")
})
class RecommendationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "recommendation_text", nullable = false, length = 500)
    private String recommendationText;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "viewed", nullable = false)
    private boolean viewed = false;

    @Column(name = "viewed_date")
    private LocalDateTime viewedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public ProductEntity getProduct() { return product; }
    public void setProduct(ProductEntity product) { this.product = product; }
    public String getRecommendationText() { return recommendationText; }
    public void setRecommendationText(String recommendationText) { this.recommendationText = recommendationText; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isViewed() { return viewed; }
    public void setViewed(boolean viewed) {
        this.viewed = viewed;
        if (viewed && viewedDate == null) {
            viewedDate = LocalDateTime.now();
        }
    }
    public LocalDateTime getViewedDate() { return viewedDate; }
    public void setViewedDate(LocalDateTime viewedDate) { this.viewedDate = viewedDate; }
}

// ==================== 4. DTO КЛАССЫ ====================

// Базовый ответ
@Schema(description = "Базовый ответ API")
class BaseResponseDto {
    @Schema(description = "Статус", example = "success", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String status;

    @Schema(description = "Сообщение", example = "Операция выполнена успешно")
    private String message;

    @Schema(description = "Временная метка", example = "2023-01-15T10:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private LocalDateTime timestamp;

    public BaseResponseDto() {
        this.timestamp = LocalDateTime.now();
    }

    public BaseResponseDto(String status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

// Ответ с ошибкой
@Schema(description = "Ответ с ошибкой")
class ErrorResponseDto extends BaseResponseDto {
    @Schema(description = "Код ошибки", example = "404", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Integer code;

    @Schema(description = "Детали ошибки")
    private Map<String, String> details;

    public ErrorResponseDto() {
        super("error", null);
    }

    public ErrorResponseDto(String message, Integer code) {
        super("error", message);
        this.code = code;
    }

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public Map<String, String> getDetails() { return details; }
    public void setDetails(Map<String, String> details) { this.details = details; }
}

// Ответ с данными
@Schema(description = "Ответ с данными")
class DataResponseDto<T> extends BaseResponseDto {
    @Schema(description = "Данные", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private T data;

    public DataResponseDto() {
        super("success", null);
    }

    public DataResponseDto(T data) {
        super("success", null);
        this.data = data;
    }

    public DataResponseDto(T data, String message) {
        super("success", message);
        this.data = data;
    }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}

// ID ответ
@Schema(description = "Ответ с ID созданного ресурса")
class IdResponseDto {
    @Schema(description = "ID ресурса", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long id;

    public IdResponseDto() {}

    public IdResponseDto(Long id) {
        this.id = id;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}

// Пользователь DTO
@Schema(description = "Пользователь")
class UserDto {
    @Schema(description = "ID пользователя", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long id;

    @Schema(description = "Имя", example = "Иван", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Имя не может быть пустым")
    @Size(min = 1, max = 50, message = "Имя должно быть от 1 до 50 символов")
    private String firstName;

    @Schema(description = "Фамилия", example = "Иванов", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Фамилия не может быть пустой")
    @Size(min = 1, max = 50, message = "Фамилия должна быть от 1 до 50 символов")
    private String lastName;

    @Schema(description = "Email", example = "ivan@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный формат email")
    @Size(max = 100, message = "Email должен быть до 100 символов")
    private String email;

    @Schema(description = "Телефон", example = "+79991234567")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Некорректный формат телефона")
    private String phone;

    @Schema(description = "Дата регистрации", example = "2023-01-15T10:30:00")
    private LocalDateTime registrationDate;

    @Schema(description = "Активен", example = "true")
    private boolean active = true;

    public UserDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}

// Запрос пользователя
@Schema(description = "Запрос на создание/обновление пользователя")
class UserRequestDto {
    @Schema(description = "Имя", example = "Иван", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Имя не может быть пустым")
    @Size(min = 1, max = 50, message = "Имя должно быть от 1 до 50 символов")
    private String firstName;

    @Schema(description = "Фамилия", example = "Иванов", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Фамилия не может быть пустой")
    @Size(min = 1, max = 50, message = "Фамилия должна быть от 1 до 50 символов")
    private String lastName;

    @Schema(description = "Email", example = "ivan@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный формат email")
    @Size(max = 100, message = "Email должен быть до 100 символов")
    private String email;

    @Schema(description = "Телефон", example = "+79991234567")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Некорректный формат телефона")
    private String phone;

    @Schema(description = "Активен", example = "true")
    private boolean active = true;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

// Продукт DTO
@Schema(description = "Продукт")
class ProductDto {
    @Schema(description = "ID продукта", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long id;

    @Schema(description = "Название", example = "Премиальная кредитная карта", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Название не может быть пустым")
    @Size(min = 1, max = 100, message = "Название должно быть от 1 до 100 символов")
    private String name;

    @Schema(description = "Тип", example = "CREDIT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Тип не может быть пустым")
    @Pattern(regexp = "DEBIT|CREDIT|INVEST|SAVING", message = "Тип должен быть одним из: DEBIT, CREDIT, INVEST, SAVING")
    private String type;

    @Schema(description = "Описание", example = "Премиальная карта с кэшбэком")
    @Size(max = 500, message = "Описание должно быть до 500 символов")
    private String description;

    @Schema(description = "Минимальный баланс", example = "1000.00")
    @DecimalMin(value = "0.00", inclusive = true, message = "Минимальный баланс не может быть отрицательным")
    private BigDecimal minBalance;

    @Schema(description = "Процентная ставка", example = "5.5")
    @DecimalMin(value = "0.00", inclusive = true, message = "Процентная ставка не может быть отрицательной")
    private BigDecimal interestRate;

    @Schema(description = "Активен", example = "true")
    private boolean active = true;

    @Schema(description = "Дата создания", example = "2023-01-15T10:30:00")
    private LocalDateTime createdDate;

    public ProductDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getMinBalance() { return minBalance; }
    public void setMinBalance(BigDecimal minBalance) { this.minBalance = minBalance; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}

// Запрос продукта
@Schema(description = "Запрос на создание/обновление продукта")
class ProductRequestDto {
    @Schema(description = "Название", example = "Премиальная кредитная карта", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Название не может быть пустым")
    @Size(min = 1, max = 100, message = "Название должно быть от 1 до 100 символов")
    private String name;

    @Schema(description = "Тип", example = "CREDIT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Тип не может быть пустым")
    @Pattern(regexp = "DEBIT|CREDIT|INVEST|SAVING", message = "Тип должен быть одним из: DEBIT, CREDIT, INVEST, SAVING")
    private String type;

    @Schema(description = "Описание", example = "Премиальная карта с кэшбэком")
    @Size(max = 500, message = "Описание должно быть до 500 символов")
    private String description;

    @Schema(description = "Минимальный баланс", example = "1000.00")
    @DecimalMin(value = "0.00", inclusive = true, message = "Минимальный баланс не может быть отрицательным")
    private BigDecimal minBalance;

    @Schema(description = "Процентная ставка", example = "5.5")
    @DecimalMin(value = "0.00", inclusive = true, message = "Процентная ставка не может быть отрицательной")
    private BigDecimal interestRate;

    @Schema(description = "Активен", example = "true")
    private boolean active = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getMinBalance() { return minBalance; }
    public void setMinBalance(BigDecimal minBalance) { this.minBalance = minBalance; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

// Транзакция DTO
@Schema(description = "Транзакция")
class TransactionDto {
    @Schema(description = "ID транзакции", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long id;

    @Schema(description = "ID пользователя", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long userId;

    @Schema(description = "ID продукта", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long productId;

    @Schema(description = "Тип", example = "DEPOSIT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Тип не может быть пустым")
    @Pattern(regexp = "DEPOSIT|WITHDRAW", message = "Тип должен быть DEPOSIT или WITHDRAW")
    private String type;

    @Schema(description = "Сумма", example = "1000.50", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Сумма не может быть пустой")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
    private BigDecimal amount;

    @Schema(description = "Дата транзакции", example = "2023-01-15T10:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private LocalDateTime transactionDate;

    @Schema(description = "Описание", example = "Зачисление зарплаты")
    @Size(max = 200, message = "Описание должно быть до 200 символов")
    private String description;

    @Schema(description = "Статус", example = "COMPLETED")
    private String status = "COMPLETED";

    public TransactionDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

// Запрос транзакции
@Schema(description = "Запрос на создание транзакции")
class TransactionRequestDto {
    @Schema(description = "ID пользователя", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID пользователя не может быть пустым")
    private Long userId;

    @Schema(description = "ID продукта", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID продукта не может быть пустым")
    private Long productId;

    @Schema(description = "Тип", example = "DEPOSIT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Тип не может быть пустым")
    @Pattern(regexp = "DEPOSIT|WITHDRAW", message = "Тип должен быть DEPOSIT или WITHDRAW")
    private String type;

    @Schema(description = "Сумма", example = "1000.50", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Сумма не может быть пустой")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
    private BigDecimal amount;

    @Schema(description = "Описание", example = "Зачисление зарплаты")
    @Size(max = 200, message = "Описание должно быть до 200 символов")
    private String description;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

// Запрос правила DTO
@Schema(description = "Компонент запроса правила")
class RuleQueryDto {
    @Schema(description = "Тип запроса", example = "USER_OF", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Тип запроса не может быть пустым")
    private String query;

    @Schema(description = "Аргументы", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Аргументы не могут быть пустыми")
    @Size(min = 1, message = "Должен быть хотя бы один аргумент")
    private List<String> arguments;

    @Schema(description = "Отрицание", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean negate = false;

    public RuleQueryDto() {}

    public RuleQueryDto(String query, List<String> arguments, boolean negate) {
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

// Правило DTO
@Schema(description = "Правило рекомендации")
class RuleDto {
    @Schema(description = "ID правила", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long id;

    @Schema(description = "Название продукта", example = "Простой кредит", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Название продукта не может быть пустым")
    @Size(min = 1, max = 100, message = "Название продукта должно быть от 1 до 100 символов")
    private String productName;

    @Schema(description = "ID продукта", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID продукта не может быть пустым")
    private Long productId;

    @Schema(description = "Текст рекомендации", example = "Рекомендуем простой кредит на выгодных условиях", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Текст рекомендации не может быть пустым")
    @Size(min = 1, max = 500, message = "Текст рекомендации должен быть от 1 до 500 символов")
    private String productText;

    @Schema(description = "Запросы правила", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Запросы правила не могут быть пустыми")
    @Size(min = 1, message = "Должен быть хотя бы один запрос")
    private List<RuleQueryDto> rule;

    @Schema(description = "Активно", example = "true")
    private boolean active = true;

    @Schema(description = "Дата создания", example = "2023-01-15T10:30:00")
    private LocalDateTime createdDate;

    public RuleDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductText() { return productText; }
    public void setProductText(String productText) { this.productText = productText; }
    public List<RuleQueryDto> getRule() { return rule; }
    public void setRule(List<RuleQueryDto> rule) { this.rule = rule; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}

// Запрос создания правила
@Schema(description = "Запрос на создание правила")
class RuleRequestDto {
    @Schema(description = "Название продукта", example = "Простой кредит", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Название продукта не может быть пустым")
    @Size(min = 1, max = 100, message = "Название продукта должно быть от 1 до 100 символов")
    private String productName;

    @Schema(description = "ID продукта", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID продукта не может быть пустым")
    private Long productId;

    @Schema(description = "Текст рекомендации", example = "Рекомендуем простой кредит на выгодных условиях", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Текст рекомендации не может быть пустым")
    @Size(min = 1, max = 500, message = "Текст рекомендации должен быть от 1 до 500 символов")
    private String productText;

    @Schema(description = "Запросы правила", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Запросы правила не могут быть пустыми")
    @Size(min = 1, message = "Должен быть хотя бы один запрос")
    private List<RuleQueryDto> rule;

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductText() { return productText; }
    public void setProductText(String productText) { this.productText = productText; }
    public List<RuleQueryDto> getRule() { return rule; }
    public void setRule(List<RuleQueryDto> rule) { this.rule = rule; }
}

// Ответ со списком правил
@Schema(description = "Ответ со списком правил")
class RulesListResponseDto {
    @Schema(description = "Список правил", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private List<RuleDto> data;

    public RulesListResponseDto() {}

    public RulesListResponseDto(List<RuleDto> data) {
        this.data = data;
    }

    public List<RuleDto> getData() { return data; }
    public void setData(List<RuleDto> data) { this.data = data; }
}

// Статистика правила DTO
@Schema(description = "Статистика правила")
class RuleStatDto {
    @Schema(description = "ID правила", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long ruleId;

    @Schema(description = "Количество срабатываний", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Min(value = 0, message = "Количество срабатываний не может быть отрицательным")
    private Long count;

    @Schema(description = "Последнее срабатывание", example = "2023-01-15T10:30:00")
    private LocalDateTime lastTriggered;

    public RuleStatDto() {}

    public RuleStatDto(Long ruleId, Long count) {
        this.ruleId = ruleId;
        this.count = count;
    }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
    public LocalDateTime getLastTriggered() { return lastTriggered; }
    public void setLastTriggered(LocalDateTime lastTriggered) { this.lastTriggered = lastTriggered; }
}

// Ответ со статистикой
@Schema(description = "Ответ со статистикой правил")
class RuleStatsResponseDto {
    @Schema(description = "Статистика", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private List<RuleStatDto> stats;

    public RuleStatsResponseDto() {}

    public RuleStatsResponseDto(List<RuleStatDto> stats) {
        this.stats = stats;
    }

    public List<RuleStatDto> getStats() { return stats; }
    public void setStats(List<RuleStatDto> stats) { this.stats = stats; }
}

// Рекомендация продукта DTO
@Schema(description = "Рекомендация продукта")
class ProductRecommendationDto {
    @Schema(description = "ID продукта", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long productId;

    @Schema(description = "Название продукта", example = "Премиальная кредитная карта", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String productName;

    @Schema(description = "Текст рекомендации", example = "Рекомендуем на основе вашей истории транзакций", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String recommendationText;

    @Schema(description = "Дата создания", example = "2023-01-15T10:30:00")
    private LocalDateTime createdDate;

    @Schema(description = "Просмотрено", example = "false")
    private boolean viewed = false;

    public ProductRecommendationDto() {}

    public ProductRecommendationDto(Long productId, String productName, String recommendationText) {
        this.productId = productId;
        this.productName = productName;
        this.recommendationText = recommendationText;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getRecommendationText() { return recommendationText; }
    public void setRecommendationText(String recommendationText) { this.recommendationText = recommendationText; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isViewed() { return viewed; }
    public void setViewed(boolean viewed) { this.viewed = viewed; }
}

// Ответ с рекомендациями
@Schema(description = "Ответ с рекомендациями")
class RecommendationsResponseDto {
    @Schema(description = "Рекомендации", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private List<ProductRecommendationDto> recommendations;

    public RecommendationsResponseDto() {}

    public RecommendationsResponseDto(List<ProductRecommendationDto> recommendations) {
        this.recommendations = recommendations;
    }

    public List<ProductRecommendationDto> getRecommendations() { return recommendations; }
    public void setRecommendations(List<ProductRecommendationDto> recommendations) { this.recommendations = recommendations; }
}

// Информация о сервисе
@Schema(description = "Информация о сервисе")
class ServiceInfoDto {
    @Schema(description = "Название", example = "recommendation-service", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String name;

    @Schema(description = "Версия", example = "1.0.0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String version;

    @Schema(description = "Статус", example = "running")
    private String status = "running";

    @Schema(description = "Время запуска", example = "2023-01-15T10:30:00")
    private LocalDateTime startupTime;

    public ServiceInfoDto() {
        this.startupTime = LocalDateTime.now();
    }

    public ServiceInfoDto(String name, String version) {
        this.name = name;
        this.version = version;
        this.startupTime = LocalDateTime.now();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartupTime() { return startupTime; }
    public void setStartupTime(LocalDateTime startupTime) { this.startupTime = startupTime; }
}

// ==================== 5. MAPPER ИНТЕРФЕЙСЫ (MapStruct) ====================

// User Mapper
@Mapper(componentModel = "spring")
interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    UserDto toDto(UserEntity entity);

    UserEntity toEntity(UserDto dto);

    UserEntity toEntity(UserRequestDto requestDto);

    List<UserDto> toDtoList(List<UserEntity> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UserDto dto, @MappingTarget UserEntity entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UserRequestDto requestDto, @MappingTarget UserEntity entity);
}

// Product Mapper
@Mapper(componentModel = "spring")
interface ProductMapper {
    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);

    ProductDto toDto(ProductEntity entity);

    ProductEntity toEntity(ProductDto dto);

    ProductEntity toEntity(ProductRequestDto requestDto);

    List<ProductDto> toDtoList(List<ProductEntity> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(ProductDto dto, @MappingTarget ProductEntity entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(ProductRequestDto requestDto, @MappingTarget ProductEntity entity);
}

// Transaction Mapper
@Mapper(componentModel = "spring")
interface TransactionMapper {
    TransactionMapper INSTANCE = Mappers.getMapper(TransactionMapper.class);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "product.id", target = "productId")
    TransactionDto toDto(TransactionEntity entity);

    @Mapping(source = "userId", target = "user.id")
    @Mapping(source = "productId", target = "product.id")
    TransactionEntity toEntity(TransactionDto dto);

    @Mapping(source = "userId", target = "user.id")
    @Mapping(source = "productId", target = "product.id")
    TransactionEntity toEntity(TransactionRequestDto requestDto);

    List<TransactionDto> toDtoList(List<TransactionEntity> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(TransactionDto dto, @MappingTarget TransactionEntity entity);
}

// Rule Mapper
@Mapper(componentModel = "spring")
interface RuleMapper {
    RuleMapper INSTANCE = Mappers.getMapper(RuleMapper.class);

    @Mapping(source = "product.id", target = "productId")
    RuleDto toDto(RuleEntity entity);

    @Mapping(source = "productId", target = "product.id")
    RuleEntity toEntity(RuleDto dto);

    @Mapping(source = "productId", target = "product.id")
    RuleEntity toEntity(RuleRequestDto requestDto);

    List<RuleDto> toDtoList(List<RuleEntity> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(RuleDto dto, @MappingTarget RuleEntity entity);
}

// Rule Query Mapper
@Mapper(componentModel = "spring")
interface RuleQueryMapper {
    RuleQueryMapper INSTANCE = Mappers.getMapper(RuleQueryMapper.class);

    RuleQueryDto toDto(RuleQueryEntity entity);

    RuleQueryEntity toEntity(RuleQueryDto dto);

    List<RuleQueryDto> toDtoList(List<RuleQueryEntity> entities);

    List<RuleQueryEntity> toEntityList(List<RuleQueryDto> dtos);
}

// Rule Statistic Mapper
@Mapper(componentModel = "spring")
interface RuleStatisticMapper {
    RuleStatisticMapper INSTANCE = Mappers.getMapper(RuleStatisticMapper.class);

    @Mapping(source = "rule.id", target = "ruleId")
    RuleStatDto toDto(RuleStatisticEntity entity);

    @Mapping(source = "ruleId", target = "rule.id")
    RuleStatisticEntity toEntity(RuleStatDto dto);

    List<RuleStatDto> toDtoList(List<RuleStatisticEntity> entities);
}

// Recommendation Mapper
@Mapper(componentModel = "spring")
interface RecommendationMapper {
    RecommendationMapper INSTANCE = Mappers.getMapper(RecommendationMapper.class);

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    ProductRecommendationDto toDto(RecommendationEntity entity);

    @Mapping(source = "productId", target = "product.id")
    @Mapping(source = "productName", target = "product.name")
    RecommendationEntity toEntity(ProductRecommendationDto dto);

    List<ProductRecommendationDto> toDtoList(List<RecommendationEntity> entities);
}

// ==================== 6. РЕПОЗИТОРИИ ====================

// User Repository
@Repository
interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByPhone(String phone);

    List<UserEntity> findByActiveTrue();

    List<UserEntity> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String lastName, String email);

    boolean existsByEmail(String email);

    long countByActiveTrue();
}

// Product Repository
@Repository
interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    List<ProductEntity> findByType(String type);

    List<ProductEntity> findByActiveTrue();

    List<ProductEntity> findByTypeAndActiveTrue(String type);

    List<ProductEntity> findByNameContainingIgnoreCase(String name);

    boolean existsByNameAndType(String name, String type);
}

// Transaction Repository
@Repository
interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    List<TransactionEntity> findByUserId(Long userId);

    List<TransactionEntity> findByProductId(Long productId);

    List<TransactionEntity> findByUserIdAndProductId(Long userId, Long productId);

    List<TransactionEntity> findByUserIdAndType(Long userId, String type);

    List<TransactionEntity> findByUserIdAndTransactionDateBetween(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(t.amount) FROM TransactionEntity t WHERE t.user.id = :userId AND t.type = :type")
    BigDecimal sumAmountByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    @Query("SELECT COUNT(t) FROM TransactionEntity t WHERE t.user.id = :userId AND t.product.type = :productType")
    Long countByUserIdAndProductType(@Param("userId") Long userId, @Param("productType") String productType);

    @Query("SELECT SUM(t.amount) FROM TransactionEntity t WHERE t.user.id = :userId AND t.product.type = :productType AND t.type = :transactionType")
    BigDecimal sumByUserAndProductTypeAndTransactionType(
            @Param("userId") Long userId,
            @Param("productType") String productType,
            @Param("transactionType") String transactionType);
}

// Rule Repository
@Repository
interface RuleRepository extends JpaRepository<RuleEntity, Long> {
    List<RuleEntity> findByActiveTrue();

    List<RuleEntity> findByProductId(Long productId);

    List<RuleEntity> findByProductIdAndActiveTrue(Long productId);
}

// Rule Query Repository
@Repository
interface RuleQueryRepository extends JpaRepository<RuleQueryEntity, Long> {
    List<RuleQueryEntity> findByRuleId(Long ruleId);

    void deleteByRuleId(Long ruleId);
}

// Rule Statistic Repository
@Repository
interface RuleStatisticRepository extends JpaRepository<RuleStatisticEntity, Long> {
    Optional<RuleStatisticEntity> findByRuleId(Long ruleId);

    @Modifying
    @Query("UPDATE RuleStatisticEntity r SET r.count = r.count + 1, r.lastTriggered = CURRENT_TIMESTAMP WHERE r.rule.id = :ruleId")
    void incrementCount(@Param("ruleId") Long ruleId);

    void deleteByRuleId(Long ruleId);
}

// Recommendation Repository
@Repository
interface RecommendationRepository extends JpaRepository<RecommendationEntity, Long> {
    List<RecommendationEntity> findByUserId(Long userId);

    List<RecommendationEntity> findByUserIdAndViewedFalse(Long userId);

    List<RecommendationEntity> findByUserIdAndProductId(Long userId, Long productId);

    long countByUserIdAndViewedFalse(Long userId);

    @Modifying
    @Query("UPDATE RecommendationEntity r SET r.viewed = true, r.viewedDate = CURRENT_TIMESTAMP WHERE r.user.id = :userId AND r.viewed = false")
    void markAllAsViewedByUserId(@Param("userId") Long userId);
}

// ==================== 7. СЕРВИСЫ ====================

// User Service
@Service
@Transactional
class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    public UserDto createUser(UserRequestDto requestDto) {
        // Проверка уникальности email
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }

        UserEntity entity = userMapper.toEntity(requestDto);
        entity = userRepository.save(entity);
        return userMapper.toDto(entity);
    }

    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto);
    }

    public List<UserDto> getAllUsers() {
        return userMapper.toDtoList(userRepository.findAll());
    }

    public List<UserDto> getActiveUsers() {
        return userMapper.toDtoList(userRepository.findByActiveTrue());
    }

    public Optional<UserDto> updateUser(Long id, UserRequestDto requestDto) {
        return userRepository.findById(id)
                .map(existing -> {
                    // Проверка уникальности email (кроме текущего пользователя)
                    if (!existing.getEmail().equals(requestDto.getEmail()) &&
                            userRepository.existsByEmail(requestDto.getEmail())) {
                        throw new RuntimeException("Пользователь с таким email уже существует");
                    }

                    userMapper.updateEntityFromRequest(requestDto, existing);
                    UserEntity updated = userRepository.save(existing);
                    return userMapper.toDto(updated);
                });
    }

    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<UserDto> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDto);
    }

    public long countActiveUsers() {
        return userRepository.countByActiveTrue();
    }

    public List<UserDto> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllUsers();
        }

        String searchQuery = query.trim();
        List<UserEntity> results = userRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        searchQuery, searchQuery, searchQuery);
        return userMapper.toDtoList(results);
    }
}

// Product Service
@Service
@Transactional
class ProductService {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMapper productMapper;

    public ProductDto createProduct(ProductRequestDto requestDto) {
        // Проверка уникальности
        if (productRepository.existsByNameAndType(requestDto.getName(), requestDto.getType())) {
            throw new RuntimeException("Продукт с таким названием и типом уже существует");
        }

        ProductEntity entity = productMapper.toEntity(requestDto);
        entity = productRepository.save(entity);
        return productMapper.toDto(entity);
    }

    public Optional<ProductDto> getProductById(Long id) {
        return productRepository.findById(id)
                .map(productMapper::toDto);
    }

    public List<ProductDto> getAllProducts() {
        return productMapper.toDtoList(productRepository.findAll());
    }

    public List<ProductDto> getActiveProducts() {
        return productMapper.toDtoList(productRepository.findByActiveTrue());
    }

    public List<ProductDto> getProductsByType(String type) {
        return productMapper.toDtoList(productRepository.findByTypeAndActiveTrue(type));
    }

    public Optional<ProductDto> updateProduct(Long id, ProductRequestDto requestDto) {
        return productRepository.findById(id)
                .map(existing -> {
                    // Проверка уникальности (кроме текущего продукта)
                    if ((!existing.getName().equals(requestDto.getName()) ||
                            !existing.getType().equals(requestDto.getType())) &&
                            productRepository.existsByNameAndType(requestDto.getName(), requestDto.getType())) {
                        throw new RuntimeException("Продукт с таким названием и типом уже существует");
                    }

                    productMapper.updateEntityFromRequest(requestDto, existing);
                    ProductEntity updated = productRepository.save(existing);
                    return productMapper.toDto(updated);
                });
    }

    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<ProductDto> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllProducts();
        }

        String searchQuery = query.trim();
        List<ProductEntity> results = productRepository.findByNameContainingIgnoreCase(searchQuery);
        return productMapper.toDtoList(results);
    }
}

// Transaction Service
@Service
@Transactional
class TransactionService {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TransactionMapper transactionMapper;

    public TransactionDto createTransaction(TransactionRequestDto requestDto) {
        // Проверка существования пользователя и продукта
        UserEntity user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        ProductEntity product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Продукт не найден"));

        TransactionEntity entity = transactionMapper.toEntity(requestDto);
        entity.setUser(user);
        entity.setProduct(product);

        entity = transactionRepository.save(entity);
        return transactionMapper.toDto(entity);
    }

    public Optional<TransactionDto> getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .map(transactionMapper::toDto);
    }

    public List<TransactionDto> getAllTransactions() {
        return transactionMapper.toDtoList(transactionRepository.findAll());
    }

    public List<TransactionDto> getUserTransactions(Long userId) {
        return transactionMapper.toDtoList(transactionRepository.findByUserId(userId));
    }

    public List<TransactionDto> getProductTransactions(Long productId) {
        return transactionMapper.toDtoList(transactionRepository.findByProductId(productId));
    }

    public BigDecimal getUserTransactionSum(Long userId, String type) {
        BigDecimal sum = transactionRepository.sumAmountByUserIdAndType(userId, type);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public Long countUserTransactionsByProductType(Long userId, String productType) {
        Long count = transactionRepository.countByUserIdAndProductType(userId, productType);
        return count != null ? count : 0L;
    }

    public BigDecimal sumTransactionsByType(Long userId, String productType, String transactionType) {
        BigDecimal sum = transactionRepository.sumByUserAndProductTypeAndTransactionType(
                userId, productType, transactionType);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public Map<String, Object> getUserTransactionSummary(Long userId) {
        BigDecimal totalDeposits = getUserTransactionSum(userId, "DEPOSIT");
        BigDecimal totalWithdrawals = getUserTransactionSum(userId, "WITHDRAW");
        BigDecimal balance = totalDeposits.subtract(totalWithdrawals);

        List<TransactionDto> transactions = getUserTransactions(userId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", userId);
        summary.put("totalTransactions", transactions.size());
        summary.put("totalDeposits", totalDeposits);
        summary.put("totalWithdrawals", totalWithdrawals);
        summary.put("balance", balance);
        summary.put("transactions", transactions);

        return summary;
    }
}

// Rule Service
@Service
@Transactional
class RuleService {
    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private RuleQueryRepository ruleQueryRepository;

    @Autowired
    private RuleStatisticRepository ruleStatisticRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RuleMapper ruleMapper;

    @Autowired
    private RuleQueryMapper ruleQueryMapper;

    @Autowired
    private RuleStatisticMapper ruleStatisticMapper;

    public RuleDto createRule(RuleRequestDto requestDto) {
        // Проверка существования продукта
        ProductEntity product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Продукт не найден"));

        RuleEntity entity = ruleMapper.toEntity(requestDto);
        entity.setProduct(product);

        // Сохраняем правило
        entity = ruleRepository.save(entity);

        // Сохраняем запросы правила
        if (requestDto.getRule() != null) {
            List<RuleQueryEntity> queries = ruleQueryMapper.toEntityList(requestDto.getRule());
            queries.forEach(query -> query.setRule(entity));
            ruleQueryRepository.saveAll(queries);
            entity.setQueries(queries);
        }

        // Создаем статистику для правила
        RuleStatisticEntity statistic = new RuleStatisticEntity();
        statistic.setRule(entity);
        statistic.setCount(0L);
        ruleStatisticRepository.save(statistic);

        return ruleMapper.toDto(entity);
    }

    public Optional<RuleDto> getRuleById(Long id) {
        return ruleRepository.findById(id)
                .map(rule -> {
                    RuleDto dto = ruleMapper.toDto(rule);
                    // Загружаем запросы
                    List<RuleQueryEntity> queries = ruleQueryRepository.findByRuleId(id);
                    List<RuleQueryDto> queryDtos = ruleQueryMapper.toDtoList(queries);
                    dto.setRule(queryDtos);
                    return dto;
                });
    }

    public List<RuleDto> getAllRules() {
        List<RuleEntity> rules = ruleRepository.findAll();
        return rules.stream()
                .map(rule -> {
                    RuleDto dto = ruleMapper.toDto(rule);
                    List<RuleQueryEntity> queries = ruleQueryRepository.findByRuleId(rule.getId());
                    List<RuleQueryDto> queryDtos = ruleQueryMapper.toDtoList(queries);
                    dto.setRule(queryDtos);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<RuleDto> getActiveRules() {
        return ruleMapper.toDtoList(ruleRepository.findByActiveTrue());
    }

    public boolean deleteRule(Long id) {
        if (ruleRepository.existsById(id)) {
            // Удаляем запросы и статистику перед удалением правила
            ruleQueryRepository.deleteByRuleId(id);
