package ru.hogwarts.school;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rule") class DynamicRuleController {
    private final DynamicRuleService dynamicRuleService; public DynamicRuleController(DynamicRuleService dynamicRuleService) { this.dynamicRuleService = dynamicRuleService; }

    @PostMapping
    public ResponseEntity<DynamicRuleResponse> createRule(@Valid @RequestBody DynamicRuleRequest request) {
        DynamicRuleResponse response = dynamicRuleService.createRule(request); return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<RulesListResponse> getAllRules() {
        List<DynamicRuleResponse> rules = dynamicRuleService.getAllRules(); return ResponseEntity.ok(new RulesListResponse(rules));
    }

    @DeleteMapping("/{id}") public ResponseEntity<Void> deleteRule(@PathVariable Long id) { dynamicRuleService.deleteRule(id); return ResponseEntity.noContent().build(); }
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) { return ResponseEntity.badRequest().body(e.getMessage()); }
}