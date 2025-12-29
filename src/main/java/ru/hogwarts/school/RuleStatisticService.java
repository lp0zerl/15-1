package ru.hogwarts.school;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
class RuleStatisticService {
    private final RuleStatisticRepository statisticRepository; private final DynamicRuleRepository ruleRepository;
    public RuleStatisticService(RuleStatisticRepository statisticRepository, DynamicRuleRepository ruleRepository) {
        this.statisticRepository = statisticRepository; this.ruleRepository = ruleRepository;
    }

    @Transactional(transactionManager = "rulesTransactionManager") public void createStatisticForRule(RuleEntity rule) {
        RuleStatisticEntity statistic = new RuleStatisticEntity(rule); statisticRepository.save(statistic);
    }

    @Transactional(transactionManager = "rulesTransactionManager") public void incrementRuleStatistic(Long ruleId) {
        statisticRepository.incrementCount(ruleId);
    }

    @Transactional(readOnly = true, transactionManager = "rulesTransactionManager")
    public RuleStatsResponse getAllStatistics() {
        List<RuleEntity> allRules = ruleRepository.findAll();
        List<RuleStatsDto> stats = allRules.stream().map(rule -> {
            Long count = statisticRepository.findByRuleId(rule.getId()).map(RuleStatisticEntity::getCount).orElse(0L);
            return new RuleStatsDto(rule.getId(), count);
        }).collect(Collectors.toList());
        return new RuleStatsResponse(stats);
    }

    @Transactional(transactionManager = "rulesTransactionManager") public void deleteStatisticsForRule(Long ruleId) {
        statisticRepository.deleteByRuleId(ruleId);
    }
}