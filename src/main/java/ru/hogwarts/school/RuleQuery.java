package ru.hogwarts.school;

import java.util.List;

class RuleQuery {
    private String query;
    private List<String> arguments;
    private boolean negate;

    public RuleQuery() {}
    public RuleQuery(String query, List<String> arguments, boolean negate) {
        this.query = query;
        this.arguments = arguments;
        this.negate = negate;
    }
