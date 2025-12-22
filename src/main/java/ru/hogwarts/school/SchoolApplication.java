package com.example.testing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ПОЛНЫЙ ПРОЕКТ ДЛЯ ТЕСТИРОВАНИЯ С MOCKITO И JUNIT
 * Скопируйте этот файл целиком в IntelliJ IDEA
 */

// ==================== 1. МОДЕЛЬНЫЕ КЛАССЫ ====================

class Product {
    private final Long id;
    private final String name;
    private final BigDecimal price;
    private final String category;

    public Product(Long id, String name, BigDecimal price, String category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getCategory() { return category; }

    @Override
    public String toString() {
        return name + " ($" + price + ")";
    }
}

class UserBasket {
    private final List<BasketItem> items;
    private final BigDecimal total;

    public UserBasket(List<BasketItem> items, BigDecimal total) {
        this.items = items;
        this.total = total;
    }

    public List<BasketItem> getItems() { return items; }
    public BigDecimal getTotal() { return total; }

    @Override
    public String toString() {
        return "Корзина: " + items.size() + " товаров, Итого: $" + total;
    }
}

class BasketItem {
    private final Product product;
    private final int quantity;

    public BasketItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }

    @Override
    public String toString() {
        return product.getName() + " x" + quantity + " = $" +
                product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}

// ==================== 2. ИНТЕРФЕЙСЫ СЕРВИСОВ ====================

interface StorageService {
    Optional<Product> getProductById(Long id);
    List<Product> getAllProducts();
    List<Product> findProductsByName(String name);
}

interface ProductBasket {
    void addProduct(Long productId);
    void removeProduct(Long productId);
    Map<Long, Integer> getItems();
    void clear();
    boolean isEmpty();
}

// ==================== 3. СЕРВИС ПОИСКА ====================

class SearchService {
    private final StorageService storageService;

    public SearchService(StorageService storageService) {
        this.storageService = storageService;
    }

    public List<Product> searchProducts(String searchPattern) {
        if (searchPattern == null || searchPattern.trim().isEmpty()) {
            return storageService.getAllProducts();
        }
        return storageService.findProductsByName(searchPattern);
    }

    public Optional<Product> searchProductById(Long id) {
        return storageService.getProductById(id);
    }
}

// ==================== 4. СЕРВИС КОРЗИНЫ ====================

class BasketService {
    private final StorageService storageService;
    private final ProductBasket productBasket;

    public BasketService(StorageService storageService, ProductBasket productBasket) {
        this.storageService = storageService;
        this.productBasket = productBasket;
    }

    public void addProduct(Long productId) {
        Optional<Product> product = storageService.getProductById(productId);
        if (product.isEmpty()) {
            throw new IllegalArgumentException("Товар с ID " + productId + " не найден");
        }
        productBasket.addProduct(productId);
    }

    public UserBasket getUserBasket() {
        Map<Long, Integer> basketItems = productBasket.getItems();
        List<BasketItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : basketItems.entrySet()) {
            Optional<Product> productOpt = storageService.getProductById(entry.getKey());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                int quantity = entry.getValue();
                BasketItem basketItem = new BasketItem(product, quantity);
                items.add(basketItem);

                BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
                total = total.add(itemTotal);
            }
        }

        return new UserBasket(items, total);
    }

    public void clearBasket() {
        productBasket.clear();
    }
}

// ==================== 5. РЕАЛИЗАЦИИ ДЛЯ ДЕМОНСТРАЦИИ ====================

class DemoStorageService implements StorageService {
    private final Map<Long, Product> products = new HashMap<>();

    public DemoStorageService() {
        // Добавляем тестовые товары
        products.put(1L, new Product(1L, "Ноутбук", new BigDecimal("999.99"), "Электроника"));
        products.put(2L, new Product(2L, "Смартфон", new BigDecimal("499.99"), "Электроника"));
        products.put(3L, new Product(3L, "Наушники", new BigDecimal("199.99"), "Электроника"));
        products.put(4L, new Product(4L, "Книга", new BigDecimal("29.99"), "Книги"));
        products.put(5L, new Product(5L, "Кофе", new BigDecimal("9.99"), "Продукты"));
    }

    @Override
    public Optional<Product> getProductById(Long id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    @Override
    public List<Product> findProductsByName(String name) {
        List<Product> result = new ArrayList<>();
        String searchLower = name.toLowerCase();
        for (Product product : products.values()) {
            if (product.getName().toLowerCase().contains(searchLower)) {
                result.add(product);
            }
        }
        return result;
    }
}

class DemoProductBasket implements ProductBasket {
    private final Map<Long, Integer> items = new HashMap<>();

    @Override
    public void addProduct(Long productId) {
        items.put(productId, items.getOrDefault(productId, 0) + 1);
    }

    @Override
    public void removeProduct(Long productId) {
        if (items.containsKey(productId)) {
            int quantity = items.get(productId);
            if (quantity > 1) {
                items.put(productId, quantity - 1);
            } else {
                items.remove(productId);
            }
        }
    }

    @Override
    public Map<Long, Integer> getItems() {
        return new HashMap<>(items);
    }

    @Override
    public void clear() {
        items.clear();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }
}

// ==================== 6. ТЕСТЫ SEARCH SERVICE ====================

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    StorageService storageService;

    @InjectMocks
    SearchService searchService;

    @Test
    void test1_ПоискВПустомХранилище() {
        // Подготовка
        when(storageService.findProductsByName("ноутбук")).thenReturn(Collections.emptyList());

        // Действие
        List<Product> результаты = searchService.searchProducts("ноутбук");

        // Проверка
        assertThat(результаты).isEmpty();
        verify(storageService).findProductsByName("ноутбук");
    }

    @Test
    void test2_ПоискБезСовпадений() {
        // Подготовка
        Product продукт = new Product(1L, "ТестовыйПродукт", BigDecimal.TEN, "Категория");
        when(storageService.findProductsByName("Другой")).thenReturn(Collections.emptyList());

        // Действие
        List<Product> результаты = searchService.searchProducts("Другой");

        // Проверка
        assertThat(результаты).isEmpty();
    }

    @Test
    void test3_ПоискССовпадением() {
        // Подготовка
        Product продукт = new Product(1L, "ТестовыйПродукт", BigDecimal.TEN, "Категория");
        when(storageService.findProductsByName("Тест")).thenReturn(List.of(продукт));

        // Действие
        List<Product> результаты = searchService.searchProducts("Тест");

        // Проверка
        assertThat(результаты).hasSize(1);
        assertThat(результаты.get(0).getName()).isEqualTo("ТестовыйПродукт");
    }

    @Test
    void test4_ПоискСПустымЗапросом() {
        // Подготовка
        Product продукт1 = new Product(1L, "Продукт1", BigDecimal.TEN, "Кат1");
        Product продукт2 = new Product(2L, "Продукт2", BigDecimal.valueOf(20), "Кат2");
        when(storageService.getAllProducts()).thenReturn(List.of(продукт1, продукт2));

        // Действие
        List<Product> результаты = searchService.searchProducts("");

        // Проверка
        assertThat(результаты).hasSize(2);
    }

    @Test
    void test5_ПоискПоНесуществующемуID() {
        // Подготовка
        when(storageService.getProductById(999L)).thenReturn(Optional.empty());

        // Действие
        Optional<Product> результат = searchService.searchProductById(999L);

        // Проверка
        assertThat(результат).isEmpty();
    }
}

// ==================== 7. ТЕСТЫ BASKET SERVICE ====================

@ExtendWith(MockitoExtension.class)
class BasketServiceTest {

    @Mock
    StorageService storageService;

    @Mock
    ProductBasket productBasket;

    @InjectMocks
    BasketService basketService;

    @Test
    void test1_ДобавлениеНесуществующегоТовара() {
        // Подготовка
        when(storageService.getProductById(999L)).thenReturn(Optional.empty());

        // Действие и проверка
        assertThrows(IllegalArgumentException.class, () -> {
            basketService.addProduct(999L);
        });

        verify(storageService).getProductById(999L);
        verify(productBasket, never()).addProduct(anyLong());
    }

    @Test
    void test2_ДобавлениеСуществующегоТовара() {
        // Подготовка
        Product продукт = new Product(1L, "Тестовый", BigDecimal.TEN, "Категория");
        when(storageService.getProductById(1L)).thenReturn(Optional.of(продукт));

        // Действие
        basketService.addProduct(1L);

        // Проверка
        verify(productBasket, times(1)).addProduct(1L);
    }

    @Test
    void test3_ПолучениеПустойКорзины() {
        // Подготовка
        when(productBasket.getItems()).thenReturn(Collections.emptyMap());

        // Действие
        UserBasket корзина = basketService.getUserBasket();

        // Проверка
        assertThat(корзина.getItems()).isEmpty();
        assertThat(корзина.getTotal()).isZero();
    }

    @Test
    void test4_ПолучениеНепустойКорзины() {
        // Подготовка
        Product продукт = new Product(1L, "Товар", new BigDecimal("100.00"), "Кат");
        Map<Long, Integer> товарыВКорзине = Map.of(1L, 2);

        when(productBasket.getItems()).thenReturn(товарыВКорзине);
        when(storageService.getProductById(1L)).thenReturn(Optional.of(продукт));

        // Действие
        UserBasket корзина = basketService.getUserBasket();

        // Проверка
        assertThat(корзина.getItems()).hasSize(1);
        assertThat(корзина.getTotal()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void test5_ОчисткаКорзины() {
        // Действие
        basketService.clearBasket();

        // Проверка
        verify(productBasket).clear();
    }

    @Test
    void test6_КорзинаСНесколькимиТоварами() {
        // Подготовка
        Product продукт1 = new Product(1L, "Товар1", new BigDecimal("50.00"), "Кат");
        Product продукт2 = new Product(2L, "Товар2", new BigDecimal("30.00"), "Кат");
        Map<Long, Integer> товарыВКорзине = Map.of(1L, 3, 2L, 2);

        when(productBasket.getItems()).thenReturn(товарыВКорзине);
        when(storageService.getProductById(1L)).thenReturn(Optional.of(продукт1));
        when(storageService.getProductById(2L)).thenReturn(Optional.of(продукт2));

        // Действие
        UserBasket корзина = basketService.getUserBasket();

        // Проверка
        assertThat(корзина.getItems()).hasSize(2);
        BigDecimal ожидаемаяСумма = new BigDecimal("50.00").multiply(BigDecimal.valueOf(3))
                .add(new BigDecimal("30.00").multiply(BigDecimal.valueOf(2)));
        assertThat(корзина.getTotal()).isEqualByComparingTo(ожидаемаяСумма);
    }
}

// ==================== 8. ТЕСТЫ ДЛЯ ПРОВЕРКИ КРИТЕРИЕВ ====================

@ExtendWith(MockitoExtension.class)
class CriteriaTests {

    @Mock StorageService storageService;
    @Mock ProductBasket productBasket;
    @InjectMocks SearchService searchService;
    @InjectMocks BasketService basketService;

    // Критерий 1: Поиск в пустом хранилище
    @Test
    void критерий1_ПоискВПустомХранилище() {
        when(storageService.findProductsByName("что-то")).thenReturn(Collections.emptyList());
        List<Product> результаты = searchService.searchProducts("что-то");
        assertThat(результаты).isEmpty();
    }

    // Критерий 2: Поиск без совпадений
    @Test
    void критерий2_ПоискБезСовпадений() {
        Product тестовый = new Product(1L, "TestProduct", BigDecimal.TEN, "Категория");
        when(storageService.findProductsByName("Другой")).thenReturn(Collections.emptyList());
        List<Product> результаты = searchService.searchProducts("Другой");
        assertThat(результаты).isEmpty();
    }

    // Критерий 3: Поиск с совпадением
    @Test
    void критерий3_ПоискССовпадением() {
        Product продукт = new Product(1L, "TestProduct", BigDecimal.TEN, "Категория");
        when(storageService.findProductsByName("Test")).thenReturn(List.of(продукт));
        List<Product> результаты = searchService.searchProducts("Test");
        assertThat(результаты).hasSize(1);
    }

    // Критерий 4: Добавление несуществующего товара
    @Test
    void критерий4_ДобавлениеНесуществующегоТовара() {
        when(storageService.getProductById(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> basketService.addProduct(999L));
    }

    // Критерий 5: Добавление существующего товара
    @Test
    void критерий5_ДобавлениеСуществующегоТовара() {
        Product продукт = new Product(1L, "Товар", BigDecimal.TEN, "Категория");
        when(storageService.getProductById(1L)).thenReturn(Optional.of(продукт));
        basketService.addProduct(1L);
        verify(productBasket, times(1)).addProduct(1L);
    }

    // Критерий 6: Получение пустой корзины
    @Test
    void критерий6_ПолучениеПустойКорзины() {
        when(productBasket.getItems()).thenReturn(Collections.emptyMap());
        UserBasket корзина = basketService.getUserBasket();
        assertThat(корзина.getItems()).isEmpty();
        assertThat(корзина.getTotal()).isZero();
    }

    // Критерий 7: Получение непустой корзины
    @Test
    void критерий7_ПолучениеНепустойКорзины() {
        Product продукт = new Product(1L, "Товар", new BigDecimal("100.00"), "Категория");
        Map<Long, Integer> товары = Map.of(1L, 2);
        when(productBasket.getItems()).thenReturn(товары);
        when(storageService.getProductById(1L)).thenReturn(Optional.of(продукт));
        UserBasket корзина = basketService.getUserBasket();
        assertThat(корзина.getItems()).hasSize(1);
        assertThat(корзина.getTotal()).isEqualByComparingTo(new BigDecimal("200.00"));
    }
}

// ==================== 9. ИНТЕГРАЦИОННЫЙ ТЕСТ ====================

@ExtendWith(MockitoExtension.class)
class IntegrationTest {

    @Mock StorageService storageService;
    @Mock ProductBasket productBasket;

    @Test
    void интеграционныйТест_ПоискИДобавлениеВКорзину() {
        // Подготовка
        Product ноутбук = new Product(1L, "Ноутбук", new BigDecimal("999.99"), "Электроника");
        when(storageService.findProductsByName("Ноут")).thenReturn(List.of(ноутбук));
        when(storageService.getProductById(1L)).thenReturn(Optional.of(ноутбук));

        // Поиск
        SearchService поискСервис = new SearchService(storageService);
        List<Product> найденные = поискСервис.searchProducts("Ноут");
        assertThat(найденные).hasSize(1);

        // Добавление в корзину
        BasketService корзинаСервис = new BasketService(storageService, productBasket);
        корзинаСервис.addProduct(1L);
        verify(productBasket).addProduct(1L);
    }
}

// ==================== 10. ГЛАВНЫЙ КЛАСС ДЛЯ ЗАПУСКА ====================

public class Main {

    // Демонстрация работы сервисов
    public static void демонстрацияРаботы() {
        System.out.println("=== ДЕМОНСТРАЦИЯ РАБОТЫ СЕРВИСОВ ===\n");

        // Создаем реальные сервисы
        StorageService хранилище = new DemoStorageService();
        ProductBasket корзина = new DemoProductBasket();
        SearchService поиск = new SearchService(хранилище);
        BasketService сервисКорзины = new BasketService(хранилище, корзина);

        // Поиск товаров
        System.out.println("1. ПОИСК ТОВАРОВ:");
        System.out.println("   Все товары: " + поиск.searchProducts(""));
        System.out.println("   Поиск 'ноут': " + поиск.searchProducts("ноут"));
        System.out.println("   Поиск 'кофе': " + поиск.searchProducts("кофе"));

        // Работа с корзиной
        System.out.println("\n2. РАБОТА С КОРЗИНОЙ:");
        try {
            сервисКорзины.addProduct(1L); // Ноутбук
            сервисКорзины.addProduct(1L); // Еще ноутбук
            сервисКорзины.addProduct(3L); // Наушники
            сервисКорзины.addProduct(5L); // Кофе

            System.out.println("   Добавлены товары в корзину");
            System.out.println("   " + сервисКорзины.getUserBasket());

            // Удаляем один ноутбук
            ((DemoProductBasket) корзина).removeProduct(1L);
            System.out.println("   Удален 1 ноутбук");
            System.out.println("   " + сервисКорзины.getUserBasket());

        } catch (Exception e) {
            System.out.println("   Ошибка: " + e.getMessage());
        }

        // Очистка корзины
        сервисКорзины.clearBasket();
        System.out.println("\n3. КОРЗИНА ОЧИЩЕНА:");
        System.out.println("   " + сервисКорзины.getUserBasket());

        System.out.println("\n=== ДЕМОНСТРАЦИЯ ЗАВЕРШЕНА ===\n");
    }

    // Запуск тестов вручную
    public static void запускТестов() {
        System.out.println("=== ЗАПУСК ТЕСТОВ ===");

        try {
            SearchServiceTest тестПоиска = new SearchServiceTest();

            // Инициализация теста поиска
            тестПоиска.storageService = mock(StorageService.class);
            тестПоиска.searchService = new SearchService(тестПоиска.storageService);

            System.out.println("✓ Тесты SearchService созданы");

            BasketServiceTest тестКорзины = new BasketServiceTest();
            тестКорзины.storageService = mock(StorageService.class);
            тестКорзины.productBasket = mock(ProductBasket.class);
            тестКорзины.basketService = new BasketService(
                    тестКорзины.storageService,
                    тестКорзины.productBasket
            );

            System.out.println("✓ Тесты BasketService созданы");
            System.out.println("\nДля запуска JUnit тестов используйте:");
            System.out.println("1. Правый клик на классе теста → Run");
            System.out.println("2. Или запустите через Maven: mvn test");

        } catch (Exception e) {
            System.out.println("Ошибка при создании тестов: " + e.getMessage());
        }
    }

    // Главный метод
    public static void main(String[] args) {
        System.out.println("======================================");
        System.out.println("   ТЕСТИРОВАНИЕ С MOCKITO И JUNIT    ");
        System.out.println("======================================\n");

        демонстрацияРаботы();
        запускТестов();

        System.out.println("\n======================================");
        System.out.println("       ПРОЕКТ ГОТОВ К РАБОТЕ          ");
        System.out.println("======================================");
    }
}
