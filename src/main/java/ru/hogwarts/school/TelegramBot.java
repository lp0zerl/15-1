package ru.hogwarts.school;

@Component class TelegramBot extends TelegramLongPollingBot {
    private final String botToken = "test_bot_token"; private final String botUsername = "test_recommendation_bot";
    private final UserRecommendationService userRecommendationService;
    public TelegramBot(UserRecommendationService userRecommendationService) { this.userRecommendationService = userRecommendationService; }
    @Override public String getBotToken() { return botToken; } @Override public String getBotUsername() { return botUsername; }

    @Override public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage(); String text = message.getText(); Long chatId = message.getChatId();
            if (text.startsWith("/start")) sendHelpMessage(chatId);
            else if (text.startsWith("/recommend")) handleRecommendCommand(chatId, text);
            else sendHelpMessage(chatId);
        }
    }

    private void handleRecommendCommand(Long chatId, String text) {
        try { String[] parts = text.split(" ", 2);
            if (parts.length < 2) { sendMessage(chatId, "Использование: /recommend username"); return; }
            String username = parts[1].trim(); String recommendations = userRecommendationService.getRecommendationsForUsername(username);
            sendMessage(chatId, recommendations);
        } catch (Exception e) { sendMessage(chatId, "Произошла ошибка при обработке запроса"); e.printStackTrace(); }
    }

    private void sendHelpMessage(Long chatId) {
        String helpText = """
                Привет! Я бот для получения финансовых рекомендаций.

                Доступные команды:
                /start - показать это сообщение
                /recommend <username> - получить рекомендации для пользователя

                Пример:
                /recommend ivanov
                """;
        sendMessage(chatId, helpText);
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(); message.setChatId(chatId.toString()); message.setText(text);
        try { execute(message); } catch (TelegramApiException e) { e.printStackTrace(); }
    }
}