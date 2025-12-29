package ru.hogwarts.school;

import java.util.List;
import java.util.UUID;

class DynamicRuleRequest {
    private String product_name; private UUID product_id; private String product_text; private List<com.example.recommendation.RuleQueryDto> rule;
    public String getProduct_name() { return product_name; } public void setProduct_name(String product_name) { this.product_name = product_name; }
    public UUID getProduct_id() { return product_id; } public void setProduct_id(UUID product_id) { this.product_id = product_id; }
    public String getProduct_text() { return product_text; } public void setProduct_text(String product_text) { this.product_text = product_text; }
    public List<com.example.recommendation.RuleQueryDto> getRule() { return rule; } public void setRule(List<com.example.recommendation.RuleQueryDto> rule) { this.rule = rule; }
}
