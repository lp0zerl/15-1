package response;

import ru.hogwarts.school.RuleQueryDto;

import java.util.List;
import java.util.UUID;

class DynamicRuleResponse {
    private Long id; private String product_name; private UUID product_id; private String product_text; private List<RuleQueryDto> rule;
    public DynamicRuleResponse() {}
    public DynamicRuleResponse(Long id, String product_name, UUID product_id, String product_text, List<RuleQueryDto> rule) {
        this.id = id; this.product_name = product_name; this.product_id = product_id; this.product_text = product_text; this.rule = rule;
    }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getProduct_name() { return product_name; } public void setProduct_name(String product_name) { this.product_name = product_name; }
    public UUID getProduct_id() { return product_id; } public void setProduct_id(UUID product_id) { this.product_id = product_id; }
    public String getProduct_text() { return product_text; } public void setProduct_text(String product_text) { this.product_text = product_text; }
    public List<RuleQueryDto> getRule() { return rule; } public void setRule(List<RuleQueryDto> rule) { this.rule = rule; }
}