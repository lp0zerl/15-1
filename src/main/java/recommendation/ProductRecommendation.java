package recommendation;

import java.util.UUID;

class ProductRecommendation {
    private UUID productId; private String productName; private String recommendationText;
    public ProductRecommendation() {}
    public ProductRecommendation(UUID productId, String productName, String recommendationText) {
        this.productId = productId; this.productName = productName; this.recommendationText = recommendationText;
    }
    public UUID getProductId() { return productId; } public void setProductId(UUID productId) { this.productId = productId; }
    public String getProductName() { return productName; } public void setProductName(String productName) { this.productName = productName; }
    public String getRecommendationText() { return recommendationText; } public void setRecommendationText(String recommendationText) { this.recommendationText = recommendationText; }
    @Override public String toString() { return String.format("• %s: %s", productName, recommendationText); }
}