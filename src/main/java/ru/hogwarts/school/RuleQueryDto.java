package ru.hogwarts.school;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

class RuleQueryDto {
    private final String query; private final List<String> arguments; private final boolean negate;
    @JsonCreator
    public RuleQueryDto(@JsonProperty("query") String query, @JsonProperty("arguments") List<String> arguments, @JsonProperty("negate") boolean negate) {
        this.query = query; this.arguments = arguments; this.negate = negate;
    }
    public String getQuery() { return query; } public List<String> getArguments() { return arguments; } public boolean isNegate() { return negate; }
}
