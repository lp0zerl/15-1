package ru.hogwarts.school;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class TelegramBotService extends TelegramLongPollingBot {
    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    @Value("${telegram.bot.token:test_token}")
    private String botToken;

    @Value("${telegram.bot.username:test_bot}")
    private String botUsername;

    public TelegramBotService(RecommendationService recommendationService, UserRepository userRepository) {
        this.recommendationService = recommendationService;
        this.userRepository = userRepository;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                sendHelpMessage(chatId);
            } else if (messageText.startsWith("/recommend ")) {
                handleRecommendCommand(chatId, messageText);
            } else {
                sendUnknownCommandMessage(chatId);
            }
        }
    }

    private void handleRecommendCommand(Long chatId, String messageText) {
        try {
            String[] parts = messageText.split(" ", 2);
            if (parts.length < 2) {
                sendMessage(chatId, "Пожалуйста, укажите имя пользователя: /recommend Имя Фамилия");
                return;
            }

            String username = parts[1].trim();
            String[] nameParts = username.split(" ");

            if (nameParts.length != 2) {
                sendMessage(chatId, "Пожалуйста, укажите имя и фамилию через пробел: /recommend Иван Иванов");
                return;
            }

            String firstName = nameParts[0];
            String lastName = nameParts[1];

            List<UserInfo> users = userRepository.findUsersByName(firstName, lastName);

            if (users.isEmpty()) {
                sendMessage(chatId, "Пользователь не найден");
            } else if (users.size() > 1) {
                sendMessage(chatId, "Найдено несколько пользователей. Пожалуйста, уточните запрос.");
            } else {
                UserInfo user = users.get(0);
                RecommendationResponse response = recommendationService.getRecommendations(user.getId());
                sendRecommendations(chatId, user, response);
            }

        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка при обработке запроса");
        }
    }

    private void sendRecommendations(Long chatId, UserInfo user, RecommendationResponse response) {
        StringBuilder message = new StringBuilder();
        message.append("Здравствуйте, ").append(user.getFirstName()).append(" ").append(user.getLastName()).append("!\n\n");

        if (response.getRecommendations().isEmpty()) {
            message.append("К сожалению, у нас пока нет персональных рекомендаций для вас.");
        } else {
            message.append("Новые продукты для вас:\n\n");

            for (Recommendation recommendation : response.getRecommendations()) {
                message.append("• ").append(recommendation.getName()).append("\n");
                message.append("  ").append(recommendation.getText()).append("\n\n");
            }
        }

        sendMessage(chatId, message.toString());
    }

    private void sendHelpMessage(Long chatId) {
        String helpText = "Добро пожаловать в банк «Стар»! 🏦\n\n" +
                "Доступные команды:\n" +
                "/start - показать это сообщение\n" +
                "/recommend Имя Фамилия - получить персональные рекомендации\n\n" +
                "Пример: /recommend Иван Иванов";
        sendMessage(chatId, helpText);
    }

    private void sendUnknownCommandMessage(Long chatId) {
        sendMessage(chatId, "Неизвестная команда. Используйте /start для справки.");
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}