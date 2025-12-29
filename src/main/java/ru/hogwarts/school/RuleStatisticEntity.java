package ru.hogwarts.school;

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