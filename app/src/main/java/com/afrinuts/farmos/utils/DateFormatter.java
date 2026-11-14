package com.afrinuts.farmos.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Single source of truth for rendering sentinel date fields
 * (targetDate / approvalDate / expiryDate, and document issue/expiry dates).
 * 0 = unset (Build Instructions §1 step 1). Unset renders as "—", never 1970.
 */
public final class DateFormatter {

    public static final String UNSET = "—";

    private DateFormatter() {
    }

    public static String formatDateOrDash(long date) {
        if (date <= 0) return UNSET;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(date));
    }

    public static String formatDateOrDash(Long date) {
        return date == null ? UNSET : formatDateOrDash(date.longValue());
    }
}