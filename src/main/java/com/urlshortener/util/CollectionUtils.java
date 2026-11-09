package com.urlshortener.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class CollectionUtils {
    public static void main(String[] args) throws IOException {
        String input = readInput();
        List<Integer> list = new ArrayList<Integer>();
        String[] numbers = input.split(",");
        for (String number : numbers) {
            list.add(Integer.parseInt(number.trim()));
            System.out.println("Added number: " + number.trim());
        }
        Map<Boolean, Set<Integer>> partitioned = list.stream()
                .collect(Collectors.partitioningBy(
                        n -> n % 2 == 0,         // Condition: true for even, false for odd
                        Collectors.toSet()       // Downstream collector to group them into Sets
                ));

// Extract the separate sets
        Set<Integer> evenNumbers = partitioned.get(true);
        Set<Integer> oddNumbers = partitioned.get(false);
        System.out.println(evenNumbers);
        System.out.println(oddNumbers);

        List<Integer> sorted = list.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        System.out.println(sorted);
//        list.stream().collect(Collectors.groupingBy())

        System.out.println(list.stream().sorted(Comparator.reverseOrder()).skip(2).findFirst().get());


    }

    public static String readInput() throws IOException {
        BufferedReader br = new  BufferedReader(new InputStreamReader(System.in));
        StringBuffer sb = new StringBuffer();
        String line = null;
        while ((line = br.readLine()) !=null && !line.trim().isEmpty()) {
            sb.append(line);
        }
        return sb.toString();
    }

}
