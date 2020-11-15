package com.utsusynth.utsu.common.i18n;

import java.util.Locale;

/**
 * A locale with toString overridden as the localized name of that locale.
 */
public class NativeLocale {
    private final Locale locale;
    private final String localizedName;

    public NativeLocale(Locale locale) {
        this.locale = locale;
        this.localizedName = locale.getDisplayLanguage(locale)
                + (locale.getCountry().isEmpty() ? "" : " (" + locale.getCountry() + ")");
    }

    public Locale getLocale() {
        return locale;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof NativeLocale)) {
            return false;
        }
        NativeLocale otherLocale = (NativeLocale) other;
        return this.localizedName.equals(otherLocale.localizedName);
    }

    @Override
    public String toString() {
        return localizedName;
    }
}
