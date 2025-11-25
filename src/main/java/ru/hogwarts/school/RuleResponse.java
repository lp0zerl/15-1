package ru.hogwarts.school;

import java.util.List;

class RuleResponse {
    private Long id;
    private String productName;
    private String productId;
    private String productText;
    private List<RuleQuery> rule;

    public RuleResponse() {}
    public RuleResponse(Long id, String productName, String productId, String productText, List<RuleQuery> rule) {
        this.id = id;
        this.productName = productName;
        this.productId = productId;
        this.productText = productText;
        this.rule = rule;
    }
