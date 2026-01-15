package repository;


import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.hogwarts.school.RuleStatisticEntity;

import java.util.Optional;

@Repository
interface RuleStatisticRepository extends JpaRepository<RuleStatisticEntity, Long> {
    Optional<RuleStatisticEntity> findByRuleId(Long ruleId);
    @Modifying @Transactional
    @Query("UPDATE RuleStatisticEntity r SET r.count = r.count + 1 WHERE r.rule.id = :ruleId")
    void incrementCount(@Param("ruleId") Long ruleId);
    @Modifying @Transactional @Query("DELETE FROM RuleStatisticEntity r WHERE r.rule.id = :ruleId")
    void deleteByRuleId(@Param("ruleId") Long ruleId);
}

