package entity;


import ru.hogwarts.school.QueryType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "rule_queries")
class RuleQueryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "query_type", nullable = false) @Enumerated(EnumType.STRING)
    private QueryType queryType;
    @Column(name = "arguments", nullable = false, columnDefinition = "TEXT")
    private String arguments;
    @Column(name = "negate", nullable = false)
    private boolean negate;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rule_id", nullable = false)
    private RuleEntity rule;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public QueryType getQueryType() { return queryType; }
    public void setQueryType(QueryType queryType) { this.queryType = queryType; }
    public String getArguments() { return arguments; }
    public void setArguments(String arguments) { this.arguments = arguments; }
    public boolean isNegate() { return negate; }
    public void setNegate(boolean negate) { this.negate = negate; }
    public RuleEntity getRule() { return rule; }
    public void setRule(RuleEntity rule) { this.rule = rule; }
    public List<String> getArgumentsAsList() {
        return arguments == null || arguments.isEmpty() ? Collections.emptyList() : Arrays.asList(arguments.split(","));
    }
    public void setArgumentsFromList(List<String> args) {
        this.arguments = args == null || args.isEmpty() ? "" : String.join(",", args);
    }
}