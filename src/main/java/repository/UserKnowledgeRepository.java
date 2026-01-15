package repository;


import org.springframework.stereotype.Repository;
import ru.hogwarts.school.UserTransaction;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
interface UserKnowledgeRepository extends JpaRepository<UserTransaction, UUID> {
    @Query("SELECT COUNT(DISTINCT t.id) FROM UserTransaction t WHERE t.userId = :userId AND t.productType = :productType")
    Long countTransactionsByUserAndProductType(@Param("userId") UUID userId, @Param("productType") String productType);
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM UserTransaction t WHERE t.userId = :userId AND t.productType = :productType AND t.transactionType = :transactionType")
    BigDecimal sumTransactionsByType(@Param("userId") UUID userId, @Param("productType") String productType, @Param("transactionType") String transactionType);
    @Query("SELECT COUNT(DISTINCT t.id) > 0 FROM UserTransaction t WHERE t.userId = :userId AND t.productType = :productType")
    boolean existsByUserAndProductType(@Param("userId") UUID userId, @Param("productType") String productType);
}
