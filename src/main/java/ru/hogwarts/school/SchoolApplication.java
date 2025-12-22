package com.example.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
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

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

// ==================== 1. ГЛАВНОЕ ПРИЛОЖЕНИЕ ====================
@SpringBootApplication
public class RecommendationApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecommendationApplication.class, args);
        System.out.println("🚀 Приложение запущено на http://localhost:8080");
        System.out.println("📚 Swagger UI: http://localhost:8080/swagger-ui.html");
        System.out.println("📊 API Docs: http://localhost:8080/api-docs");
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

    // Сообщения об ошибках
    public static final String ERROR_INTERNAL = "Внутренняя ошибка сервера";
    public static final String ERROR_NOT_FOUND = "Ресурс не найден";
    public static final String ERROR_VALIDATION = "Ошибка валидации";
    public static final String ERROR_BAD_REQUEST = "Неверный запрос";

    // Сообщения об успехе
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

// ==================== 3. DTO КЛАССЫ ====================

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
    private Object id;

    public IdResponseDto() {}

    public IdResponseDto(Object id) {
        this.id = id;
    }

    public Object getId() { return id; }
    public void setId(Object id) { this.id = id; }
}

// Пользователь DTO
@Schema(description = "Пользователь")
class UserDto {
    @Schema(description = "ID пользователя", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private UUID id;

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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
    @Schema(description = "ID продукта", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private UUID id;

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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
    @Schema(description = "ID транзакции", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private UUID id;

    @Schema(description = "ID пользователя", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private UUID userId;

    @Schema(description = "ID продукта", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private UUID productId;

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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
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
    @Schema(description = "ID пользователя", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID пользователя не может быть пустым")
    private UUID userId;

    @Schema(description = "ID продукта", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID продукта не может быть пустым")
    private UUID productId;

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

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

// Запрос правила
@Schema(description = "Компонент запроса правила")
class RuleQueryDto {
    @Schema(description = "Тип запроса", example = "USER_OF", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Тип запроса не может быть пустым")
    @Pattern(regexp = "USER_OF|ACTIVE_USER_OF|TRANSACTION_SUM_COMPARE|TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW",
            message = "Тип запроса должен быть одним из: USER_OF, ACTIVE_USER_OF, TRANSACTION_SUM_COMPARE, TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW")
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

    @Schema(description = "ID продукта", example = "ab138afb-f3ba-4a93-b74f-0fcee86d447f", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID продукта не может быть пустым")
    private UUID productId;

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
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
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

    @Schema(description = "ID продукта", example = "ab138afb-f3ba-4a93-b74f-0fcee86d447f", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID продукта не может быть пустым")
    private UUID productId;

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
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
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

// Статистика правила
@Schema(description = "Статистика правила")
class RuleStatDto {
    @Schema(description = "ID правила", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long ruleId;

    @Schema(description = "Количество срабатываний", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Min(value = 0, message = "Количество срабатываний не может быть отрицательным")
    private Long count;

    public RuleStatDto() {}

    public RuleStatDto(Long ruleId, Long count) {
        this.ruleId = ruleId;
        this.count = count;
    }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
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

// Рекомендация продукта
@Schema(description = "Рекомендация продукта")
class ProductRecommendationDto {
    @Schema(description = "ID продукта", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private UUID productId;

    @Schema(description = "Название продукта", example = "Премиальная кредитная карта", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String productName;

    @Schema(description = "Текст рекомендации", example = "Рекомендуем на основе вашей истории транзакций", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String recommendationText;

    public ProductRecommendationDto() {}

    public ProductRecommendationDto(UUID productId, String productName, String recommendationText) {
        this.productId = productId;
        this.productName = productName;
        this.recommendationText = recommendationText;
    }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getRecommendationText() { return recommendationText; }
    public void setRecommendationText(String recommendationText) { this.recommendationText = recommendationText; }
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

// ==================== 4. БАЗОВЫЙ КОНТРОЛЛЕР ====================
abstract class BaseController {

    protected <T> ResponseEntity<DataResponseDto<T>> success(T data) {
        return ResponseEntity.ok(new DataResponseDto<>(data));
    }

    protected <T> ResponseEntity<DataResponseDto<T>> success(T data, String message) {
        return ResponseEntity.ok(new DataResponseDto<>(data, message));
    }

    protected <T> ResponseEntity<DataResponseDto<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new DataResponseDto<>(data, AppConstants.SUCCESS_CREATED));
    }

    protected ResponseEntity<ErrorResponseDto> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDto(message, HttpStatus.NOT_FOUND.value()));
    }

    protected ResponseEntity<ErrorResponseDto> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDto(message, HttpStatus.BAD_REQUEST.value()));
    }

    protected ResponseEntity<ErrorResponseDto> validationError(Map<String, String> details) {
        ErrorResponseDto error = new ErrorResponseDto(AppConstants.ERROR_VALIDATION, HttpStatus.BAD_REQUEST.value());
        error.setDetails(details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }
}

// ==================== 5. КОНТРОЛЛЕР ПОЛЬЗОВАТЕЛЕЙ ====================
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/users")
@Tag(name = "Пользователи", description = "API для управления пользователями")
class UserController extends BaseController {

    private final Map<UUID, UserDto> users = new ConcurrentHashMap<>();
    private final AtomicLong userCounter = new AtomicLong(1000);

    // Инициализация тестовых данных
    public UserController() {
        initializeTestUsers();
    }

    private void initializeTestUsers() {
        // Тестовые пользователи
        UUID[] userIds = {
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                UUID.fromString("223e4567-e89b-12d3-a456-426614174001"),
                UUID.fromString("323e4567-e89b-12d3-a456-426614174002")
        };

        String[] firstNames = {"Иван", "Петр", "Мария"};
        String[] lastNames = {"Иванов", "Петров", "Сидорова"};
        String[] emails = {"ivan@example.com", "petr@example.com", "maria@example.com"};

        for (int i = 0; i < userIds.length; i++) {
            UserDto user = new UserDto();
            user.setId(userIds[i]);
            user.setFirstName(firstNames[i]);
            user.setLastName(lastNames[i]);
            user.setEmail(emails[i]);
            user.setPhone("+7999123456" + i);
            user.setRegistrationDate(LocalDateTime.now().minusDays(30 + i));
            user.setActive(true);

            users.put(user.getId(), user);
        }
    }

    @PostMapping
    @Operation(summary = "Создать пользователя", description = "Создает нового пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь создан",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Неверные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> createUser(
            @Parameter(description = "Данные пользователя", required = true)
            @Valid @RequestBody UserRequestDto request) {

        // Проверка уникальности email
        boolean emailExists = users.values().stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(request.getEmail()));

        if (emailExists) {
            return badRequest("Пользователь с таким email уже существует");
        }

        UserDto user = new UserDto();
        user.setId(UUID.randomUUID());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setActive(request.isActive());
        user.setRegistrationDate(LocalDateTime.now());

        users.put(user.getId(), user);

        return created(user);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Получить пользователя по ID", description = "Возвращает пользователя по его идентификатору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь найден",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> getUserById(
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable UUID userId) {

        UserDto user = users.get(userId);
        if (user == null) {
            return notFound(AppConstants.ERROR_NOT_FOUND);
        }

        return success(user);
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Обновить пользователя", description = "Обновляет данные пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь обновлен",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Неверные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> updateUser(
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable UUID userId,
            @Parameter(description = "Обновленные данные пользователя", required = true)
            @Valid @RequestBody UserRequestDto request) {

        UserDto existingUser = users.get(userId);
        if (existingUser == null) {
            return notFound(AppConstants.ERROR_NOT_FOUND);
        }

        // Проверка уникальности email (кроме текущего пользователя)
        boolean emailExists = users.values().stream()
                .filter(u -> !u.getId().equals(userId))
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(request.getEmail()));

        if (emailExists) {
            return badRequest("Пользователь с таким email уже существует");
        }

        existingUser.setFirstName(request.getFirstName());
        existingUser.setLastName(request.getLastName());
        existingUser.setEmail(request.getEmail());
        existingUser.setPhone(request.getPhone());
        existingUser.setActive(request.isActive());

        return success(existingUser, AppConstants.SUCCESS_UPDATED);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Удалить пользователя", description = "Удаляет пользователя по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Пользователь удален"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> deleteUser(
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable UUID userId) {

        if (!users.containsKey(userId)) {
            return notFound(AppConstants.ERROR_NOT_FOUND);
        }

        users.remove(userId);
        return noContent();
    }

    @GetMapping
    @Operation(summary = "Получить всех пользователей", description = "Возвращает список всех пользователей")
    @ApiResponse(responseCode = "200", description = "Успешно",
            content = @Content(schema = @Schema(implementation = UserDto[].class)))
    public ResponseEntity<?> getAllUsers() {
        return success(new ArrayList<>(users.values()));
    }

    @GetMapping("/search")
    @Operation(summary = "Поиск пользователей", description = "Поиск пользователей по имени или email")
    @ApiResponse(responseCode = "200", description = "Успешно",
            content = @Content(schema = @Schema(implementation = UserDto[].class)))
    public ResponseEntity<?> searchUsers(
            @Parameter(description = "Поисковый запрос")
            @RequestParam(required = false) String query) {

        if (query == null || query.trim().isEmpty()) {
            return success(new ArrayList<>(users.values()));
        }

        String searchQuery = query.toLowerCase().trim();
        List<UserDto> result = users.values().stream()
                .filter(user ->
                        user.getFirstName().toLowerCase().contains(searchQuery) ||
                                user.getLastName().toLowerCase().contains(searchQuery) ||
                                user.getEmail().toLowerCase().contains(searchQuery) ||
                                user.getFullName().toLowerCase().contains(searchQuery))
                .collect(Collectors.toList());

        return success(result);
    }
}

// ==================== 6. КОНТРОЛЛЕР ПРОДУКТОВ ====================
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/products")
@Tag(name = "Продукты", description = "API для управления продуктами")
class ProductController extends BaseController {

    private final Map<UUID, ProductDto> products = new ConcurrentHashMap<>();

    // Инициализация тестовых данных
    public ProductController() {
        initializeTestProducts();
    }

    private void initializeTestProducts() {
        // Тестовые продукты
        UUID[] productIds = {
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("44444444-4444-4444-4444-444444444444")
        };

        String[] names = {
                "Дебетовая карта Classic",
                "Кредитная карта Gold",
                "Инвестиционный портфель",
                "Накопительный счет"
        };

        String[] types = {"DEBIT", "CREDIT", "INVEST", "SAVING"};
        String[] descriptions = {
                "Базовая дебетовая карта для повседневных расходов",
                "Премиальная кредитная карта с кэшбэком",
                "Диверсифицированный инвестиционный портфель",
                "Накопительный счет с повышенным процентом"
        };

        BigDecimal[] minBalances = {
                new BigDecimal("0"),
                new BigDecimal("10000"),
                new BigDecimal("50000"),
                new BigDecimal("1000")
        };

        BigDecimal[] interestRates = {
                new BigDecimal("0"),
                new BigDecimal("5.5"),
                new BigDecimal("8.2"),
                new BigDecimal("4.3")
        };

        for (int i = 0; i < productIds.length; i++) {
            ProductDto product = new ProductDto();
            product.setId(productIds[i]);
            product.setName(names[i]);
            product.setType(types[i]);
            product.setDescription(descriptions[i]);
            product.setMinBalance(minBalances[i]);
            product.setInterestRate(interestRates[i]);
            product.setActive(true);
            product.setCreatedDate(LocalDateTime.now().minusDays(60 + i * 10));

            products.put(product.getId(), product);
        }
    }

    @PostMapping
    @Operation(summary = "Создать продукт", description = "Создает новый продукт")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Продукт создан",
                    content = @Content(schema = @Schema(implementation = ProductDto.class))),
            @ApiResponse(responseCode = "400", description = "Неверные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> createProduct(
            @Parameter(description = "Данные продукта", required = true)
            @Valid @RequestBody ProductRequestDto request) {

        ProductDto product = new ProductDto();
        product.setId(UUID.randomUUID());
        product.setName(request.getName());
        product.setType(request.getType());
        product.setDescription(request.getDescription());
        product.setMinBalance(request.getMinBalance());
        product.setInterestRate(request.getInterestRate());
        product.setActive(request.isActive());
        product.setCreatedDate(LocalDateTime.now());

        products.put(product.getId(), product);

        return created(product);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Получить продукт по ID", description = "Возвращает продукт по его идентификатору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Продукт найден",
                    content = @Content(schema = @Schema(implementation = ProductDto.class))),
            @ApiResponse(responseCode = "404", description = "Продукт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> getProductById(
            @Parameter(description = "ID продукта", required = true)
            @PathVariable UUID productId) {

        ProductDto product = products.get(productId);
        if (product == null) {
            return notFound(AppConstants.ERROR_NOT_FOUND);
        }

        return success(product);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Обновить продукт", description = "Обновляет данные продукта")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Продукт обновлен",
                    content = @Content(schema = @Schema(implementation = ProductDto.class))),
            @ApiResponse(responseCode = "400", description = "Неверные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Продукт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> updateProduct(
            @Parameter(description = "ID продукта", required = true)
            @PathVariable UUID productId,
            @Parameter(description = "Обновленные данные продукта", required = true)
            @Valid @RequestBody ProductRequestDto request) {

        ProductDto existingProduct = products.get(productId);
        if (existingProduct == null) {
            return notFound(AppConstants.ERROR_NOT_FOUND);
        }

        existingProduct.setName(request.getName());
        existingProduct.setType(request.getType());
        existingProduct.setDescription(request.getDescription());
        existingProduct.setMinBalance(request.getMinBalance());
        existingProduct.setInterestRate(request.getInterestRate());
        existingProduct.setActive(request.isActive());

        return success(existingProduct, AppConstants.SUCCESS_UPDATED);
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Удалить продукт", description = "Удаляет продукт по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Продукт удален"),
            @ApiResponse(responseCode = "404", description = "Продукт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> deleteProduct(
            @Parameter(description = "ID продукта", required = true)
            @PathVariable UUID productId) {

        if (!products.containsKey(productId)) {
            return notFound(AppConstants.ERROR_NOT_FOUND);
        }

        products.remove(productId);
        return noContent();
    }

    @GetMapping
    @Operation(summary = "Получить все продукты", description = "Возвращает список всех продуктов")
    @ApiResponse(responseCode = "200", description = "Успешно",
            content = @Content(schema = @Schema(implementation = ProductDto[].class)))
    public ResponseEntity<?> getAllProducts(
            @Parameter(description = "Тип продукта для фильтрации")
            @RequestParam(required = false) String type,
            @Parameter(description = "Только активные продукты")
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {

        List<ProductDto> productList = new ArrayList<>(products.values());

        // Применяем фильтры
        if (type != null && !type.isEmpty()) {
            productList = productList.stream()
                    .filter(p -> p.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        if (activeOnly) {
            productList = productList.stream()
                    .filter(ProductDto::isActive)
                    .collect(Collectors.toList());
        }

        return success(productList);
    }

    @GetMapping("/types")
    @Operation(summary = "Получить типы продуктов", description = "Возвращает список доступных типов продуктов")
    @ApiResponse(responseCode = "200", description = "Успешно")
    public ResponseEntity<?> getProductTypes() {
        return success(AppConstants.PRODUCT_TYPES);
    }
}

// ==================== 7. КОНТРОЛЛЕР ТРАНЗАКЦИЙ ====================
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/transactions")
@Tag(name = "Транзакции", description = "API для управления транзакциями")
class TransactionController extends BaseController {

    private final Map<UUID, TransactionDto> transactions = new ConcurrentHashMap<>();

    // Инициализация тестовых данных
    public TransactionController() {
        initializeTestTransactions();
    }

    private void initializeTestTransactions() {
        UUID[] userIds = {
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), // Иван
                UUID.fromString("223e4567-e89b-12d3-a456-426614174001"), // Петр
                UUID.fromString("323e4567-e89b-12d3-a456-426614174002")  // Мария
        };

        UUID[] productIds = {
                UUID.fromString("11111111-1111-1111-1111-111111111111"), // Дебетовая
                UUID.fromString("22222222-2222-2222-2222-222222222222")  // Кредитная
        };

        // Создаем тестовые транзакции
        int transactionCount = 0;
        LocalDateTime baseDate = LocalDateTime.now().minusDays(30);

        for (UUID userId : userIds) {
            for (UUID productId : productIds) {
                for (int i = 0; i < 5; i++) {
                    TransactionDto transaction = new TransactionDto();
                    transaction.setId(UUID.randomUUID());
                    transaction.setUserId(userId);
                    transaction.setProductId(productId);
                    transaction.setType(i % 2 == 0 ? "DEPOSIT" : "WITHDRAW");
                    transaction.setAmount(new BigDecimal(1000 + i * 500));
                    transaction.setDescription(
                            transaction.getType().equals("DEPOSIT") ?
                                    "Зачисление средств" : "Списание средств"
                    );
                    transaction.setTransactionDate(baseDate.plusDays(transactionCount));
                    transaction.setStatus("COMPLETED");

                    transactions.put(transaction.getId(), transaction);
                    transactionCount++;
                }
            }
        }
    }

    @PostMapping
    @Operation(summary = "Создать транзакцию", description = "Создает новую транзакцию")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Транзакция создана",
                    content = @Content(schema = @Schema(implementation = TransactionDto.class))),
            @ApiResponse(responseCode = "400", description = "Неверные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> createTransaction(
            @Parameter(description = "Данные транзакции", required = true)
            @Valid @RequestBody TransactionRequestDto request) {

        TransactionDto transaction = new TransactionDto();
        transaction.setId(UUID.randomUUID());
        transaction.setUserId(request.getUserId());
        transaction.setProductId(request.getProductId());
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus("COMPLETED");

        transactions.put(transaction.getId(), transaction);

        return created(transaction);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Получить транзакцию по ID", description = "Возвращает транзакцию по ее идентификатору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Транзакция найдена",
                    content = @Content(schema = @Schema(implementation = TransactionDto.class))),
            @ApiResponse(responseCode = "404", description = "Транзакция не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> getTransactionById(
            @Parameter(description = "ID транзакции", required = true)
            @PathVariable UUID transactionId) {

        TransactionDto transaction = transactions.get(transactionId);
        if (transaction == null) {
            return notFound(AppConstants.ERROR_NOT_FOUND);
        }

        return success(transaction);
    }

    @GetMapping
    @Operation(summary = "Получить все транзакции", description = "Возвращает список всех транзакций")
    @ApiResponse(responseCode = "200", description = "Успешно",
            content = @Content(schema = @Schema(implementation = TransactionDto[].class)))
    public ResponseEntity<?> getAllTransactions(
            @Parameter(description = "ID пользователя для фильтрации")
            @RequestParam(required = false) UUID userId,
            @Parameter(description = "ID продукта для фильтрации")
            @RequestParam(required = false) UUID productId,
            @Parameter(description = "Тип транзакции для фильтрации")
            @RequestParam(required = false) String type,
            @Parameter(description = "Начальная дата для фильтрации (yyyy-MM-dd)")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "Конечная дата для фильтрации (yyyy-MM-dd)")
            @RequestParam(required = false) String endDate) {

        List<TransactionDto> transactionList = new ArrayList<>(transactions.values());

        // Применяем фильтры
        if (userId != null) {
            transactionList = transactionList.stream()
                    .filter(t -> t.getUserId().equals(userId))
                    .collect(Collectors.toList());
        }

        if (productId != null) {
            transactionList = transactionList.stream()
                    .filter(t -> t.getProductId().equals(productId))
                    .collect(Collectors.toList());
        }

        if (type != null && !type.isEmpty()) {
            transactionList = transactionList.stream()
                    .filter(t -> t.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        // TODO: Добавить фильтрацию по датам

        return success(transactionList);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Получить транзакции пользователя", description = "Возвращает все транзакции конкретного пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно",
                    content = @Content(schema = @Schema(implementation = TransactionDto[].class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> getUserTransactions(
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable UUID userId) {

        List<TransactionDto> userTransactions = transactions.values().stream()
                .filter(t -> t.getUserId().equals(userId))
                .collect(Collectors.toList());

        return success(userTransactions);
    }

    @GetMapping("/summary/{userId}")
    @Operation(summary = "Получить сводку по транзакциям пользователя", description = "Возвращает сводную информацию по транзакциям пользователя")
    @ApiResponse(responseCode = "200", description = "Успешно")
    public ResponseEntity<?> getUserTransactionSummary(
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable UUID userId) {

        List<TransactionDto> userTransactions = transactions.values().stream()
                .filter(t -> t.getUserId().equals(userId))
                .collect(Collectors.toList());

        BigDecimal totalDeposits = userTransactions.stream()
                .filter(t -> t.getType().equals("DEPOSIT"))
                .map(TransactionDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithdrawals = userTransactions.stream()
                .filter(t -> t.getType().equals("WITHDRAW"))
                .map(TransactionDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", userId);
        summary.put("totalTransactions", userTransactions.size());
        summary.put("totalDeposits", totalDeposits);
        summary.put("totalWithdrawals", totalWithdrawals);
        summary.put("balance", totalDeposits.subtract(totalWithdrawals));
        summary.put("transactions", userTransactions);

        return success(summary);
    }
}

// ==================== 8. КОНТРОЛЛЕР ПРАВИЛ ====================
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/rules")
@Tag(name = "Правила", description = "API для управления правилами рекомендаций")
class RuleController extends BaseController {

    private final Map<Long, RuleDto> rules = new ConcurrentHashMap<>();
    private final Map<Long, AtomicLong> ruleStats = new ConcurrentHashMap<>();
    private final AtomicLong ruleIdCounter = new AtomicLong(1);

    // Инициализация тестовых данных
    public RuleController() {
        initializeTestRules();
    }

    private void initializeTestRules() {
        // Пример правила из задания
        RuleDto rule = new RuleDto();
        rule.setId(ruleIdCounter.getAndIncrement());
        rule.setProductName("Простой кредит");
        rule.setProductId(UUID.fromString("ab138afb-f3ba-4a93-b74f-0fcee86d447f"));
        rule.setProductText("Рекомендуем простой кредит на выгодных условиях");
        rule.setActive(true);
        rule.setCreatedDate(LocalDateTime.now().minusDays(10));

        List<RuleQueryDto> queries = new ArrayList<>();
        queries.add(new RuleQueryDto("USER_OF", Arrays.asList("CREDIT"), true));
        queries.add(new RuleQueryDto("TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW", Arrays.asList("DEBIT", ">"), false));
        queries.add(new RuleQueryDto("TRANSACTION_SUM_COMPARE", Arrays.asList("DEBIT", "DEPOSIT", ">", "100000"), false));

        rule.setRule(queries);
        rules.put(rule.getId(), rule);
        ruleStats.put(rule.getId(), new AtomicLong(5)); // 5 срабатываний для теста

        // Еще одно тестовое правило
        RuleDto rule2 = new RuleDto();
        rule2.setId(ruleIdCounter.getAndIncrement());
        rule2.setProductName("Премиальная дебетовая карта");
        rule2.setProductId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
        rule2.setProductText("Рекомендуем премиальную дебетовую карту для активных пользователей");
        rule2.setActive(true);
        rule2.setCreatedDate(LocalDateTime.now().minusDays(5));

        List<RuleQueryDto> queries2 = new ArrayList<>();
        queries2.add(new RuleQueryDto("ACTIVE_USER_OF", Arrays.asList("DEBIT"), false));
        queries2.add(new RuleQueryDto("TRANSACTION_SUM_COMPARE", Arrays.asList("DEBIT", "DEPOSIT", ">", "50000"), false));

        rule2.setRule(queries2);
        rules.put(rule2.getId(), rule2);
        ruleStats.put(rule2.getId(), new AtomicLong(2)); // 2 срабатывания для теста
    }

    @PostMapping
    @Operation(summary = "Создать правило", description = "Создает новое правило рекомендаций")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Правило создано",
                    content = @Content(schema = @Schema(implementation = RuleDto.class))),
            @ApiResponse(responseCode = "400", description = "Неверные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> createRule(
            @Parameter(description = "Данные правила", required = true)
            @Valid @RequestBody RuleRequestDto request) {

        // Валидация запросов правила
        for (RuleQueryDto query : request.getRule()) {
            if (!AppConstants.QUERY_TYPES.contains(query.getQuery())) {
                return badRequest("Неверный тип запроса: " + query.getQuery());
            }

            // Валидация аргументов в зависимости от типа запроса
            List<String> args = query.getArguments();
            switch (query.getQuery()) {
                case "USER_OF":
                case "ACTIVE_USER_OF":
                    if (args.size() != 1 || !AppConstants.PRODUCT_TYPES.contains(args.get(0))) {
                        return badRequest("Для запроса " + query.getQuery() + " требуется 1 аргумент типа продукта (DEBIT, CREDIT, INVEST, SAVING)");
                    }
                    break;
                case "TRANSACTION_SUM_COMPARE":
                    if (args.size() != 4 ||
                            !AppConstants.PRODUCT_TYPES.contains(args.get(0)) ||
                            !AppConstants.TRANSACTION_TYPES.contains(args.get(1)) ||
                            !AppConstants.COMPARISON_OPERATORS.contains(args.get(2))) {
                        return badRequest("Для запроса TRANSACTION_SUM_COMPARE требуется 4 аргумента: тип продукта, тип транзакции, оператор сравнения, константа");
                    }
                    try {
                        new BigDecimal(args.get(3));
                    } catch (NumberFormatException e) {
                        return badRequest("Четвертый аргумент должен быть числом");
                    }
                    break;
                case "TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW":
                    if (args.size() != 2 ||
                            !AppConstants.PRODUCT_TYPES.contains(args.get(0)) ||
                            !AppConstants.COMPARISON_OPERATORS.contains(args.get(1))) {
                        return badRequest("Для запроса TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW требуется 2 аргумента: тип продукта, оператор сравнения");
                    }
                    break;
            }
        }

        RuleDto rule = new RuleDto();
        rule.setId(ruleIdCounter.getAndIncrement());
        rule.setProductName(request.getProductName());
        rule.setProductId(request.getProductId());
        rule.setProductText(request.getProductText());
        rule.setRule(request.getRule());
        rule.setActive(true);
        rule.setCreatedDate(LocalDateTime.now());

        rules.put(rule.getId(), rule);
        ruleStats.put(rule.getId(), new AtomicLong(0));

        return created(rule);
    }

    @GetMapping("/{ruleId}")
    @Operation(summary = "Получить правило по ID", description = "Возвращает правило по его идентификатору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Правило найдено",
                    content = @Content(schema = @Schema(implementation = RuleDto.class))),
            @ApiResponse(responseCode = "404", description = "Правило не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> getRuleById(
            @Parameter(description = "ID правила", required = true)
            @PathVariable Long ruleId) {

        RuleDto rule = rules.get(ruleId);
        if (rule == null) {
            return notFound(AppConstants.ERROR_NOT_FOUND);
        }

        return success(rule);
    }

    @DeleteMapping("/{ruleId}")
    @Operation(summary = "Удалить правило", description = "Удаляет правило по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Правило удалено"),
            @ApiResponse(responseCode = "404", description = "Правило не найдено",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> deleteRule(
            @Parameter(description = "ID правила", required = true)
            @PathVariable Long ruleId) {

        if (!rules.containsKey(ruleId)) {
            return notFound(AppConstants.ERROR_NOT_FOUND);
        }

        rules.remove(ruleId);
        ruleStats.remove(ruleId);

        return noContent();
    }

    @GetMapping
    @Operation(summary = "Получить все правила", description = "Возвращает список всех правил")
    @ApiResponse(responseCode = "200", description = "Успешно",
            content = @Content(schema = @Schema(implementation = RulesListResponseDto.class)))
    public ResponseEntity<?> getAllRules() {
        RulesListResponseDto response = new RulesListResponseDto(new ArrayList<>(rules.values()));
        return success(response);
    }

    @PostMapping("/{ruleId}/increment")
    @Operation(summary = "Увеличить счетчик правила", description = "Увеличивает счетчик срабатываний правила на 1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Счетчик увеличен"),
            @ApiResponse(responseCode = "404", description = "Правило не найдено",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> incrementRuleCounter(
            @Parameter(description = "ID правила", required = true)
            @PathVariable Long ruleId) {

        if (!rules.containsKey(ruleId)) {
            return notFound(AppConstants.ERROR_NOT_FOUND);
        }

        AtomicLong counter = ruleStats.computeIfAbsent(ruleId, k -> new AtomicLong(0));
        counter.incrementAndGet();

        Map<String, Object> response = new HashMap<>();
        response.put("ruleId", ruleId);
        response.put("newCount", counter.get());
        response.put("message", "Счетчик увеличен");

        return success(response);
    }

    @GetMapping("/stats")
    @Operation(summary = "Получить статистику правил", description = "Возвращает статистику срабатываний всех правил")
    @ApiResponse(responseCode = "200", description = "Успешно",
            content = @Content(schema = @Schema(implementation = RuleStatsResponseDto.class)))
    public ResponseEntity<?> getRuleStats() {
        List<RuleStatDto> stats = new ArrayList<>();

        // Для всех правил, включая те, у которых счетчик = 0
        for (RuleDto rule : rules.values()) {
            Long ruleId = rule.getId();
            Long count = ruleStats.getOrDefault(ruleId, new AtomicLong(0)).get();
            stats.add(new RuleStatDto(ruleId, count));
        }

        RuleStatsResponseDto response = new RuleStatsResponseDto(stats);
        return success(response);
    }

    @GetMapping("/query-types")
    @Operation(summary = "Получить типы запросов", description = "Возвращает список доступных типов запросов для правил")
    @ApiResponse(responseCode = "200", description = "Успешно")
    public ResponseEntity<?> getQueryTypes() {
        return success(AppConstants.QUERY_TYPES);
    }

    @GetMapping("/operators")
    @Operation(summary = "Получить операторы сравнения", description = "Возвращает список доступных операторов сравнения")
    @ApiResponse(responseCode = "200", description = "Успешно")
    public ResponseEntity<?> getComparisonOperators() {
        return success(AppConstants.COMPARISON_OPERATORS);
    }
}

// ==================== 9. КОНТРОЛЛЕР РЕКОМЕНДАЦИЙ ====================
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/recommendations")
@Tag(name = "Рекомендации", description = "API для получения рекомендаций")
class RecommendationController extends BaseController {

    private final UserController userController;
    private final RuleController ruleController;
    private final TransactionController transactionController;
    private final ProductController productController;

    // Простой кеш рекомендаций
    private final Map<UUID, List<ProductRecommendationDto>> recommendationCache = new ConcurrentHashMap<>();

    public RecommendationController() {
        this.userController = new UserController();
        this.ruleController = new RuleController();
        this.transactionController = new TransactionController();
        this.productController = new ProductController();
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Получить рекомендации для пользователя", description = "Возвращает рекомендации продуктов для указанного пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Рекомендации получены",
                    content = @Content(schema = @Schema(implementation = RecommendationsResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> getRecommendationsForUser(
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable UUID userId) {

        // Проверяем кеш
        if (recommendationCache.containsKey(userId)) {
            List<ProductRecommendationDto> cached = recommendationCache.get(userId);
            return success(new RecommendationsResponseDto(cached), "Рекомендации из кеша");
        }

        // Получаем все правила
        Map<Long, RuleDto> rules = new ConcurrentHashMap<>(); // Здесь должен быть доступ к правилам

        // Получаем все продукты
        Map<UUID, ProductDto> products = new ConcurrentHashMap<>(); // Здесь должен быть доступ к продуктам

        List<ProductRecommendationDto> recommendations = new ArrayList<>();

        // Пример простых рекомендаций на основе тестовых данных
        recommendations.add(new ProductRecommendationDto(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Дебетовая карта Classic",
                "Рекомендуем для ежедневных расходов"
        ));

        recommendations.add(new ProductRecommendationDto(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Кредитная карта Gold",
                "Идеально подходит для крупных покупок"
        ));

        // Сохраняем в кеш
        recommendationCache.put(userId, recommendations);

        return success(new RecommendationsResponseDto(recommendations));
    }

    @PostMapping("/clear-cache")
    @Operation(summary = "Очистить кеш рекомендаций", description = "Очищает кеш рекомендаций для всех пользователей")
    @ApiResponse(responseCode = "200", description = "Кеш очищен")
    public ResponseEntity<?> clearRecommendationCache() {
        int cacheSize = recommendationCache.size();
        recommendationCache.clear();

        Map<String, Object> response = new HashMap<>();
        response.put("clearedEntries", cacheSize);
        response.put("message", "Кеш рекомендаций очищен");

        return success(response);
    }

    @PostMapping("/clear-cache/{userId}")
    @Operation(summary = "Очистить кеш рекомендаций для пользователя", description = "Очищает кеш рекомендаций для конкретного пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Кеш очищен"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден в кеше")
    })
    public ResponseEntity<?> clearUserRecommendationCache(
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable UUID userId) {

        if (!recommendationCache.containsKey(userId)) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Пользователь не найден в кеше");
            return success(response);
        }

        recommendationCache.remove(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("message", "Кеш рекомендаций для пользователя очищен");

        return success(response);
    }
}

// ==================== 10. КОНТРОЛЛЕР УПРАВЛЕНИЯ ====================
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/management")
@Tag(name = "Управление", description = "API для управления сервисом")
class ManagementController extends BaseController {

    @PostMapping("/clear-caches")
    @Operation(summary = "Очистить все кеши", description = "Очищает все кеши в системе")
    @ApiResponse(responseCode = "200", description = "Кеши очищены")
    public ResponseEntity<?> clearAllCaches() {
        // В реальной системе здесь будет очистка всех кешей
        Map<String, Object> response = new HashMap<>();
        response.put("action", "clearAllCaches");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "Все кеши очищены успешно");

        return success(response);
    }

    @GetMapping("/info")
    @Operation(summary = "Получить информацию о сервисе", description = "Возвращает информацию о сервисе")
    @ApiResponse(responseCode = "200", description = "Успешно",
            content = @Content(schema = @Schema(implementation = ServiceInfoDto.class)))
    public ResponseEntity<?> getServiceInfo() {
        ServiceInfoDto info = new ServiceInfoDto("recommendation-service", "1.0.0");
        info.setStatus("running");

        return success(info);
    }

    @GetMapping("/health")
    @Operation(summary = "Проверить здоровье сервиса", description = "Возвращает статус здоровья сервиса")
    @ApiResponse(responseCode = "200", description = "Сервис здоров")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "recommendation-service");
        health.put("version", "1.0.0");

        return success(health);
    }

    @GetMapping("/metrics")
    @Operation(summary = "Получить метрики сервиса", description = "Возвращает основные метрики сервиса")
    @ApiResponse(responseCode = "200", description = "Метрики получены")
    public ResponseEntity<?> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("timestamp", LocalDateTime.now());
        metrics.put("uptime", "PT1H30M"); // Примерное время работы
        metrics.put("memoryUsed", "512MB");
        metrics.put("memoryMax", "2GB");
        metrics.put("activeRequests", 5);
        metrics.put("totalRequests", 1000);

        return success(metrics);
    }
}

// ==================== 11. ГЛОБАЛЬНЫЙ ОБРАБОТЧИК ОШИБОК ====================
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleException(Exception ex) {
        ErrorResponseDto error = new ErrorResponseDto(
                AppConstants.ERROR_INTERNAL,
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(javax.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(javax.validation.ConstraintViolationException ex) {
        Map<String, String> details = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String field = violation.getPropertyPath().toString();
            details.put(field, violation.getMessage());
        });

        ErrorResponseDto error = new ErrorResponseDto(
                AppConstants.ERROR_VALIDATION,
                HttpStatus.BAD_REQUEST.value()
        );
        error.setDetails(details);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValid(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            details.put(error.getField(), error.getDefaultMessage());
        });

        ErrorResponseDto error = new ErrorResponseDto(
                AppConstants.ERROR_VALIDATION,
                HttpStatus.BAD_REQUEST.value()
        );
        error.setDetails(details);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}

// ==================== 12. НАСТРОЙКА OPENAPI ====================
@io.swagger.v3.oas.annotations.OpenAPIDefinition(
        info = @io.swagger.v3.oas.annotations.info.Info(
                title = "Recommendation Service API",
                version = "1.0.0",
                description = "API для сервиса рекомендаций финансовых продуктов",
                contact = @io.swagger.v3.oas.annotations.info.Contact(
                        name = "Support",
                        email = "support@example.com"
                )
        ),
        servers = {
                @io.swagger.v3.oas.annotations.servers.Server(
                        url = "http://localhost:8080",
                        description = "Local server"
                )
        }
)
class OpenApiConfig {
    // Конфигурация OpenAPI
}

