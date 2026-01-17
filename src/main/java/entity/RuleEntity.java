package entity;


import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dynamic_rules")
class RuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "product_name", nullable = false)
    private String productName;
    @Column(name = "product_id", nullable = false)
    private String productId;
    @Column(name = "product_text", nullable = false, columnDefinition = "TEXT")
    private String productText;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "rule")
    private List<com.example.recommendation.RuleQueryEntity> queries = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "rule")
    private List<com.example.recommendation.RuleStatisticEntity> statistics = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductText() { return productText; }
    public void setProductText(String productText) { this.productText = productText; }
    public List<com.example.recommendation.RuleQueryEntity> getQueries() { return queries; }
    public void setQueries(List<com.example.recommendation.RuleQueryEntity> queries) {
        this.queries = queries;
        if (queries != null) queries.forEach(q -> q.setRule(this));
    }
    public List<com.example.recommendation.RuleStatisticEntity> getStatistics() { return statistics; }
    public void setStatistics(List<com.example.recommendation.RuleStatisticEntity> statistics) {
        this.statistics = statistics;
        if (statistics != null) statistics.forEach(s -> s.setRule(this));
    }
}