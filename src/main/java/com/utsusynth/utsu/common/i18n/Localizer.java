package com.utsusynth.utsu.common.i18n;

import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import com.google.common.collect.ImmutableList;

public class Localizer {
	private final ImmutableList<NativeLocale> supportedLocales;
	private final LinkedList<Localizable> targets;
	private NativeLocale locale;
	private ResourceBundle bundle;

	public Localizer(NativeLocale locale, List<NativeLocale> supportedLocales) {
		this.supportedLocales = ImmutableList.copyOf(supportedLocales);
		this.targets = new LinkedList<>();
		this.locale = locale;
		this.bundle = ResourceBundle.getBundle("messages.messages", locale.getLocale());
	}

	public void localize(Localizable target) {
		if (targets.contains(target)) {
			// TODO: Handle this, or just ignore it. I'm not a cop.
			return;
		}
		targets.add(target);
		target.localize(this.bundle);
	}

	public void setLocale(NativeLocale locale) {
		this.locale = locale;
		this.bundle = ResourceBundle.getBundle("messages.messages", locale.getLocale());
		for (Localizable target : targets) {
			target.localize(this.bundle);
		}
	}

	public NativeLocale getCurrentLocale() {
		return locale;
	}

	public List<NativeLocale> getAllLocales() {
		return supportedLocales;
	}
}
