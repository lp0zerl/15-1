package controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hogwarts.school.RuleStatisticService;
import ru.hogwarts.school.RuleStatsResponse;

@RestController
@RequestMapping("/rule") class RuleStatsController {
    private final RuleStatisticService ruleStatisticService; public RuleStatsController(RuleStatisticService ruleStatisticService) { this.ruleStatisticService = ruleStatisticService; }
    @GetMapping("/stats") public ResponseEntity<RuleStatsResponse> getRuleStatistics() { RuleStatsResponse stats = ruleStatisticService.getAllStatistics(); return ResponseEntity.ok(stats); }
}