package ru.hogwarts.school;

import org.springframework.stereotype.Repository;

@Repository
interface DynamicRuleRepository extends JpaRepository<RuleEntity, Long> {}
