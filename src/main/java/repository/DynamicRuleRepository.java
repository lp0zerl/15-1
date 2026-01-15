package repository;

import org.springframework.stereotype.Repository;
import ru.hogwarts.school.RuleEntity;

@Repository
interface DynamicRuleRepository extends JpaRepository<RuleEntity, Long> {}
