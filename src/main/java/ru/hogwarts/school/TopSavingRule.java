package ru.hogwarts.school;


class TopSavingRule implements RecommendationRule {
    private static final String PRODUCT_ID = "59efc529-2fff-41af-baff-90ccd7402925";
    private static final String PRODUCT_NAME = "Top Saving";
    private static final String PRODUCT_DESCRIPTION = "Откройте свою собственную «Копилку» с нашим банком! «Копилка» — это уникальный банковский инструмент, который поможет вам легко и удобно накапливать деньги на важные цели. Больше никаких забытых чеков и потерянных квитанций — всё под контролем!\n\nПреимущества «Копилки»:\n\nНакопление средств на конкретные цели. Установите лимит и срок накопления, и банк будет автоматически переводить определенную сумму на ваш счет.\n\nПрозрачность и контроль. Отслеживайте свои доходы и расходы, контролируйте процесс накопления и корректируйте стратегию при необходимости.\n\nБезопасность и надежность. Ваши средства находятся под защитой банка, а доступ к ним возможен только через мобильное приложение или интернет-банкинг.\n\nНачните использовать «Копилку» уже сегодня и станьте ближе к своим финансовым целям!";

    private final BankRepository bankRepository;

    public TopSavingRule(BankRepository bankRepository) {
        this.bankRepository = bankRepository;
    }

    @Override
    public Optional<Recommendation> check(String userId) {
        boolean usesDebit = bankRepository.usesProductType(userId, "DEBIT");
        BigDecimal debitDeposits = bankRepository.getTotalDebitDeposits(userId);
        BigDecimal savingDeposits = bankRepository.getTotalSavingDeposits(userId);
        boolean depositsOver50k = debitDeposits.compareTo(new BigDecimal("50000")) >= 0 ||
                savingDeposits.compareTo(new BigDecimal("50000")) >= 0;
        BigDecimal debitExpenses = bankRepository.getTotalDebitExpenses(userId);
        boolean depositsGreaterThanExpenses = debitDeposits.compareTo(debitExpenses) > 0;

        if (usesDebit && depositsOver50k && depositsGreaterThanExpenses) {
            return Optional.of(new Recommendation(PRODUCT_NAME, PRODUCT_ID, PRODUCT_DESCRIPTION));
        }
        return Optional.empty();
    }
}