package com.urlshortener.util;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class UniCodeTest {

    static class Point {
        int x;
        int y;
        char ch;

        Point(int x, int y, char ch) {
            this.x = x;
            this.y = y;
            this.ch = ch;
        }
    }

    public static void printSecretMessage(String urlString) throws Exception {

        // 1. Download the Google Doc
        URL url = new URL(urlString);
        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        StringBuilder html = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        connection.getInputStream(),
                        StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {
                html.append(line);
            }
        }

        // 2. Extract all table cells
        List<String> cells = extractCells(html.toString());

        List<Point> points = new ArrayList<>();

        // 3. Process cells in groups of 3
        // x | character | y
        for (int i = 0; i + 2 < cells.size(); i += 3) {

            try {
                int x = Integer.parseInt(cells.get(i).trim());

                String character = cells.get(i + 1).trim();

                int y = Integer.parseInt(cells.get(i + 2).trim());

                if (!character.isEmpty()) {
                    points.add(
                            new Point(x, y, character.charAt(0))
                    );
                }

            } catch (NumberFormatException e) {
                // Header row - ignore
            }
        }

        if (points.isEmpty()) {
            System.out.println("No data found.");
            return;
        }

        // 4. Find grid size
        int maxX = 0;
        int maxY = 0;

        for (Point p : points) {
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
        }

        // 5. Create grid
        char[][] grid = new char[maxY + 1][maxX + 1];

        for (char[] row : grid) {
            Arrays.fill(row, ' ');
        }

        // 6. Place characters
        for (Point p : points) {
            grid[p.y][p.x] = p.ch;
        }

        // 7. Print result
        for (char[] row : grid) {
            System.out.println(new String(row));
        }
    }


    // Extract text inside <td>...</td>
    static List<String> extractCells(String html) {

        List<String> cells = new ArrayList<>();

        String lower = html.toLowerCase();

        int position = 0;

        while (true) {

            int start = lower.indexOf("<td", position);

            if (start == -1) {
                break;
            }

            int startTagEnd = lower.indexOf(">", start);

            if (startTagEnd == -1) {
                break;
            }

            int end = lower.indexOf("</td>", startTagEnd);

            if (end == -1) {
                break;
            }

            String value =
                    html.substring(startTagEnd + 1, end);

            // Remove HTML tags
            value = value.replaceAll("<[^>]*>", "");

            // Decode common HTML entities
            value = value
                    .replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&#39;", "'")
                    .replace("&quot;", "\"");

            cells.add(value.trim());

            position = end + 5;
        }

        return cells;
    }


    public static void main(String[] args) throws Exception {

        String url =
                "https://docs.google.com/document/d/e/2PACX-1vSvM5gDlNvt7npYHhp_XfsJvuntUhq184By5xO_pA4b_gCWeXb6dM6ZxwN8rE6S4ghUsCj2VKR21oEP/pub";

        printSecretMessage(url);
    }
}