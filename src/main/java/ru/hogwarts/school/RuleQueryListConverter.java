package ru.hogwarts.school;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.List;

@Converter
class RuleQueryListConverter implements AttributeConverter<List<RuleQuery>, String> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<RuleQuery> ruleQueries) {
        try {
            return objectMapper.writeValueAsString(ruleQueries);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting rule queries to JSON", e);
        }
    }

    @Override
    public List<RuleQuery> convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, RuleQuery.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JSON to rule queries", e);
        }
    }
}