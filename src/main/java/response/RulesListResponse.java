package response;

import java.util.List;

class RulesListResponse { private List<DynamicRuleResponse> data;
    public RulesListResponse() {} public RulesListResponse(List<DynamicRuleResponse> data) { this.data = data; }
    public List<DynamicRuleResponse> getData() { return data; } public void setData(List<DynamicRuleResponse> data) { this.data = data; }
}
