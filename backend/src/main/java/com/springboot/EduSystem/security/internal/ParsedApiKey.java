package com.springboot.EduSystem.security.internal;

import java.util.regex.Pattern;
import org.springframework.security.authentication.BadCredentialsException;

public record ParsedApiKey(String keyId) {

    private static final Pattern FORMAT = Pattern.compile(
            "([A-Za-z0-9_-]{1,64})\\.([A-Za-z0-9_-]{43})");

    public static ParsedApiKey parse(String rawKey) {
        if (rawKey == null || rawKey.length() > 108) {
            throw invalidKey();
        }

        var matcher = FORMAT.matcher(rawKey);

        if (!matcher.matches()) {
            throw invalidKey();
        }

        return new ParsedApiKey(matcher.group(1));
    }

    private static BadCredentialsException invalidKey() {
        return new BadCredentialsException("Invalid service credential");
    }

}
