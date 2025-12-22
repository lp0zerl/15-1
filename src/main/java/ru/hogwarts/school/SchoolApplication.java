import java.util.Objects;

// ==================== 1. КЛАСС СОТРУДНИКА ====================
class Employee {
    private static int idCounter = 1;

    private final int id;
    private final String fullName;
    private int department;
    private double salary;

    // Конструктор
    public Employee(String fullName, int department, double salary) {
        this.id = idCounter++;
        this.fullName = fullName;
        setDepartment(department);
        setSalary(salary);
    }

    // Геттеры
    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public int getDepartment() {
        return department;
    }

    public double getSalary() {
        return salary;
    }

    // Сеттеры (только для отдела и зарплаты)
    public void setDepartment(int department) {
        if (department < 1 || department > 5) {
            throw new IllegalArgumentException("Отдел должен быть в диапазоне 1-5");
        }
        this.department = department;
    }

    public void setSalary(double salary) {
        if (salary < 50 || salary > 450) {
            throw new IllegalArgumentException("Зарплата должна быть в диапазоне 50-450");
        }
        this.salary = salary;
    }

    // Метод equals для бухгалтерского учета (сравнение только по зарплате)
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Employee employee = (Employee) obj;
        return Double.compare(employee.salary, salary) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(salary);
    }

    // Полная информация о сотруднике
    @Override
    public String toString() {
        return String.format("ID: %d, ФИО: %s, Отдел: %d, Зарплата: %.2f",
                id, fullName, department, salary);
    }

    // Короткая информация (имя и зарплата)
    public void printShortInfo() {
        System.out.printf("%s: %.2f%n", fullName, salary);
    }
}

// ==================== 2. КЛАСС КНИГИ СОТРУДНИКОВ ====================
class EmployeeBook {
    private final Employee[] employees;
    private int size; // Количество реально добавленных сотрудников

    public EmployeeBook() {
        this.employees = new Employee[10];
        this.size = 0;
    }

    // 1. Получить список всех сотрудников (кроме null)
    public void printAllEmployees() {
        System.out.println("=== СПИСОК ВСЕХ СОТРУДНИКОВ ===");
        boolean hasEmployees = false;
        for (Employee employee : employees) {
            if (employee != null) {
                System.out.println(employee);
                hasEmployees = true;
            }
        }
        if (!hasEmployees) {
            System.out.println("Сотрудники не найдены.");
        }
        System.out.println();
    }

    // 2. Подсчитать среднее значение зарплат
    public double calculateAverageSalary() {
        if (size == 0) {
            return 0.0;
        }

        double totalSalary = 0.0;
        for (Employee employee : employees) {
            if (employee != null) {
                totalSalary += employee.getSalary();
            }
        }
        return totalSalary / size;
    }

    // 3. Вывести значения налогов
    public void calculateAndPrintTaxes(String taxType) {
        System.out.println("=== РАСЧЕТ НАЛОГОВ (" + taxType + ") ===");

        for (Employee employee : employees) {
            if (employee != null) {
                double tax = 0.0;
                double salary = employee.getSalary();

                switch (taxType.toUpperCase()) {
                    case "PROPORTIONAL":
                        tax = salary * 0.13;
                        break;
                    case "PROGRESSIVE":
                        if (salary <= 150) {
                            tax = salary * 0.13;
                        } else if (salary <= 350) {
                            tax = salary * 0.17;
                        } else {
                            tax = salary * 0.21;
                        }
                        break;
                    default:
                        System.out.println("Неизвестный тип налога: " + taxType);
                        return;
                }

                System.out.printf("%s: зарплата=%.2f, налог=%.2f%n",
                        employee.getFullName(), salary, tax);
            }
        }
        System.out.println();
    }

    // 4. Индексация зарплат в отделе
    public void indexSalariesInDepartment(int department, double percentage) {
        System.out.printf("=== ИНДЕКСАЦИЯ ЗАРПЛАТ В ОТДЕЛЕ %d НА %.1f%% ===%n", department, percentage);
        boolean hasEmployees = false;

        for (Employee employee : employees) {
            if (employee != null && employee.getDepartment() == department) {
                hasEmployees = true;
                double oldSalary = employee.getSalary();
                double newSalary = oldSalary * (1 + percentage / 100);

                // Проверка диапазона зарплаты
                if (newSalary < 50 || newSalary > 450) {
                    System.out.printf("%s: индексация невозможна (новая зарплата %.2f вне диапазона)%n",
                            employee.getFullName(), newSalary);
                    continue;
                }

                employee.setSalary(newSalary);
                System.out.printf("%s: %.2f -> %.2f (+%.2f)%n",
                        employee.getFullName(), oldSalary, newSalary, newSalary - oldSalary);
            }
        }

        if (!hasEmployees) {
            System.out.printf("В отделе %d нет сотрудников.%n", department);
        }
        System.out.println();
    }

    // 5. Поиск первого сотрудника в отделе с зарплатой больше указанной
    public void findFirstEmployeeWithSalaryAbove(int department, double minSalary) {
        System.out.printf("=== ПОИСК СОТРУДНИКА В ОТДЕЛЕ %d С ЗАРПЛАТОЙ > %.2f ===%n", department, minSalary);

        for (int i = 0; i < employees.length; i++) {
            Employee employee = employees[i];
            if (employee != null &&
                    employee.getDepartment() == department &&
                    employee.getSalary() > minSalary) {

                System.out.printf("Найден сотрудник (индекс %d): ", i);
                employee.printShortInfo();
                System.out.println();
                return;
            }
        }

        System.out.printf("Сотрудник в отделе %d с зарплатой > %.2f не найден.%n%n", department, minSalary);
    }

    // 6. Вывод первых N сотрудников с зарплатой меньше указанной
    public void printEmployeesWithSalaryBelow(double maxSalary, int limit) {
        System.out.printf("=== ПЕРВЫЕ %d СОТРУДНИКОВ С ЗАРПЛАТОЙ < %.2f ===%n", limit, maxSalary);

        int count = 0;
        int index = 0;

        while (index < employees.length && count < limit) {
            Employee employee = employees[index];
            if (employee != null && employee.getSalary() < maxSalary) {
                System.out.printf("%d. ", ++count);
                employee.printShortInfo();
            }
            index++;
        }

        if (count == 0) {
            System.out.printf("Сотрудники с зарплатой < %.2f не найдены.%n", maxSalary);
        } else if (count < limit) {
            System.out.printf("Найдено только %d сотрудников (меньше запрошенных %d)%n", count, limit);
        }
        System.out.println();
    }

    // 7. Проверка наличия сотрудника по бухгалтерским критериям (по зарплате)
    public boolean hasEmployeeByAccountingCriteria(Employee target) {
        if (target == null) {
            return false;
        }

        for (Employee employee : employees) {
            if (employee != null && employee.equals(target)) {
                return true;
            }
        }
        return false;
    }

    // 8. Добавление нового сотрудника
    public boolean addEmployee(Employee employee) {
        if (employee == null) {
            System.out.println("Нельзя добавить null сотрудника");
            return false;
        }

        // Поиск свободной ячейки
        for (int i = 0; i < employees.length; i++) {
            if (employees[i] == null) {
                employees[i] = employee;
                size++;
                return true;
            }
        }

        return false;
    }

    // 9. Получение сотрудника по ID
    public Employee getEmployeeById(int id) {
        for (Employee employee : employees) {
            if (employee != null && employee.getId() == id) {
                return employee;
            }
        }
        return null;
    }

    // Вспомогательный метод для получения количества реальных сотрудников
    public int getSize() {
        return size;
    }

    // Вспомогательный метод для получения максимальной вместимости
    public int getCapacity() {
        return employees.length;
    }
}

// ==================== 3. ГЛАВНЫЙ КЛАСС ДЛЯ ТЕСТИРОВАНИЯ ====================
public class Main {
    public static void main(String[] args) {
        // Создание книги сотрудников
        EmployeeBook employeeBook = new EmployeeBook();

        System.out.println("=== ИНИЦИАЛИЗАЦИЯ И ТЕСТИРОВАНИЕ СИСТЕМЫ ===\n");

        // Тест 1: Добавление 11 сотрудников (на 1 больше, чем вместимость)
        System.out.println("=== ТЕСТ 1: ДОБАВЛЕНИЕ СОТРУДНИКОВ ===\n");
        System.out.println("Вместимость книги: " + employeeBook.getCapacity());

        String[] names = {
                "Иванов Иван Иванович",
                "Петров Петр Петрович",
                "Сидорова Анна Сергеевна",
                "Кузнецов Алексей Владимирович",
                "Смирнова Ольга Дмитриевна",
                "Васильев Михаил Андреевич",
                "Николаева Екатерина Павловна",
                "Морозов Денис Игоревич",
                "Павлова Мария Александровна",
                "Григорьев Сергей Викторович",
                "Алексеева Татьяна Николаевна" // Этот сотрудник не должен добавиться
        };

        int[] departments = {1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1};
        double[] salaries = {100.0, 200.0, 150.0, 300.0, 250.0, 180.0, 220.0, 170.0, 320.0, 280.0, 90.0};

        for (int i = 0; i < names.length; i++) {
            try {
                Employee employee = new Employee(names[i], departments[i], salaries[i]);
                boolean added = employeeBook.addEmployee(employee);
                System.out.printf("Добавление сотрудника '%s': %s%n",
                        names[i], added ? "УСПЕШНО" : "НЕ УДАЛОСЬ (нет места)");
            } catch (IllegalArgumentException e) {
                System.out.printf("Ошибка при добавлении '%s': %s%n", names[i], e.getMessage());
            }
        }

        System.out.println("\nТекущее количество сотрудников: " + employeeBook.getSize());
        System.out.println();

        // Тест 2: Вывод всех сотрудников
        employeeBook.printAllEmployees();

        // Тест 3: Расчет средней зарплаты
        System.out.printf("Средняя зарплата: %.2f%n%n", employeeBook.calculateAverageSalary());

        // Тест 4: Расчет налогов (пропорциональная система)
        employeeBook.calculateAndPrintTaxes("PROPORTIONAL");

        // Тест 5: Расчет налогов (прогрессивная система)
        employeeBook.calculateAndPrintTaxes("PROGRESSIVE");

        // Тест 6: Индексация зарплат в отделе 1 на 10%
        employeeBook.indexSalariesInDepartment(1, 10.0);

        // Тест 7: Индексация зарплат в отделе 2 на 50% (проверка ограничений)
        employeeBook.indexSalariesInDepartment(2, 50.0);

        // Тест 8: Поиск первого сотрудника в отделе 3 с зарплатой > 160
        employeeBook.findFirstEmployeeWithSalaryAbove(3, 160.0);

        // Тест 9: Поиск сотрудника в отделе 99 (несуществующий отдел)
        employeeBook.findFirstEmployeeWithSalaryAbove(99, 100.0);

        // Тест 10: Вывод первых 3 сотрудников с зарплатой < 200
        employeeBook.printEmployeesWithSalaryBelow(200.0, 3);

        // Тест 11: Вывод первых 10 сотрудников с зарплатой < 50 (не должно быть таких)
        employeeBook.printEmployeesWithSalaryBelow(50.0, 10);

        // Тест 12: Проверка наличия сотрудника по бухгалтерским критериям
        Employee testEmployee1 = new Employee("Тестовый Сотрудник", 1, 150.0);
        Employee testEmployee2 = new Employee("Другой Сотрудник", 2, 500.0); // Зарплата вне диапазона

        System.out.println("=== ТЕСТ 12: ПРОВЕРКА НАЛИЧИЯ СОТРУДНИКОВ ===");
        System.out.printf("Есть ли сотрудник с зарплатой %.2f: %s%n",
                testEmployee1.getSalary(),
                employeeBook.hasEmployeeByAccountingCriteria(testEmployee1));

        try {
            System.out.printf("Есть ли сотрудник с зарплатой %.2f: %s%n",
                    testEmployee2.getSalary(),
                    employeeBook.hasEmployeeByAccountingCriteria(testEmployee2));
        } catch (IllegalArgumentException e) {
            System.out.printf("Ошибка при создании тестового сотрудника: %s%n", e.getMessage());
        }
        System.out.println();

        // Тест 13: Поиск сотрудника по ID
        System.out.println("=== ТЕСТ 13: ПОИСК СОТРУДНИКОВ ПО ID ===");
        for (int i = 1; i <= 12; i++) {
            Employee found = employeeBook.getEmployeeById(i);
            if (found != null) {
                System.out.printf("Сотрудник с ID %d найден: %s%n", i, found.getFullName());
            } else {
                System.out.printf("Сотрудник с ID %d не найден%n", i);
            }
        }
        System.out.println();

        // Тест 14: Edge cases - работа с пустым массивом
        System.out.println("=== ТЕСТ 14: ПРОВЕРКА НА ПУСТОМ МАССИВЕ ===");
        EmployeeBook emptyBook = new EmployeeBook();
        emptyBook.printAllEmployees();
        System.out.printf("Средняя зарплата в пустой книге: %.2f%n", emptyBook.calculateAverageSalary());
        emptyBook.calculateAndPrintTaxes("PROPORTIONAL");
        emptyBook.indexSalariesInDepartment(1, 10.0);
        emptyBook.findFirstEmployeeWithSalaryAbove(1, 100.0);
        emptyBook.printEmployeesWithSalaryBelow(200.0, 3);

        // Тест 15: Проверка сеттеров с некорректными значениями
        System.out.println("=== ТЕСТ 15: ПРОВЕРКА ВАЛИДАЦИИ ===");
        try {
            Employee employee = employeeBook.getEmployeeById(1);
            if (employee != null) {
                System.out.println("Попытка установить зарплату 1000...");
                employee.setSalary(1000.0);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка валидации: " + e.getMessage());
        }

        try {
            Employee employee = employeeBook.getEmployeeById(2);
            if (employee != null) {
                System.out.println("Попытка установить отдел 10...");
                employee.setDepartment(10);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка валидации: " + e.getMessage());
        }

        // Тест 16: Проверка equals
        System.out.println("\n=== ТЕСТ 16: ПРОВЕРКА EQUALS ===");
        Employee emp1 = new Employee("Тест 1", 1, 200.0);
        Employee emp2 = new Employee("Тест 2", 2, 200.0);
        Employee emp3 = new Employee("Тест 3", 3, 300.0);

        System.out.printf("emp1.equals(emp2) (обе зарплаты 200): %s%n", emp1.equals(emp2));
        System.out.printf("emp1.equals(emp3) (200 vs 300): %s%n", emp1.equals(emp3));
        System.out.printf("emp1.equals(null): %s%n", emp1.equals(null));

        // Тест 17: Проверка printShortInfo
        System.out.println("\n=== ТЕСТ 17: ПРОВЕРКА printShortInfo ===");
        System.out.print("Краткая информация о сотруднике 1: ");
        emp1.printShortInfo();

        System.out.println("\n=== ТЕСТИРОВАНИЕ ЗАВЕРШЕНО ===");
    }
}