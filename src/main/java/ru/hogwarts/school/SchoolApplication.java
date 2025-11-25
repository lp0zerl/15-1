
package ru.example.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getProductName() { return productName; }
	public void setProductName(String productName) { this.productName = productName; }
	public String getProductId() { return productId; }
	public void setProductId(String productId) { this.productId = productId; }
	public String getProductText() { return productText; }
	public void setProductText(String productText) { this.productText = productText; }
	public List<RuleQuery> getRule() { return rule; }
	public void setRule(List<RuleQuery> rule) { this.rule = rule; }
}

	public String getQuery() { return query; }
	public void setQuery(String query) { this.query = query; }
	public List<String> getArguments() { return arguments; }
	public void setArguments(List<String> arguments) { this.arguments = arguments; }
	public boolean isNegate() { return negate; }
	public void setNegate(boolean negate) { this.negate = negate; }
}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getProductName() { return productName; }
	public void setProductName(String productName) { this.productName = productName; }
	public String getProductId() { return productId; }
	public void setProductId(String productId) { this.productId = productId; }
	public String getProductText() { return productText; }
	public void setProductText(String productText) { this.productText = productText; }
	public List<RuleQuery> getRule() { return rule; }
	public void setRule(List<RuleQuery> rule) { this.rule = rule; }
	public Date getCreatedAt() { return createdAt; }
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
	public Date getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Long getRuleId() { return ruleId; }
	public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
	public Long getExecutionCount() { return executionCount; }
	public void setExecutionCount(Long executionCount) { this.executionCount = executionCount; }
	public Date getCreatedAt() { return createdAt; }
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
	public Date getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

@SpringBootApplication
@EnableCaching
public class BankApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankApplication.class, args);
	}

	@Bean
	public BuildProperties buildProperties() {
		return new BuildProperties(new Properties() {{
			setProperty("name", "bank-recommendation-system");
			setProperty("version", "1.0.0");
		}});
	}
}
