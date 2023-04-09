package com.mindmorphosis.protoc.gen.http.util;

public class StringUtil {

    /**
     * 将字符串转换为大驼峰
     *
     * @param input
     * @return
     */
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

    /**
     * 将字符串转换为小驼峰
     *
     * @param input
     * @return
     */
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
