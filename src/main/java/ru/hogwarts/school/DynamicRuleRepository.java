package ru.hogwarts.school;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface DynamicRuleRepository extends JpaRepository<DynamicRule, Long> {
    Optional<DynamicRule> findByProductId(String productId);
    void deleteByProductId(String productId);
}

@Repository
interface RuleStatisticRepository extends JpaRepository<RuleStatistic, Long> {
    Optional<RuleStatistic> findByRuleId(Long ruleId);
    List<RuleStatistic> findAll();
}