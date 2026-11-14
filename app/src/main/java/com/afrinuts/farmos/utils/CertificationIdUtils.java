package com.afrinuts.farmos.utils;

/**
 * Programme-segment and sequence helpers shared by all five ID generators
 * and the five repositories.
 */
public final class CertificationIdUtils {

    private CertificationIdUtils() {}

    /** Programme code (ORG/HALAL/FT/ISO/GAP/RAIN/UTZ/...) from a certification name. */
    public static String programmeCodeFromName(String certificationName) {
        if (certificationName == null) return "CERT";
        String name = certificationName.toUpperCase();
        if (name.contains("ORGANIC")) return "ORG";
        if (name.contains("HALAL")) return "HALAL";
        if (name.contains("FAIR TRADE") || name.contains("FAIRTRADE")) return "FT";
        if (name.contains("ISO")) return "ISO";
        if (name.contains("GLOBALGAP") || name.contains("GLOBAL GAP")) return "GAP";
        if (name.contains("RAINFOREST")) return "RAIN";
        if (name.contains("UTZ")) return "UTZ";
        if (name.contains("ECOCERT")) return "ECO";
        if (name.contains("CERES")) return "CER";
        return "CERT";
    }

    /**
     * Programme code parsed from a parent certificationId.
     * CERT-ORG-2026-001 -> ORG ; ORG-ECO-2026-011 -> ORG ; null/junk -> CERT.
     */
    public static String programmeCodeFromCertificationId(String certificationId) {
        if (certificationId == null || certificationId.isEmpty()) return "CERT";
        String[] parts = certificationId.split("-");
        if (parts.length >= 4 && "CERT".equalsIgnoreCase(parts[0])) {
            return parts[1].toUpperCase();
        }
        if (parts.length >= 3) {
            return parts[0].toUpperCase();
        }
        return "CERT";
    }

    /** Trailing sequence of an ID under the given prefix, or 0 when none. */
    public static int trailingSequence(String id, String prefix) {
        if (id == null || prefix == null || !id.startsWith(prefix)) return 0;
        String tail = id.substring(prefix.length());
        try {
            return Integer.parseInt(tail);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** User-facing message for an ID-collision insertAbort. */
    public static String collisionMessage() {
        return "ID collision — refresh and retry";
    }
}