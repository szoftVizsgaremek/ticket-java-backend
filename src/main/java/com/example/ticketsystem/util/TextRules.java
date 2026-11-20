package com.example.ticketsystem.util;

import com.example.ticketsystem.exception.BadRequestException;

public class TextRules {
    
    public static String require(String value, int max, String label) {
        if (value == null) {
            throw new BadRequestException(label + " is required");
        }
        String trimmed = value.strip();
        if (trimmed.isEmpty() || trimmed.length() > max) {
            throw new BadRequestException(label + " must be 1-" + max + " characters");
        }
        return trimmed;
    }
}
