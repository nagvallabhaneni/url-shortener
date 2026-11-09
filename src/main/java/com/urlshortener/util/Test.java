package com.urlshortener.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Test {
    public static void main(String[] args) {
        practiceCollections();
        practiceStreams();
    }

    private static void practiceCollections() {
        System.out.println("=== Collections ===");

        List<String> languages = new ArrayList<>(List.of("Java", "Python", "JavaScript"));
        languages.add("Kotlin");
        languages.remove("Python");
        System.out.println("List (ordered): " + languages);

        Set<String> uniqueLanguages = new HashSet<>(languages);
        uniqueLanguages.add("Java");
        System.out.println("Set (unique values): " + uniqueLanguages);

        Map<String, Integer> visitsByPage = new HashMap<>();
        visitsByPage.put("home", 12);
        visitsByPage.put("about", 5);
        visitsByPage.merge("home", 3, Integer::sum);
        System.out.println("Map (page visits): " + visitsByPage);

        Deque<String> recentPages = new ArrayDeque<>();
        recentPages.addFirst("home");
        recentPages.addFirst("shorten");
        recentPages.addLast("stats");
        System.out.println("Deque (first to last): " + recentPages);
        System.out.println("Next page to process: " + recentPages.removeFirst());
    }

    private static void practiceStreams() {
        System.out.println("\n=== Stream API ===");

        List<Integer> numbers = List.of(8, 3, 12, 5, 8, 2, 15, 4);

        List<Integer> evenNumbers = numbers.stream()
                .filter(number -> number % 2 == 0)
                .distinct()
                .sorted()
                .toList();
        System.out.println("Unique even numbers, sorted: " + evenNumbers);

        int sumOfSquares = numbers.stream()
                .map(number -> number * number)
                .reduce(0, Integer::sum);
        System.out.println("Sum of squares: " + sumOfSquares);

        Map<Boolean, List<Integer>> numbersByParity = numbers.stream()
                .collect(Collectors.partitioningBy(number -> number % 2 == 0));
        System.out.println("Partitioned by even/odd: " + numbersByParity);

        List<Product> products = List.of(
                new Product("Keyboard", "Accessories", 45.00),
                new Product("Mouse", "Accessories", 25.00),
                new Product("Monitor", "Electronics", 220.00),
                new Product("Webcam", "Electronics", 80.00),
                new Product("Notebook", "Stationery", 6.50));

        Map<String, List<String>> productsByCategory = products.stream()
                .collect(Collectors.groupingBy(
                        Product::category,
                        Collectors.mapping(Product::name, Collectors.toList())));
        System.out.println("Products by category: " + productsByCategory);

        products.stream()
                .filter(product -> product.price() >= 50)
                .sorted(Comparator.comparingDouble(Product::price).reversed())
                .forEach(product -> System.out.printf(
                        "%s: $%.2f%n", product.name(), product.price()));

        products.stream()
                .mapToDouble(Product::price)
                .average()
                .ifPresent(average -> System.out.printf("Average product price: $%.2f%n", average));
    }

    private record Product(String name, String category, double price) {
    }
}
