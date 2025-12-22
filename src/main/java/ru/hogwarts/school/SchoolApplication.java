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
}

// ==================== 3. НАСТРОЙКА SPRING SECURITY ====================
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/api/v1/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .antMatchers("/api/v1/products/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/v1/users/**").hasAnyRole("USER", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/v1/users/**").hasAnyRole("USER", "ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/v1/users/**").hasAnyRole("USER", "ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasRole("ADMIN")
                .antMatchers("/api/v1/transactions/**").hasAnyRole("USER", "ADMIN")
                .antMatchers("/api/v1/rules/**").hasRole("ADMIN")
                .antMatchers("/api/v1/recommendations/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
                .and()
                .httpBasic();

        return http.build();
    }

    @Bean
    public JdbcUserDetailsManager userDetailsManager(DataSource dataSource) {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);

        // Используем кастомные запросы для нашей структуры БД
        manager.setUsersByUsernameQuery(
                "SELECT email as username, password, enabled FROM users WHERE email = ?");
        manager.setAuthoritiesByUsernameQuery(
                "SELECT email as username, 'ROLE_' || UPPER(role) as authority FROM users WHERE email = ?");

        return manager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}

// ==================== 4. СУЩНОСТИ (ENTITIES) ====================

// Пользователь (добавляем поля для Spring Security)
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

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "role", nullable = false, length = 20)
    private String role = "USER";

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

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
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
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

// ==================== 5. DTO КЛАССЫ ====================

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

// Регистрация пользователя
@Schema(description = "Запрос на регистрацию")
class RegisterRequestDto {
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

    @Schema(description = "Пароль", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Пароль не может быть пустым")
    @Size(min = 6, message = "Пароль должен быть не менее 6 символов")
    private String password;

    @Schema(description = "Телефон", example = "+79991234567")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Некорректный формат телефона")
    private String phone;

    @Schema(description = "Роль", example = "USER")
    private String role = "USER";

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}

// Смена пароля
@Schema(description = "Запрос на смену пароля")
class ChangePasswordRequestDto {
    @Schema(description = "Текущий пароль", example = "oldPassword123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Текущий пароль не может быть пустым")
    private String currentPassword;

    @Schema(description = "Новый пароль", example = "newPassword123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Новый пароль не может быть пустым")
    @Size(min = 6, message = "Новый пароль должен быть не менее 6 символов")
    private String newPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
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

    @Schema(description = "Роль", example = "USER")
    private String role = "USER";

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
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getFullName() {
        return firstName + " " + lastName;
    }
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
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}

// Рекомендация продукта DTO
@Schema(description = "Рекомендация продукта")
class ProductRecommendationDto {
    @Schema(description = "ID рекомендации", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long id;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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

// ==================== 6. MAPPER ИНТЕРФЕЙСЫ ====================

// User Mapper
@Mapper(componentModel = "spring")
interface UserMapper {
    UserDto toDto(UserEntity entity);

    UserEntity toEntity(UserDto dto);

    UserEntity toEntity(RegisterRequestDto requestDto);

    List<UserDto> toDtoList(List<UserEntity> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UserDto dto, @MappingTarget UserEntity entity);
}

// Product Mapper
@Mapper(componentModel = "spring")
interface ProductMapper {
    ProductDto toDto(ProductEntity entity);

    ProductEntity toEntity(ProductDto dto);

    List<ProductDto> toDtoList(List<ProductEntity> entities);
}

// Transaction Mapper
@Mapper(componentModel = "spring")
interface TransactionMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "product.id", target = "productId")
    TransactionDto toDto(TransactionEntity entity);

    @Mapping(source = "userId", target = "user.id")
    @Mapping(source = "productId", target = "product.id")
    TransactionEntity toEntity(TransactionDto dto);

    List<TransactionDto> toDtoList(List<TransactionEntity> entities);
}

// Rule Mapper
@Mapper(componentModel = "spring")
interface RuleMapper {
    @Mapping(source = "product.id", target = "productId")
    RuleDto toDto(RuleEntity entity);

    @Mapping(source = "productId", target = "product.id")
    RuleEntity toEntity(RuleDto dto);

    List<RuleDto> toDtoList(List<RuleEntity> entities);
}

// Recommendation Mapper
@Mapper(componentModel = "spring")
interface RecommendationMapper {
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    ProductRecommendationDto toDto(RecommendationEntity entity);

    List<ProductRecommendationDto> toDtoList(List<RecommendationEntity> entities);
}

// ==================== 7. РЕПОЗИТОРИИ ====================

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

    @Query("SELECT SUM(t.amount) FROM TransactionEntity t WHERE t.user.id = :userId AND t.type = :type")
    BigDecimal sumAmountByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    @Query("SELECT COUNT(t) FROM TransactionEntity t WHERE t.user.id = :userId AND t.product.type = :productType")
    Long countByUserIdAndProductType(@Param("userId") Long userId, @Param("productType") String productType);
}

// Rule Repository
@Repository
interface RuleRepository extends JpaRepository<RuleEntity, Long> {
    List<RuleEntity> findByActiveTrue();

    List<RuleEntity> findByProductId(Long productId);
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

// ==================== 8. СЕРВИСЫ ====================

// Auth Service
@Service
@Transactional
class AuthService {
    @Autowired
    private JdbcUserDetailsManager userDetailsManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    public void register(RegisterRequestDto requestDto) {
        if (userDetailsManager.userExists(requestDto.getEmail())) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }

        // Создаем пользователя через JdbcUserDetailsManager
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(requestDto.getEmail())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .roles(requestDto.getRole())
                .build();

        userDetailsManager.createUser(userDetails);

        // Дополнительно сохраняем в нашу таблицу users
        UserEntity user = new UserEntity();
        user.setEmail(requestDto.getEmail());
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        user.setFirstName(requestDto.getFirstName());
        user.setLastName(requestDto.getLastName());
        user.setPhone(requestDto.getPhone());
        user.setRole(requestDto.getRole());
        user.setEnabled(true);
        user.setActive(true);

        userRepository.save(user);
    }

    public void changePassword(String email, ChangePasswordRequestDto requestDto) {
        // Получаем пользователя
        UserDetails userDetails = userDetailsManager.loadUserByUsername(email);

        // Проверяем текущий пароль
        if (!passwordEncoder.matches(requestDto.getCurrentPassword(), userDetails.getPassword())) {
            throw new BadCredentialsException("Текущий пароль неверен");
        }

        // Меняем пароль
        userDetailsManager.changePassword(
                requestDto.getCurrentPassword(),
                passwordEncoder.encode(requestDto.getNewPassword())
        );

        // Обновляем пароль в нашей таблице
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
        user.setPassword(passwordEncoder.encode(requestDto.getNewPassword()));
        userRepository.save(user);
    }

    public UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .map(user -> {
                    UserDto dto = new UserDto();
                    dto.setId(user.getId());
                    dto.setFirstName(user.getFirstName());
                    dto.setLastName(user.getLastName());
                    dto.setEmail(user.getEmail());
                    dto.setPhone(user.getPhone());
                    dto.setRole(user.getRole());
                    dto.setRegistrationDate(user.getRegistrationDate());
                    dto.setActive(user.isActive());
                    return dto;
                })
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
    }
}

// User Service
@Service
@Transactional
class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<UserDto> getAllUsers() {
        return userMapper.toDtoList(userRepository.findAll());
    }

    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto);
    }

    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public Optional<UserDto> updateUser(Long id, UserDto userDto) {
        return userRepository.findById(id)
                .map(existing -> {
                    // Проверка прав: только ADMIN может менять роль
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    boolean isAdmin = auth.getAuthorities().stream()
                            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

                    if (!isAdmin && !existing.getRole().equals(userDto.getRole())) {
                        throw new AccessDeniedException("Только администратор может изменять роль пользователя");
                    }

                    userMapper.updateEntityFromDto(userDto, existing);
                    UserEntity updated = userRepository.save(existing);
                    return userMapper.toDto(updated);
                });
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
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

    public List<ProductDto> getAllProducts() {
        return productMapper.toDtoList(productRepository.findAll());
    }

    public Optional<ProductDto> getProductById(Long id) {
        return productRepository.findById(id)
                .map(productMapper::toDto);
    }

    public List<ProductDto> getActiveProducts() {
        return productMapper.toDtoList(productRepository.findByActiveTrue());
    }

    public List<ProductDto> getProductsByType(String type) {
        return productMapper.toDtoList(productRepository.findByTypeAndActiveTrue(type));
    }

    public ProductDto createProduct(ProductDto productDto) {
        ProductEntity entity = productMapper.toEntity(productDto);
        entity = productRepository.save(entity);
        return productMapper.toDto(entity);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Optional<ProductDto> updateProduct(Long id, ProductDto productDto) {
        return productRepository.findById(id)
                .map(existing -> {
                    productMapper.toDto(existing); // Для обновления нужно отдельное отображение
                    ProductEntity updated = productRepository.save(existing);
                    return productMapper.toDto(updated);
                });
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
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

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public TransactionDto createTransaction(TransactionDto transactionDto) {
        // Проверка прав: пользователь может создавать транзакции только для себя
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        UserEntity currentUser = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        // Проверяем, что пользователь создает транзакцию для себя
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !currentUser.getId().equals(transactionDto.getUserId())) {
            throw new AccessDeniedException("Вы можете создавать транзакции только для себя");
        }

        UserEntity user = userRepository.findById(transactionDto.getUserId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        ProductEntity product = productRepository.findById(transactionDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Продукт не найден"));

        TransactionEntity entity = transactionMapper.toEntity(transactionDto);
        entity.setUser(user);
        entity.setProduct(product);

        entity = transactionRepository.save(entity);
        return transactionMapper.toDto(entity);
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<TransactionDto> getUserTransactions(Long userId) {
        // Проверка прав: пользователь может видеть только свои транзакции
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        UserEntity currentUser = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Вы можете просматривать только свои транзакции");
        }

        return transactionMapper.toDtoList(transactionRepository.findByUserId(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<TransactionDto> getAllTransactions() {
        return transactionMapper.toDtoList(transactionRepository.findAll());
    }

    public BigDecimal getUserTransactionSum(Long userId, String type) {
        BigDecimal sum = transactionRepository.sumAmountByUserIdAndType(userId, type);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public Map<String, Object> getUserTransactionSummary(Long userId) {
        // Проверка прав
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        UserEntity currentUser = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Вы можете просматривать только свою статистику");
        }

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
    private ProductRepository productRepository;

    @Autowired
    private RuleMapper ruleMapper;

    @PreAuthorize("hasRole('ADMIN')")
    public RuleDto createRule(RuleDto ruleDto) {
        ProductEntity product = productRepository.findById(ruleDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Продукт не найден"));

        RuleEntity entity = ruleMapper.toEntity(ruleDto);
        entity.setProduct(product);

        entity = ruleRepository.save(entity);
        return ruleMapper.toDto(entity);
    }

    public List<RuleDto> getAllRules() {
        return ruleMapper.toDtoList(ruleRepository.findAll());
    }

    public List<RuleDto> getActiveRules() {
        return ruleMapper.toDtoList(ruleRepository.findByActiveTrue());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Optional<RuleDto> updateRule(Long id, RuleDto ruleDto) {
        return ruleRepository.findById(id)
                .map(existing -> {
                    ProductEntity product = productRepository.findById(ruleDto.getProductId())
                            .orElseThrow(() -> new RuntimeException("Продукт не найден"));

                    existing.setProductName(ruleDto.getProductName());
                    existing.setProduct(product);
                    existing.setProductText(ruleDto.getProductText());
                    existing.setActive(ruleDto.isActive());

                    RuleEntity updated = ruleRepository.save(existing);
                    return ruleMapper.toDto(updated);
                });
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteRule(Long id) {
        if (ruleRepository.existsById(id)) {
            ruleRepository.deleteById(id);
            return true;
        }
        return false;
    }
}

// Recommendation Service
@Service
@Transactional
class RecommendationService {
    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private RecommendationMapper recommendationMapper;

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<ProductRecommendationDto> getUserRecommendations(Long userId) {
        // Проверка прав
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        UserEntity currentUser = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Вы можете просматривать только свои рекомендации");
        }

        List<RecommendationEntity> recommendations = recommendationRepository.findByUserId(userId);
        return recommendationMapper.toDtoList(recommendations);
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<ProductRecommendationDto> getNewRecommendations(Long userId) {
        // Проверка прав (аналогично getUserRecommendations)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        UserEntity currentUser = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Вы можете просматривать только свои рекомендации");
        }

        List<RecommendationEntity> recommendations = recommendationRepository.findByUserIdAndViewedFalse(userId);
        return recommendationMapper.toDtoList(recommendations);
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void markAsViewed(Long recommendationId) {
        RecommendationEntity recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("Рекомендация не найдена"));

        // Проверка прав
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        UserEntity currentUser = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !recommendation.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Вы можете отмечать только свои рекомендации");
        }

        recommendation.setViewed(true);
        recommendationRepository.save(recommendation);
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void markAllAsViewed(Long userId) {
        // Проверка прав
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        UserEntity currentUser = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Вы можете отмечать только свои рекомендации");
        }

        recommendationRepository.markAllAsViewedByUserId(userId);
    }

    // Метод для генерации рекомендаций на основе правил
    public void generateRecommendationsForUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<RuleEntity> activeRules = ruleRepository.findByActiveTrue();

        for (RuleEntity rule : activeRules) {
            // Проверяем условия правила (упрощенная логика)
            boolean shouldRecommend = checkRuleConditions(user, rule);

            if (shouldRecommend) {
                // Проверяем, не рекомендовали ли уже этот продукт
                boolean alreadyRecommended = recommendationRepository
                        .findByUserIdAndProductId(userId, rule.getProduct().getId())
                        .stream()
                        .anyMatch(r -> !r.isViewed());

                if (!alreadyRecommended) {
                    RecommendationEntity recommendation = new RecommendationEntity();
                    recommendation.setUser(user);
                    recommendation.setProduct(rule.getProduct());
                    recommendation.setRecommendationText(rule.getProductText());
                    recommendation.setViewed(false);

                    recommendationRepository.save(recommendation);
                }
            }
        }
    }

    private boolean checkRuleConditions(UserEntity user, RuleEntity rule) {
        // Упрощенная логика проверки условий правила
        // В реальном приложении здесь была бы сложная логика анализа транзакций

        // Пример: если у пользователя есть транзакции по кредитным продуктам
        Long creditTransactionsCount = transactionRepository
                .countByUserIdAndProductType(user.getId(), "CREDIT");

        return creditTransactionsCount > 0;
    }
}

// ==================== 9. КОНТРОЛЛЕРЫ ====================

// Auth Controller
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/auth")
@Tag(name = "Аутентификация", description = "API для регистрации и аутентификации")
class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Регистрация нового пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "409", description = "Пользователь уже существует")
    })
    public ResponseEntity<DataResponseDto<Void>> register(@Valid @RequestBody RegisterRequestDto requestDto) {
        authService.register(requestDto);
        return ResponseEntity.ok(new DataResponseDto<>(null, "Пользователь успешно зарегистрирован"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Смена пароля")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пароль успешно изменен"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "401", description = "Неверный текущий пароль")
    })
    public ResponseEntity<DataResponseDto<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto requestDto,
            Authentication authentication) {
        authService.changePassword(authentication.getName(), requestDto);
        return ResponseEntity.ok(new DataResponseDto<>(null, "Пароль успешно изменен"));
    }

    @GetMapping("/me")
    @Operation(summary = "Получить информацию о текущем пользователе")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация о пользователе"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован")
    })
    public ResponseEntity<DataResponseDto<UserDto>> getCurrentUser() {
        UserDto userDto = authService.getCurrentUser();
        return ResponseEntity.ok(new DataResponseDto<>(userDto));
    }
}

// User Controller
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/users")
@Tag(name = "Пользователи", description = "API для управления пользователями")
class UserController {
    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получить всех пользователей")
    public ResponseEntity<DataResponseDto<List<UserDto>>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(new DataResponseDto<>(users));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь найден"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<DataResponseDto<UserDto>> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(new DataResponseDto<>(user)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new DataResponseDto<>(null, "Пользователь не найден")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь обновлен"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<DataResponseDto<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDto userDto) {
        return userService.updateUser(id, userDto)
                .map(user -> ResponseEntity.ok(new DataResponseDto<>(user, "Пользователь обновлен")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new DataResponseDto<>(null, "Пользователь не найден")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь удален"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<DataResponseDto<Void>> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            return ResponseEntity.ok(new DataResponseDto<>(null, "Пользователь удален"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new DataResponseDto<>(null, "Пользователь не найден"));
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Поиск пользователей")
    public ResponseEntity<DataResponseDto<List<UserDto>>> searchUsers(
            @RequestParam(required = false) String query) {
        List<UserDto> users = userService.searchUsers(query);
        return ResponseEntity.ok(new DataResponseDto<>(users));
    }
}

// Product Controller
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/products")
@Tag(name = "Продукты", description = "API для управления продуктами")
class ProductController {
    @Autowired
    private ProductService productService;

    @GetMapping
    @Operation(summary = "Получить все продукты")
    public ResponseEntity<DataResponseDto<List<ProductDto>>> getAllProducts() {
        List<ProductDto> products = productService.getAllProducts();
        return ResponseEntity.ok(new DataResponseDto<>(products));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить продукт по ID")
    public ResponseEntity<DataResponseDto<ProductDto>> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(product -> ResponseEntity.ok(new DataResponseDto<>(product)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new DataResponseDto<>(null, "Продукт не найден")));
    }

    @GetMapping("/active")
    @Operation(summary = "Получить активные продукты")
    public ResponseEntity<DataResponseDto<List<ProductDto>>> getActiveProducts() {
        List<ProductDto> products = productService.getActiveProducts();
        return ResponseEntity.ok(new DataResponseDto<>(products));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Получить продукты по типу")
    public ResponseEntity<DataResponseDto<List<ProductDto>>> getProductsByType(@PathVariable String type) {
        List<ProductDto> products = productService.getProductsByType(type);
        return ResponseEntity.ok(new DataResponseDto<>(products));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать новый продукт")
    public ResponseEntity<DataResponseDto<ProductDto>> createProduct(@Valid @RequestBody ProductDto productDto) {
        ProductDto created = productService.createProduct(productDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new DataResponseDto<>(created, "Продукт создан"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить продукт")
    public ResponseEntity<DataResponseDto<ProductDto>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductDto productDto) {
        return productService.updateProduct(id, productDto)
                .map(product -> ResponseEntity.ok(new DataResponseDto<>(product, "Продукт обновлен")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new DataResponseDto<>(null, "Продукт не найден")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить продукт")
    public ResponseEntity<DataResponseDto<Void>> deleteProduct(@PathVariable Long id) {
        boolean deleted = productService.deleteProduct(id);
        if (deleted) {
            return ResponseEntity.ok(new DataResponseDto<>(null, "Продукт удален"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new DataResponseDto<>(null, "Продукт не найден"));
        }
    }
}

// Transaction Controller
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/transactions")
@Tag(name = "Транзакции", description = "API для управления транзакциями")
class TransactionController {
    @Autowired
    private TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Создать новую транзакцию")
    public ResponseEntity<DataResponseDto<TransactionDto>> createTransaction(
            @Valid @RequestBody TransactionDto transactionDto) {
        TransactionDto created = transactionService.createTransaction(transactionDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new DataResponseDto<>(created, "Транзакция создана"));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Получить транзакции пользователя")
    public ResponseEntity<DataResponseDto<List<TransactionDto>>> getUserTransactions(@PathVariable Long userId) {
        List<TransactionDto> transactions = transactionService.getUserTransactions(userId);
        return ResponseEntity.ok(new DataResponseDto<>(transactions));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получить все транзакции")
    public ResponseEntity<DataResponseDto<List<TransactionDto>>> getAllTransactions() {
        List<TransactionDto> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(new DataResponseDto<>(transactions));
    }

    @GetMapping("/user/{userId}/summary")
    @Operation(summary = "Получить сводку по транзакциям пользователя")
    public ResponseEntity<DataResponseDto<Map<String, Object>>> getUserTransactionSummary(@PathVariable Long userId) {
        Map<String, Object> summary = transactionService.getUserTransactionSummary(userId);
        return ResponseEntity.ok(new DataResponseDto<>(summary));
    }
}

// Rule Controller
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/rules")
@Tag(name = "Правила", description = "API для управления правилами рекомендаций")
class RuleController {
    @Autowired
    private RuleService ruleService;

    @GetMapping
    @Operation(summary = "Получить все правила")
    public ResponseEntity<DataResponseDto<List<RuleDto>>> getAllRules() {
        List<RuleDto> rules = ruleService.getAllRules();
        return ResponseEntity.ok(new DataResponseDto<>(rules));
    }

    @GetMapping("/active")
    @Operation(summary = "Получить активные правила")
    public ResponseEntity<DataResponseDto<List<RuleDto>>> getActiveRules() {
        List<RuleDto> rules = ruleService.getActiveRules();
        return ResponseEntity.ok(new DataResponseDto<>(rules));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать новое правило")
    public ResponseEntity<DataResponseDto<RuleDto>> createRule(@Valid @RequestBody RuleDto ruleDto) {
        RuleDto created = ruleService.createRule(ruleDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new DataResponseDto<>(created, "Правило создано"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить правило")
    public ResponseEntity<DataResponseDto<RuleDto>> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody RuleDto ruleDto) {
        return ruleService.updateRule(id, ruleDto)
                .map(rule -> ResponseEntity.ok(new DataResponseDto<>(rule, "Правило обновлено")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new DataResponseDto<>(null, "Правило не найдено")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить правило")
    public ResponseEntity<DataResponseDto<Void>> deleteRule(@PathVariable Long id) {
        boolean deleted = ruleService.deleteRule(id);
        if (deleted) {
            return ResponseEntity.ok(new DataResponseDto<>(null, "Правило удалено"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new DataResponseDto<>(null, "Правило не найдено"));
        }
    }
}

// Recommendation Controller
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/recommendations")
@Tag(name = "Рекомендации", description = "API для управления рекомендациями")
class RecommendationController {
    @Autowired
    private RecommendationService recommendationService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Получить рекомендации пользователя")
    public ResponseEntity<DataResponseDto<List<ProductRecommendationDto>>> getUserRecommendations(
            @PathVariable Long userId) {
        List<ProductRecommendationDto> recommendations = recommendationService.getUserRecommendations(userId);
        return ResponseEntity.ok(new DataResponseDto<>(recommendations));
    }

    @GetMapping("/user/{userId}/new")
    @Operation(summary = "Получить новые рекомендации пользователя")
    public ResponseEntity<DataResponseDto<List<ProductRecommendationDto>>> getNewRecommendations(
            @PathVariable Long userId) {
        List<ProductRecommendationDto> recommendations = recommendationService.getNewRecommendations(userId);
        return ResponseEntity.ok(new DataResponseDto<>(recommendations));
    }

    @PostMapping("/{id}/view")
    @Operation(summary = "Отметить рекомендацию как просмотренную")
    public ResponseEntity<DataResponseDto<Void>> markAsViewed(@PathVariable Long id) {
        recommendationService.markAsViewed(id);
        return ResponseEntity.ok(new DataResponseDto<>(null, "Рекомендация отмечена как просмотренная"));
    }

    @PostMapping("/user/{userId}/view-all")
    @Operation(summary = "Отметить все рекомендации как просмотренные")
    public ResponseEntity<DataResponseDto<Void>> markAllAsViewed(@PathVariable Long userId) {
        recommendationService.markAllAsViewed(userId);
        return ResponseEntity.ok(new DataResponseDto<>(null, "Все рекомендации отмечены как просмотренные"));
    }
}

// ==================== 10. ГЛОБАЛЬНЫЙ ОБРАБОТЧИК ИСКЛЮЧЕНИЙ ====================

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentialsException(BadCredentialsException ex) {
        ErrorResponseDto error = new ErrorResponseDto("Неверные учетные данные", 401);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorResponseDto error = new ErrorResponseDto("Доступ запрещен", 403);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        ErrorResponseDto error = new ErrorResponseDto("Пользователь не найден", 404);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponseDto error = new ErrorResponseDto("Ошибка валидации", 400);
        error.setDetails(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDto> handleRuntimeException(RuntimeException ex) {
        ErrorResponseDto error = new ErrorResponseDto(ex.getMessage(), 500);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
