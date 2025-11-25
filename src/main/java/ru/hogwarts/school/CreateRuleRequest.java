package ru.hogwarts.school;

import java.util.List;

class CreateRuleRequest {
    private String productName;
    private String productId;
    private String productText;
    private List<ru.example.bank.RuleQuery> rule;

    public CreateRuleRequest() {}

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductText() { return productText; }
    public void setProductText(String productText) { this.productText = productText; }
    public List<ru.example.bank.RuleQuery> getRule() { return rule; }
    public void setRule(List<ru.example.bank.RuleQuery> rule) { this.rule = rule; }
}
