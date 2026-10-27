package com.afrinuts.farmos.data.local.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Granular farmer data-consent scopes. Stored as a list of stable string keys
 * in SupplierEntity.dataConsentScope (Room) and as a Firestore array.
 */
public final class ConsentScope {

    public static final String IDENTITY = "identity";
    public static final String LOCATION = "location";
    public static final String PHOTO    = "photo";

    private ConsentScope() {}

    /** Canonical English label used by the Buyer Portal. */
    public static String englishLabel(String key) {
        switch (key) {
            case IDENTITY: return "My identity — my name may be shown to verified buyers in the Buyer Portal.";
            case LOCATION: return "My precise location — the GPS coordinates of my farm may be shown to verified buyers in the Buyer Portal.";
            case PHOTO:    return "My photo — my photo may be shown to verified buyers in the Buyer Portal.";
            default:       return key;
        }
    }

    /** Canonical French label shown on the paper form in Odienné. */
    public static String frenchLabel(String key) {
        switch (key) {
            case IDENTITY: return "Mon identité — mon nom peut être montré aux acheteurs vérifiés dans le portail acheteur.";
            case LOCATION: return "Ma localisation précise — les coordonnées GPS de ma ferme peuvent être montrées aux acheteurs vérifiés dans le portail acheteur.";
            case PHOTO:    return "Ma photo — ma photo peut être montrée aux acheteurs vérifiés dans le portail acheteur.";
            default:       return key;
        }
    }

    public static List<String> all() {
        List<String> list = new ArrayList<>();
        list.add(IDENTITY);
        list.add(LOCATION);
        list.add(PHOTO);
        return list;
    }

    /** Normalize/validate a list coming from the UI or Firestore. */
    public static List<String> normalize(List<String> raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String s : raw) {
            if (s == null) continue;
            String k = s.trim().toLowerCase(Locale.ROOT);
            if (all().contains(k) && !out.contains(k)) out.add(k);
        }
        return out;
    }

    /** Renders the full English text stored in dataConsentText (audit trail). */
    public static String buildConsentText(String farmerName, List<String> scopes, String version) {
        String name = (farmerName == null || farmerName.isEmpty()) ? "[farmer name]" : farmerName;

        StringBuilder scopeText = new StringBuilder();
        for (String s : normalize(scopes)) {
            if (scopeText.length() > 0) scopeText.append(" ");
            // strip trailing '.' so we can join cleanly and add a single final period
            scopeText.append(englishLabel(s).replaceAll("\\.$", ""));
        }
        if (scopeText.length() == 0) scopeText.append("(none ticked)");

        return "Farmer Data Consent — I, " + name +
                ", consent to AfriNuts Export collecting and holding my identity, precise location (GPS), and photo in Farm OS, " +
                "and sharing them only with verified buyers through the Buyer Portal, and only where my consent has been recorded. " +
                "I understand that: my data remains mine, held in trust by AfriNuts; it will never be sold, never be shared in a way that harms farmers, " +
                "and never be used for price discrimination; and I may withdraw my consent at any time by contacting AfriNuts, after which my identity and precise location will be withheld from buyers. " +
                "Scopes ticked: " + scopeText + ". Consent version: " + version + ".";
    }
}