
public class EmployeeBook {
    private final Employee[] employees;
    private int size;

    public EmployeeBook() {
        this.employees = new Employee[10];
        this.size = 0;
    }

    public void printAllEmployees() {
        System.out.println("=== СПИСОК ВСЕХ СОТРУДНИКОВ ===");
        boolean hasEmployees = false;
        for (Employee employee : employees) {
            if (employee != null) {
                System.out.println(employee);
                hasEmployees = true;
            }
        }
        if (!hasEmployees) System.out.println("Сотрудники не найдены.");
        System.out.println();
    }

    public double calculateAverageSalary() {
        if (size == 0) return 0.0;
        double totalSalary = 0.0;
        for (Employee employee : employees) {
            if (employee != null) totalSalary += employee.getSalary();
        }
        return totalSalary / size;
    }

    public void calculateAndPrintTaxes(String taxType) {
        System.out.println("=== РАСЧЕТ НАЛОГОВ (" + taxType + ") ===");
        for (Employee employee : employees) {
            if (employee != null) {
                double tax = 0.0;
                double salary = employee.getSalary();
                switch (taxType.toUpperCase()) {
                    case "PROPORTIONAL": tax = salary * 0.13; break;
                    case "PROGRESSIVE":
                        if (salary <= 150) tax = salary * 0.13;
                        else if (salary <= 350) tax = salary * 0.17;
                        else tax = salary * 0.21;
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

    public void indexSalariesInDepartment(int department, double percentage) {
        System.out.printf("=== ИНДЕКСАЦИЯ ЗАРПЛАТ В ОТДЕЛЕ %d НА %.1f%% ===%n", department, percentage);
        boolean hasEmployees = false;
        for (Employee employee : employees) {
            if (employee != null && employee.getDepartment() == department) {
                hasEmployees = true;
                double oldSalary = employee.getSalary();
                double newSalary = oldSalary * (1 + percentage / 100);
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
        if (!hasEmployees) System.out.printf("В отделе %d нет сотрудников.%n", department);
        System.out.println();
    }

    public void findFirstEmployeeWithSalaryAbove(int department, double minSalary) {
        System.out.printf("=== ПОИСК СОТРУДНИКА В ОТДЕЛЕ %d С ЗАРПЛАТОЙ > %.2f ===%n", department, minSalary);
        for (int i = 0; i < employees.length; i++) {
            Employee employee = employees[i];
            if (employee != null && employee.getDepartment() == department && employee.getSalary() > minSalary) {
                System.out.printf("Найден сотрудник (индекс %d): ", i);
                employee.printShortInfo();
                System.out.println();
                return;
            }
        }
        System.out.printf("Сотрудник в отделе %d с зарплатой > %.2f не найден.%n%n", department, minSalary);
    }

    public void printEmployeesWithSalaryBelow(double maxSalary, int limit) {
        System.out.printf("=== ПЕРВЫЕ %d СОТРУДНИКОВ С ЗАРПЛАТОЙ < %.2f ===%n", limit, maxSalary);
        int count = 0, index = 0;
        while (index < employees.length && count < limit) {
            Employee employee = employees[index];
            if (employee != null && employee.getSalary() < maxSalary) {
                System.out.printf("%d. ", ++count);
                employee.printShortInfo();
            }
            index++;
        }
        if (count == 0) System.out.printf("Сотрудники с зарплатой < %.2f не найдены.%n", maxSalary);
        else if (count < limit) System.out.printf("Найдено только %d сотрудников (меньше запрошенных %d)%n", count, limit);
        System.out.println();
    }

    public boolean hasEmployeeByAccountingCriteria(Employee target) {
        if (target == null) return false;
        for (Employee employee : employees) {
            if (employee != null && employee.equals(target)) return true;
        }
        return false;
    }

    public boolean addEmployee(Employee employee) {
        if (employee == null) {
            System.out.println("Нельзя добавить null сотрудника");
            return false;
        }
        for (int i = 0; i < employees.length; i++) {
            if (employees[i] == null) {
                employees[i] = employee;
                size++;
                return true;
            }
        }
        return false;
    }

    public Employee getEmployeeById(int id) {
        for (Employee employee : employees) {
            if (employee != null && employee.getId() == id) return employee;
        }
        return null;
    }

    public int getSize() { return size; }
    public int getCapacity() { return employees.length; }
}

public class Main {
    public static void main(String[] args) {
        EmployeeBook employeeBook = new EmployeeBook();
        System.out.println("=== ИНИЦИАЛИЗАЦИЯ И ТЕСТИРОВАНИЕ СИСТЕМЫ ===\n");

        System.out.println("=== ТЕСТ 1: ДОБАВЛЕНИЕ СОТРУДНИКОВ ===\n");
        System.out.println("Вместимость книги: " + employeeBook.getCapacity());

        String[] names = {
                "Иванов Иван Иванович", "Петров Петр Петрович", "Сидорова Анна Сергеевна",
                "Кузнецов Алексей Владимирович", "Смирнова Ольга Дмитриевна", "Васильев Михаил Андреевич",
                "Николаева Екатерина Павловна", "Морозов Денис Игоревич", "Павлова Мария Александровна",
                "Григорьев Сергей Викторович", "Алексеева Татьяна Николаевна"
        };

        int[] departments = {1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1};
        double[] salaries = {100.0, 200.0, 150.0, 300.0, 250.0, 180.0, 220.0, 170.0, 320.0, 280.0, 90.0};

        for (int i = 0; i < names.length; i++) {
            try {
                Employee employee = new Employee(names[i], departments[i], salaries[i]);
                boolean added = employeeBook.addEmployee(employee);
                System.out.printf("Добавление сотрудника '%s': %s%n", names[i], added ? "УСПЕШНО" : "НЕ УДАЛОСЬ (нет места)");
            } catch (IllegalArgumentException e) {
                System.out.printf("Ошибка при добавлении '%s': %s%n", names[i], e.getMessage());
            }
        }

        System.out.println("\nТекущее количество сотрудников: " + employeeBook.getSize());
        System.out.println();

        employeeBook.printAllEmployees();
        System.out.printf("Средняя зарплата: %.2f%n%n", employeeBook.calculateAverageSalary());
        employeeBook.calculateAndPrintTaxes("PROPORTIONAL");
        employeeBook.calculateAndPrintTaxes("PROGRESSIVE");
        employeeBook.indexSalariesInDepartment(1, 10.0);
        employeeBook.indexSalariesInDepartment(2, 50.0);
        employeeBook.findFirstEmployeeWithSalaryAbove(3, 160.0);
        employeeBook.findFirstEmployeeWithSalaryAbove(99, 100.0);
        employeeBook.printEmployeesWithSalaryBelow(200.0, 3);
        employeeBook.printEmployeesWithSalaryBelow(50.0, 10);

        System.out.println("=== ТЕСТ 12: ПРОВЕРКА НАЛИЧИЯ СОТРУДНИКОВ ===");
        Employee testEmployee1 = new Employee("Тестовый Сотрудник", 1, 150.0);
        Employee testEmployee2 = new Employee("Другой Сотрудник", 2, 500.0);
        System.out.printf("Есть ли сотрудник с зарплатой %.2f: %s%n",
                testEmployee1.getSalary(), employeeBook.hasEmployeeByAccountingCriteria(testEmployee1));
        try {
            System.out.printf("Есть ли сотрудник с зарплатой %.2f: %s%n",
                    testEmployee2.getSalary(), employeeBook.hasEmployeeByAccountingCriteria(testEmployee2));
        } catch (IllegalArgumentException e) {
            System.out.printf("Ошибка при создании тестового сотрудника: %s%n", e.getMessage());
        }
        System.out.println();

        System.out.println("=== ТЕСТ 13: ПОИСК СОТРУДНИКОВ ПО ID ===");
        for (int i = 1; i <= 12; i++) {
            Employee found = employeeBook.getEmployeeById(i);
            if (found != null) System.out.printf("Сотрудник с ID %d найден: %s%n", i, found.getFullName());
            else System.out.printf("Сотрудник с ID %d не найден%n", i);
        }
        System.out.println();

        System.out.println("=== ТЕСТ 14: ПРОВЕРКА НА ПУСТОМ МАССИВЕ ===");
        EmployeeBook emptyBook = new EmployeeBook();
        emptyBook.printAllEmployees();
        System.out.printf("Средняя зарплата в пустой книге: %.2f%n", emptyBook.calculateAverageSalary());
        emptyBook.calculateAndPrintTaxes("PROPORTIONAL");
        emptyBook.indexSalariesInDepartment(1, 10.0);
        emptyBook.findFirstEmployeeWithSalaryAbove(1, 100.0);
        emptyBook.printEmployeesWithSalaryBelow(200.0, 3);

        System.out.println("=== ТЕСТ 15: ПРОВЕРКА ВАЛИДАЦИИ ===");
        try {
            Employee employee = employeeBook.getEmployeeById(1);
            if (employee != null) {
                System.out.println("Попытка установить зарплату 1000...");
                employee.setSalary(1000.0);
            }
        } catch (IllegalArgumentException e) { System.out.println("Ошибка валидации: " + e.getMessage()); }

        try {
            Employee employee = employeeBook.getEmployeeById(2);
            if (employee != null) {
                System.out.println("Попытка установить отдел 10...");
                employee.setDepartment(10);
            }
        } catch (IllegalArgumentException e) { System.out.println("Ошибка валидации: " + e.getMessage()); }

        System.out.println("\n=== ТЕСТ 16: ПРОВЕРКА EQUALS ===");
        Employee emp1 = new Employee("Тест 1", 1, 200.0);
        Employee emp2 = new Employee("Тест 2", 2, 200.0);
        Employee emp3 = new Employee("Тест 3", 3, 300.0);
        System.out.printf("emp1.equals(emp2) (обе зарплаты 200): %s%n", emp1.equals(emp2));
        System.out.printf("emp1.equals(emp3) (200 vs 300): %s%n", emp1.equals(emp3));
        System.out.printf("emp1.equals(null): %s%n", emp1.equals(null));

        System.out.println("\n=== ТЕСТ 17: ПРОВЕРКА printShortInfo ===");
        System.out.print("Краткая информация о сотруднике 1: ");
        emp1.printShortInfo();
        System.out.println("\n=== ТЕСТИРОВАНИЕ ЗАВЕРШЕНО ===");
    }
}