// Единый файл со всем кодом проекта рекомендательной системы банка

// 1. DTO классы
class RecommendationResponse {
	private String userId;
	private List<Recommendation> recommendations;

	public RecommendationResponse() {}

	public RecommendationResponse(String userId, List<Recommendation> recommendations) {
		this.userId = userId;
		this.recommendations = recommendations;
	}

	public String getUserId() { return userId; }
	public void setUserId(String userId) { this.userId = userId; }
	public List<Recommendation> getRecommendations() { return recommendations; }
	public void setRecommendations(List<Recommendation> recommendations) { this.recommendations = recommendations; }
}

class Recommendation {
	private String name;
	private String id;
	private String text;

	public Recommendation() {}

	public Recommendation(String name, String id, String text) {
		this.name = name;
		this.id = id;
		this.text = text;
	}

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	public String getText() { return text; }
	public void setText(String text) { this.text = text; }
}

// 2. Репозиторий
@Repository
class BankRepository {
	private final JdbcTemplate jdbcTemplate;

	public BankRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public boolean usesProductType(String userId, String productType) {
		String sql = """
            SELECT COUNT(*) > 0
            FROM transactions t
            JOIN products p ON t.product_id = p.id
            WHERE t.user_id = ? AND p.type = ?
            """;
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, userId, productType));
	}

	public BigDecimal getTotalDepositsByProductType(String userId, String productType) {
		String sql = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM transactions t
            JOIN products p ON t.product_id = p.id
            WHERE t.user_id = ? AND p.type = ? AND t.operation_type = 'DEPOSIT'
            """;
		return jdbcTemplate.queryForObject(sql, BigDecimal.class, userId, productType);
	}

	public BigDecimal getTotalExpensesByProductType(String userId, String productType) {
		String sql = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM transactions t
            JOIN products p ON t.product_id = p.id
            WHERE t.user_id = ? AND p.type = ? AND t.operation_type = 'EXPENSE'
            """;
		return jdbcTemplate.queryForObject(sql, BigDecimal.class, userId, productType);
	}

	public BigDecimal getTotalSavingDeposits(String userId) {
		return getTotalDepositsByProductType(userId, "SAVING");
	}

	public BigDecimal getTotalDebitDeposits(String userId) {
		return getTotalDepositsByProductType(userId, "DEBIT");
	}

	public BigDecimal getTotalDebitExpenses(String userId) {
		return getTotalExpensesByProductType(userId, "DEBIT");
	}

	public boolean userExists(String userId) {
		String sql = "SELECT COUNT(*) > 0 FROM transactions WHERE user_id = ?";
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, userId));
	}
}

// 3. Интерфейс правила рекомендации
interface RecommendationRule {
	Optional<Recommendation> check(String userId);
}

// 4. Реализации правил рекомендаций
@Component
class Invest500Rule implements RecommendationRule {
	private static final String PRODUCT_ID = "147f6a0f-3b91-413b-ab99-87f081d60d5a";
	private static final String PRODUCT_NAME = "Invest 500";
	private static final String PRODUCT_DESCRIPTION = "Откройте свой путь к успеху с индивидуальным инвестиционным счетом (ИИС) от нашего банка! Воспользуйтесь налоговыми льготами и начните инвестировать с умом. Пополните счет до конца года и получите выгоду в виде вычета на взнос в следующем налоговом периоде. Не упустите возможность разнообразить свой портфель, снизить риски и следить за актуальными рыночными тенденциями. Откройте ИИС сегодня и станьте ближе к финансовой независимости!";

	private final BankRepository bankRepository;

	public Invest500Rule(BankRepository bankRepository) {
		this.bankRepository = bankRepository;
	}

	@Override
	public Optional<Recommendation> check(String userId) {
		boolean usesDebit = bankRepository.usesProductType(userId, "DEBIT");
		boolean notUsesInvest = !bankRepository.usesProductType(userId, "INVEST");
		BigDecimal savingDeposits = bankRepository.getTotalSavingDeposits(userId);
		boolean savingDepositsOver1000 = savingDeposits.compareTo(new BigDecimal("1000")) > 0;

		if (usesDebit && notUsesInvest && savingDepositsOver1000) {
			return Optional.of(new Recommendation(PRODUCT_NAME, PRODUCT_ID, PRODUCT_DESCRIPTION));
		}
		return Optional.empty();
	}
}

@Component
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

@Component
class SimpleCreditRule implements RecommendationRule {
	private static final String PRODUCT_ID = "ab138afb-f3ba-4a93-b74f-0fcee86d447f";
	private static final String PRODUCT_NAME = "Простой кредит";
	private static final String PRODUCT_DESCRIPTION = "Откройте мир выгодных кредитов с нами!\n\nИщете способ быстро и без лишних хлопот получить нужную сумму? Тогда наш выгодный кредит — именно то, что вам нужно! Мы предлагаем низкие процентные ставки, гибкие условия и индивидуальный подход к каждому клиенту.\n\nПочему выбирают нас:\n\nБыстрое рассмотрение заявки. Мы ценим ваше время, поэтому процесс рассмотрения заявки занимает всего несколько часов.\n\nУдобное оформление. Подать заявку на кредит можно онлайн на нашем сайте или в мобильном приложении.\n\nШирокий выбор кредитных продуктов. Мы предлагаем кредиты на различные цели: покупку недвижимости, автомобиля, образование, лечение и многое другое.\n\nНе упустите возможность воспользоваться выгодными условиями кредитования от нашей компании!";

	private final BankRepository bankRepository;

	public SimpleCreditRule(BankRepository bankRepository) {
		this.bankRepository = bankRepository;
	}

	@Override
	public Optional<Recommendation> check(String userId) {
		boolean notUsesCredit = !bankRepository.usesProductType(userId, "CREDIT");
		BigDecimal debitDeposits = bankRepository.getTotalDebitDeposits(userId);
		BigDecimal debitExpenses = bankRepository.getTotalDebitExpenses(userId);
		boolean depositsGreaterThanExpenses = debitDeposits.compareTo(debitExpenses) > 0;
		boolean expensesOver100k = debitExpenses.compareTo(new BigDecimal("100000")) > 0;

		if (notUsesCredit && depositsGreaterThanExpenses && expensesOver100k) {
			return Optional.of(new Recommendation(PRODUCT_NAME, PRODUCT_ID, PRODUCT_DESCRIPTION));
		}
		return Optional.empty();
	}
}

// 5. Сервис рекомендаций
@Service
class RecommendationService {
	private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

	private final List<RecommendationRule> recommendationRules;
	private final BankRepository bankRepository;

	public RecommendationService(List<RecommendationRule> recommendationRules, BankRepository bankRepository) {
		this.recommendationRules = recommendationRules;
		this.bankRepository = bankRepository;
	}

	public RecommendationResponse getRecommendations(String userId) {
		logger.info("Getting recommendations for user: {}", userId);

		if (!bankRepository.userExists(userId)) {
			logger.warn("User not found: {}", userId);
			return new RecommendationResponse(userId, List.of());
		}

		List<Recommendation> recommendations = recommendationRules.stream()
				.map(rule -> rule.check(userId))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toList());

		logger.info("Found {} recommendations for user: {}", recommendations.size(), userId);
		return new RecommendationResponse(userId, recommendations);
	}
}

// 6. Контроллер
@RestController
class RecommendationController {
	private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

	private final RecommendationService recommendationService;

	public RecommendationController(RecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@GetMapping("/recommendation/{userId}")
	public RecommendationResponse getRecommendations(@PathVariable String userId) {
		logger.info("Received recommendation request for user: {}", userId);
		return recommendationService.getRecommendations(userId);
	}
}

// 7. Конфигурация
@Configuration
class DatabaseConfig {
	@Bean
	public JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}
}

@SpringBootApplication
public class BankApplication {
	public static void main(String[] args) {
		SpringApplication.run(BankApplication.class, args);
	}
}

// 8. Тесты
@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {
	@Mock
	private BankRepository bankRepository;
	@Mock
	private RecommendationRule rule1;
	@Mock
	private RecommendationRule rule2;
	@InjectMocks
	private RecommendationService recommendationService;

	@Test
	void getRecommendations_WhenUserExists_ShouldReturnRecommendations() {
		String userId = "test-user";
		Recommendation recommendation = new Recommendation("Test Product", "test-id", "Test description");

		when(bankRepository.userExists(userId)).thenReturn(true);
		when(rule1.check(userId)).thenReturn(Optional.of(recommendation));
		when(rule2.check(userId)).thenReturn(Optional.empty());

		RecommendationResponse response = recommendationService.getRecommendations(userId);

		assertNotNull(response);
		assertEquals(userId, response.getUserId());
		assertEquals(1, response.getRecommendations().size());
		assertEquals(recommendation, response.getRecommendations().get(0));
	}

	@Test
	void getRecommendations_WhenUserNotExists_ShouldReturnEmptyList() {
		String userId = "non-existent-user";
		when(bankRepository.userExists(userId)).thenReturn(false);

		RecommendationResponse response = recommendationService.getRecommendations(userId);

		assertNotNull(response);
		assertEquals(userId, response.getUserId());
		assertTrue(response.getRecommendations().isEmpty());
	}
}

@ExtendWith(MockitoExtension.class)
class Invest500RuleTest {
	@Mock
	private BankRepository bankRepository;
	@InjectMocks
	private Invest500Rule invest500Rule;

	@Test
	void check_WhenAllConditionsMet_ShouldReturnRecommendation() {
		String userId = "test-user";
		when(bankRepository.usesProductType(userId, "DEBIT")).thenReturn(true);
		when(bankRepository.usesProductType(userId, "INVEST")).thenReturn(false);
		when(bankRepository.getTotalSavingDeposits(userId)).thenReturn(new BigDecimal("1500"));

		Optional<Recommendation> result = invest500Rule.check(userId);

		assertTrue(result.isPresent());
		assertEquals("Invest 500", result.get().getName());
		assertEquals("147f6a0f-3b91-413b-ab99-87f081d60d5a", result.get().getId());
	}
}