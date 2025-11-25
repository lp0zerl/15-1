package ru.hogwarts.school;

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
