package com.urlshortener.util;

public class StringUtils {
    private final String str = "IADRIDRSLSLTLRILLI";
    private final String word = "DRILL";

    public static void main(String[] args) {
        StringUtils utils = new StringUtils();

        int literalCount = utils.countLiteralOccurrences(utils.str, utils.word);
        int subsequenceCount = utils.countSubsequenceOccurrences(utils.str, utils.word);

        System.out.println("Literal substring occurrences: " + literalCount);
        System.out.println("Subsequence occurrences: " + subsequenceCount);
        System.out.println("Contains as subsequence: " + utils.isSubsequence(utils.str, utils.word));
    }

    public boolean isSubsequence(String str, String word) {
        return countSubsequenceOccurrences(str, word) > 0;
    }

    public int countLiteralOccurrences(String str, String word) {
        if (str == null || word == null || word.isEmpty()) {
            return 0;
        }

        int count = 0;
        int start = 0;

        while (start <= str.length() - word.length()) {
            int index = str.indexOf(word, start);
            if (index == -1) {
                break;
            }
            count++;
            start = index + 1;
        }

        return count;
    }

    public int countSubsequenceOccurrences(String str, String word) {
        if (str == null || word == null) {
            return 0;
        }

        if (word.isEmpty()) {
            return 1;
        }

        int[][] dp = new int[str.length() + 1][word.length() + 1];
        dp[0][0] = 1;

        for (int i = 0; i < str.length(); i++) {
            for (int j = 0; j <= word.length(); j++) {
                dp[i + 1][j] += dp[i][j];

                if (j > 0 && str.charAt(i) == word.charAt(j - 1)) {
                    dp[i + 1][j] += dp[i][j - 1];
                }
            }
        }

        return dp[str.length()][word.length()];
    }
}



