package ru.hogwarts.school;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
class DynamicRuleService {
    private final DynamicRuleRepository ruleRepository; private final com.example.recommendation.RuleStatisticService statisticService;
    public DynamicRuleService(DynamicRuleRepository ruleRepository, com.example.recommendation.RuleStatisticService statisticService) {
        this.ruleRepository = ruleRepository; this.statisticService = statisticService;
    }

    @Transactional(transactionManager = "rulesTransactionManager")
    public DynamicRuleResponse createRule(DynamicRuleRequest request) {
        RuleEntity entity = new RuleEntity(); entity.setProductName(request.getProduct_name());
        entity.setProductId(request.getProduct_id().toString()); entity.setProductText(request.getProduct_text());
        List<RuleQueryEntity> queries = request.getRule().stream().map(dto -> {
            RuleQueryEntity queryEntity = new RuleQueryEntity(); queryEntity.setQueryType(QueryType.valueOf(dto.getQuery()));
            queryEntity.setArgumentsFromList(dto.getArguments()); queryEntity.setNegate(dto.isNegate()); queryEntity.setRule(entity);
            return queryEntity; }).collect(Collectors.toList());
        entity.setQueries(queries); RuleEntity saved = ruleRepository.save(entity);
        statisticService.createStatisticForRule(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true, transactionManager = "rulesTransactionManager")
    public List<DynamicRuleResponse> getAllRules() {
        return ruleRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(transactionManager = "rulesTransactionManager") public void deleteRule(Long id) { ruleRepository.deleteById(id); }
    @Transactional(readOnly = true, transactionManager = "rulesTransactionManager") public RuleEntity findRuleById(Long id) { return ruleRepository.findById(id).orElse(null); }

    private DynamicRuleResponse toResponse(RuleEntity entity) {
        List<RuleQueryDto> queries = entity.getQueries().stream()
                .map(query -> new RuleQueryDto(query.getQueryType().name(), query.getArgumentsAsList(), query.isNegate()))
                .collect(Collectors.toList());
        return new DynamicRuleResponse(entity.getId(), entity.getProductName(), UUID.fromString(entity.getProductId()), entity.getProductText(), queries);
    }
}