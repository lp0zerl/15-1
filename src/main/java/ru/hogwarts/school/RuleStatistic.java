package ru.hogwarts.school;

import javax.persistence.*;
import java.util.Date;

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