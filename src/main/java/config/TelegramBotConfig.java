package config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hogwarts.school.TelegramBot;

@Configuration
class TelegramBotConfig {
    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBot telegramBot) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class); botsApi.registerBot(telegramBot); return botsApi;
    }
}