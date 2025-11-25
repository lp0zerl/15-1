package ru.hogwarts.school;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

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