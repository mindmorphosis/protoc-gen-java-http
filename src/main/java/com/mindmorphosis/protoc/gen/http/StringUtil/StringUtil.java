package com.mindmorphosis.protoc.gen.http.StringUtil;

public class StringUtil {
    public static String toPascalCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String[] words = input.split("_");
        StringBuilder pascalCase = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                pascalCase.append(Character.toUpperCase(word.charAt(0)));
                pascalCase.append(word.substring(1));
            }
        }

        return pascalCase.toString();
    }

    public static String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String[] words = input.split("_");
        StringBuilder camelCase = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (!word.isEmpty()) {
                if (i == 0) {
                    camelCase.append(Character.toLowerCase(word.charAt(0)));
                } else {
                    camelCase.append(Character.toUpperCase(word.charAt(0)));
                }
                camelCase.append(word.substring(1));
            }
        }

        return camelCase.toString();
    }
}
