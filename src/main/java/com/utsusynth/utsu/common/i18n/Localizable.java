package com.utsusynth.utsu.common.i18n;

import java.util.ResourceBundle;

/** Class containing visual elements that can be re-localized at any time. */
public interface Localizable {
	void localize(ResourceBundle bundle);
}
