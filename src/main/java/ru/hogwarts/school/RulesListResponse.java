package ru.hogwarts.school;

import java.util.List;

class RulesListResponse {
    private List<RuleResponse> data;

    public RulesListResponse() {}
    public RulesListResponse(List<RuleResponse> data) {
        this.data = data;
    }

    public List<RuleResponse> getData() { return data; }
    public void setData(List<RuleResponse> data) { this.data = data; }
}
