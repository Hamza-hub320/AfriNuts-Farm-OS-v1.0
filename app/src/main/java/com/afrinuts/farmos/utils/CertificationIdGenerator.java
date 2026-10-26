package com.afrinuts.farmos.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class CertificationIdGenerator {

    private static final AtomicInteger counter = new AtomicInteger(1);

    public static String generateCertificationId(String certificationName, String certificationBody) {
        // Extract prefix from certification name
        String prefix = getPrefix(certificationName);
        String bodyCode = getBodyCode(certificationBody);
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        String sequence = String.format("%03d", counter.getAndIncrement() % 1000);

        return prefix + "-" + bodyCode + "-" + year + "-" + sequence;
    }

    private static String getPrefix(String certificationName) {
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

    private static String getBodyCode(String certificationBody) {
        if (certificationBody == null) return "UNK";

        String body = certificationBody.toUpperCase();
        if (body.contains("ECOCERT")) return "ECO";
        if (body.contains("CERES")) return "CER";
        if (body.contains("SOIL ASSOCIATION")) return "SOIL";
        if (body.contains("USDA")) return "USDA";
        if (body.contains("EU")) return "EU";
        if (body.contains("GLOBALGAP") || body.contains("GLOBAL GAP")) return "GAP";
        if (body.contains("FAIRTRADE") || body.contains("FAIR TRADE")) return "FT";
        if (body.contains("HALAL")) return "HAL";
        if (body.contains("ISO")) return "ISO";
        if (body.contains("ORGANIC")) return "ORG";
        return body.substring(0, Math.min(3, body.length()));
    }

    public static void resetCounter() {
        counter.set(1);
    }
}