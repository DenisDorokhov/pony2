package net.dorokhov.pony.util;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Based on https://gist.github.com/jakubkulhan/9b408a9a1d2683f9ee23303c36ba23ad.
 */
public class RandomString {
    
    private static final String ALPHABET = "0123456789ABCDEF";
    private static final Random RANDOM = new SecureRandom();

    public static String generate(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; ++i) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
