package com.brainridge.banking.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Small helper for keeping money values consistent.
 *
 * <p>All amounts in the system are stored to exactly two decimal places (like
 * real cents). Normalizing in one place means a balance is never accidentally
 * stored as {@code 100.0} in one spot and {@code 100.00} in another, which
 * keeps comparisons and display predictable.
 *
 * <p>The class is {@code final} with a private constructor because it is a pure
 * utility — it holds no state and should never be instantiated; you just call
 * {@link #normalize(BigDecimal)} directly.
 */
public final class MoneyUtils {

    /** Number of decimal places we keep for money (2 = cents). */
    private static final int SCALE = 2;

    private MoneyUtils() {
    }

    /**
     * Rounds an amount to two decimal places using standard "round half up"
     * rounding (so 1.005 becomes 1.01).
     */
    public static BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
