package com.boldtechnology.nailsalonregister;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
    private Money() {}

    public static String format(long cents) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(cents / 100.0);
    }

    public static long parseToCents(String value) {
        String clean = value == null ? "" : value.replace("$", "").replace(",", "").trim();
        if (clean.isEmpty()) {
            throw new NumberFormatException("Price is required");
        }
        BigDecimal decimal = new BigDecimal(clean).setScale(2, RoundingMode.HALF_UP);
        if (decimal.signum() < 0) {
            throw new NumberFormatException("Price cannot be negative");
        }
        return decimal.movePointRight(2).longValueExact();
    }
}
