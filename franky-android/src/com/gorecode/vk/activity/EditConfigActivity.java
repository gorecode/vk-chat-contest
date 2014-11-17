package com.gorecode.vk.activity;

import java.util.Vector;

import roboguice.activity.RoboPreferenceActivity;
import roboguice.inject.InjectExtra;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;

import com.gorecode.vk.application.VkApplication;
import com.gorecode.vk.config.ApplicationConfig;
import com.gorecode.vk.config.Config;
import com.gorecode.vk.config.PreferenceScheme;
import com.gorecode.vk.utilities.ErrorHandlingUtilities;
import com.uva.lang.BooleanUtilities;
import com.uva.log.Log;

public class EditConfigActivity extends RoboPreferenceActivity {
	private static final String TAG = "EditConfigActivity";

	private static final String EXTRA_XML_RESOURCE = "xmlResource";

	private VkApplication mApplication;
	private ApplicationConfig mConfig;

	@InjectExtra(EXTRA_XML_RESOURCE)
	private int xmlResource;

	public static void display(Context caller, int preferencesXmlResource) {
		Intent intent = new Intent(caller, EditConfigActivity.class);

		intent.putExtra(EXTRA_XML_RESOURCE, preferencesXmlResource);

		caller.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mApplication = (VkApplication)getApplication();

		mConfig = mApplication.getConfig();

		initializeSharedPreferencesFromConfig();

		super.onCreate(savedInstanceState);

		addPreferencesFromResource(xmlResource);

		initializePreferenceViews();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		mApplication.saveConfig();
	}

	private void initializeSharedPreferencesFromConfig() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = prefs.edit();
		editor.clear();

		PreferenceScheme[] schemes = mConfig.getConfigScheme().getAllPreferenceSchemes();

		Config config = mApplication.getConfig();

		for (int i = 0; i < schemes.length; i++) {
			PreferenceScheme scheme = schemes[i];

			String value = config.getString(scheme);

			if (scheme.category == PreferenceScheme.Category.BOOLEAN) {
				editor.putBoolean(scheme.key, BooleanUtilities.parse(value));
			} else {
				editor.putString(scheme.key, value);
			}
		}

		editor.commit();
	}

	private void initializePreferenceViews() {
		Vector<Preference> prefsVector = getAllPrimitivePreferencesRecursively(getPreferenceScreen());

		for (Preference pref : prefsVector) {
			PreferenceScheme scheme = null;

			String key = pref.getKey();

			if (key == null) continue;

			try {
				scheme = mConfig.getConfigScheme().getPreferenceScheme(key);
			} catch (IllegalArgumentException e) {
				Log.warning(TAG, "Cannot find scheme for key '" + key + "' in config scheme");
				continue;
			}

			pref.setOnPreferenceChangeListener(mOptionValidator);
			pref.setDefaultValue(scheme.defaultValue);
		}
	}

	private static Vector<Preference> getAllPrimitivePreferencesRecursively(PreferenceGroup prefGroup) {
		final Vector<Preference> prefs = new Vector<Preference>();

		int count = prefGroup.getPreferenceCount();

		for (int i = 0; i < count; i++) {
			Preference each = prefGroup.getPreference(i);

			if (each instanceof PreferenceGroup) {
				Vector<Preference> children = getAllPrimitivePreferencesRecursively((PreferenceGroup)each);

				prefs.addAll(children);
			} else {
				prefs.add(each);
			}
		}

		return prefs;
	}

	private final Preference.OnPreferenceChangeListener mOptionValidator = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			
			String prefKey = preference.getKey();
			String stringVal = newValue.toString();	

			try {
				PreferenceScheme optionScheme = mConfig.getConfigScheme().getPreferenceScheme(prefKey);
				mConfig.setPreference(optionScheme, stringVal);

				return true;
			} catch (IllegalArgumentException e) {
				Log.exception(TAG, e);
				
				ErrorHandlingUtilities.displayErrorSoftly(EditConfigActivity.this, e);
				
				return false;
			}
		}
	};
}
