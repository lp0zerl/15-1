package ru.hogwarts.school;

import java.util.Objects;

public class Employee {
    private static int idCounter = 1;
    private final int id;
    private final String fullName;
    private int department;
    private double salary;

    public Employee(String fullName, int department, double salary) {
        this.id = idCounter++;
        this.fullName = fullName;
        setDepartment(department);
        setSalary(salary);
    }

    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public int getDepartment() { return department; }
    public double getSalary() { return salary; }

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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Employee employee = (Employee) obj;
        return Double.compare(employee.salary, salary) == 0;
    }

    @Override
    public int hashCode() { return Objects.hash(salary); }

    @Override
    public String toString() {
        return String.format("ID: %d, ФИО: %s, Отдел: %d, Зарплата: %.2f",
                id, fullName, department, salary);
    }

    public void printShortInfo() {
        System.out.printf("%s: %.2f%n", fullName, salary);
    }
}
