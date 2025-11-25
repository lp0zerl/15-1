package ru.hogwarts.school;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rule")
class RuleController {
    private final DynamicRuleRepository dynamicRuleRepository;
    private final RuleStatisticRepository statisticRepository;

    public RuleController(DynamicRuleRepository dynamicRuleRepository, RuleStatisticRepository statisticRepository) {
        this.dynamicRuleRepository = dynamicRuleRepository;
        this.statisticRepository = statisticRepository;
    }

    @PostMapping
    public ResponseEntity<RuleResponse> createRule(@RequestBody CreateRuleRequest request) {
        DynamicRule dynamicRule = new DynamicRule(
                request.getProductName(),
                request.getProductId(),
                request.getProductText(),
                request.getRule()
        );

        DynamicRule savedRule = dynamicRuleRepository.save(dynamicRule);

        RuleStatistic statistic = new RuleStatistic(savedRule.getId());
        statisticRepository.save(statistic);

        RuleResponse response = new RuleResponse(
                savedRule.getId(),
                savedRule.getProductName(),
                savedRule.getProductId(),
                savedRule.getProductText(),
                savedRule.getRule()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<RulesListResponse> getAllRules() {
        List<DynamicRule> rules = dynamicRuleRepository.findAll();

        List<RuleResponse> ruleResponses = rules.stream()
                .map(rule -> new RuleResponse(
                        rule.getId(),
                        rule.getProductName(),
                        rule.getProductId(),
                        rule.getProductText(),
                        rule.getRule()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new RulesListResponse(ruleResponses));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteRule(@PathVariable String productId) {
        Optional<DynamicRule> rule = dynamicRuleRepository.findByProductId(productId);
        if (rule.isPresent()) {
            statisticRepository.findByRuleId(rule.get().getId())
                    .ifPresent(statisticRepository::delete);
            dynamicRuleRepository.deleteByProductId(productId);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        List<RuleStatistic> statistics = statisticRepository.findAll();

        List<RuleStat> stats = statistics.stream()
                .map(stat -> new RuleStat(stat.getRuleId().toString(), stat.getExecutionCount()))
                .collect(Collectors.toList());

        List<DynamicRule> allRules = dynamicRuleRepository.findAll();
        for (DynamicRule rule : allRules) {
            boolean hasStat = stats.stream()
                    .anyMatch(stat -> stat.getRuleId().equals(rule.getId().toString()));
            if (!hasStat) {
                stats.add(new RuleStat(rule.getId().toString(), 0L));
            }
        }

        return ResponseEntity.ok(new StatsResponse(stats));
    }
}