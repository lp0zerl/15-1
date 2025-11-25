package ru.hogwarts.school;


class AppProperties {
    public static final String[] PROPERTIES = {
            "spring.application.name=bank-recommendation-system",
            "server.port=8080",
            "spring.datasource.url=jdbc:h2:mem:bankdb",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.dynamic-rules.url=jdbc:h2:mem:rulesdb",
            "spring.datasource.dynamic-rules.driver-class-name=org.h2.Driver",
            "spring.datasource.dynamic-rules.username=sa",
            "spring.datasource.dynamic-rules.password=",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.show-sql=true",
            "spring.h2.console.enabled=true",
            "spring.h2.console.path=/h2-console",
            "telegram.bot.token=test_token",
            "telegram.bot.username=test_bot",
            "logging.level.ru.example.bank=DEBUG"
    };
}