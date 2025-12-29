package ru.hogwarts.school;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
class DatabaseConfig {

    @Primary
    @Bean(name = "knowledgeDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.knowledge")
    public DataSource knowledgeDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "rulesDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.rules")
    public DataSource rulesDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "knowledgeEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean knowledgeEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(knowledgeDataSource());
        em.setPackagesToScan("com.example.recommendation");
        em.setPersistenceUnitName("knowledge");

        Properties props = new Properties();
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        em.setJpaProperties(props);

        return em;
    }

    @Bean(name = "rulesEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean rulesEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(rulesDataSource());
        em.setPackagesToScan("com.example.recommendation");
        em.setPersistenceUnitName("rules");

        Properties props = new Properties();
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        em.setJpaProperties(props);

        return em;
    }

    @Primary
    @Bean(name = "knowledgeTransactionManager")
    public PlatformTransactionManager knowledgeTransactionManager() {
        return new JpaTransactionManager(knowledgeEntityManagerFactory().getObject());
    }

    @Bean(name = "rulesTransactionManager")
    public PlatformTransactionManager rulesTransactionManager() {
        return new JpaTransactionManager(rulesEntityManagerFactory().getObject());
    }
}