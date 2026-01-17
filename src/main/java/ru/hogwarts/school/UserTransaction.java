package ru.hogwarts.school;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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