package dto;


class RuleStatsDto { private Long rule_id; private Long count;
    public RuleStatsDto() {} public RuleStatsDto(Long rule_id, Long count) { this.rule_id = rule_id; this.count = count; }
    public Long getRule_id() { return rule_id; } public void setRule_id(Long rule_id) { this.rule_id = rule_id; }
    public Long getCount() { return count; } public void setCount(Long count) { this.count = count; }
}
