package com.gorecode.vk.activity;

import roboguice.inject.InjectView;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.danikula.aibolit.Aibolit;
import com.danikula.aibolit.annotation.InjectOnClickListener;
import com.gorecode.vk.R;
import com.google.inject.Inject;
import com.gorecode.vk.application.BuildInfo;
import com.gorecode.vk.config.ApplicationConfig;
import com.gorecode.vk.sync.Session;
import com.gorecode.vk.task.LongAction;
import com.gorecode.vk.utilities.ErrorHandlingUtilities;
import com.gorecode.vk.utilities.GenericErrorAnalyzer;
import com.uva.log.Log;

public class LoginActivity extends VkActivity implements TextWatcher, View.OnFocusChangeListener {
	public static class Preferences {
		public static final String PREF_LAST_LOGIN = "lastLogin";
		public static final String PREF_LAST_PASSWORD = "lastPassword";

		public static SharedPreferences getSharedPreferences(Context context) {
			return context.getSharedPreferences(LoginActivity.class.getSimpleName(), MODE_PRIVATE);
		}

		public static void saveLoginAndPassword(SharedPreferences prefs, String email, String password) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(Preferences.PREF_LAST_LOGIN, email);
			editor.putString(Preferences.PREF_LAST_PASSWORD, password);
			editor.commit();
		}
	}

	private static final String TAG = LoginActivity.class.getSimpleName();

	private static final int REQUEST_CODE_REGISTER_USER = 0x0;

	@InjectView(R.id.login)
	private EditText loginEdit;
	@InjectView(R.id.password)
	private EditText passwordEdit;
	@InjectView(R.id.login_button)
	private Button loginButton;
	@InjectView(R.id.password_img)
	private ImageView lockView;
	@InjectView(R.id.phone_img)
	private ImageView phoneView;

	@Inject
	private ApplicationConfig appConfig;
	@Inject
	private Session session;

	private SharedPreferences prefs;

	public static void display(Context context) {
		Intent intent = new Intent(context, LoginActivity.class);

		context.startActivity(intent);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.login_activity);

		prefs = Preferences.getSharedPreferences(this);

		if (session.hasAccessToken()) {
			switchToNextActivity();
		} else {
			setUpViews();

			updateViews();
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		savePreferences();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (BuildInfo.IS_DEBUG_BUILD) {
			getMenuInflater().inflate(R.menu.activity_login, menu);
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.extended_configuration:
			EditConfigActivity.display(this, R.xml.extended_preferences);
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_CODE_REGISTER_USER && resultCode == RESULT_OK) {
			loginEdit.setText(data.getStringExtra(RegistrationActivity.EXTRA_LOGIN));
			passwordEdit.setText(data.getStringExtra(RegistrationActivity.EXTRA_PASSWORD));

			onLoginButtonClick(null);
		}
	}
	
	@Override
	public void afterTextChanged(Editable s) {
		updateViews();
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		updateViews();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		;
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		;
	}

	@InjectOnClickListener(R.id.login_button)
	protected void onLoginButtonClick(View view) {
		String username = loginEdit.getText().toString();
		String password = passwordEdit.getText().toString();

		new LoginTask(this, username, password).execute();
	}

	@InjectOnClickListener(R.id.registration_button)
	protected void onRegistrationButtonClick(View view) {
		startActivityForResult(new Intent(this, RegistrationActivity.class), REQUEST_CODE_REGISTER_USER);
	}

	private void savePreferences() {
		Preferences.saveLoginAndPassword(prefs, loginEdit.getText().toString(), passwordEdit.getText().toString());
	}

	private void switchToNextActivity() {
		startActivity(new Intent(LoginActivity.this, MainActivity.class));
		setResult(RESULT_OK);
		finish();
	}

	private void updateViews() {
		String username = loginEdit.getText().toString();
		String password = passwordEdit.getText().toString();

		final boolean isValidUsername = username.trim().length() > 3;
		final boolean isValidPassword = password.trim().length() > 3;

		loginButton.setEnabled(isValidPassword && isValidUsername);

		phoneView.setImageResource(loginEdit.hasFocus() ? R.drawable.phone_active : R.drawable.phone);

		lockView.setImageResource(passwordEdit.hasFocus() ? R.drawable.pass_active : R.drawable.pass);
	}

	private void setUpViews() {
		Aibolit.doInjections(this);

		loginEdit.setText(prefs.getString(Preferences.PREF_LAST_LOGIN, ""));
		loginEdit.addTextChangedListener(this);
		loginEdit.setOnFocusChangeListener(this);

		passwordEdit.setText(prefs.getString(Preferences.PREF_LAST_PASSWORD, ""));
		passwordEdit.addTextChangedListener(this);
		passwordEdit.setOnFocusChangeListener(this);
	}

	private class LoginTask extends LongAction<Void, Pair<String, Long>> {
		private final String username;
		private final String password;

		public LoginTask(Context context, String username, String password) {
			super(context);

			this.username = username;
			this.password = password;

			wrapWithProgress(context.getString(R.string.login_progress_message), true);
		}

		@Override
		public Pair<String, Long> doInBackgroundOrThrow(Void unused) throws Exception {
			try {
				Log.trace(TAG, "Logging in");
				session.login(username, password);
				Pair<String, Long> result = Pair.create(session.getContext().getAccessToken(), session.getContext().getUserId());
				Log.message(TAG, "Login success");
				return result;
			} catch (Exception e) {
				Log.exception(TAG, "Error during login", e);
				throw e;
			}
		}

		@Override
		public void displayError(Throwable error) {
			if (isCancelled()) return;

			ErrorHandlingUtilities.displayErrorSoftly(getContext(), error, ERROR_ANALYZER);
		}

		@Override
		protected void onSuccess(Pair<String, Long> result) {
			switchToNextActivity();
		}
	}

	private static final GenericErrorAnalyzer ERROR_ANALYZER = new GenericErrorAnalyzer() {
		@Override
		public int getMessageResId(Throwable error) {
			if (isNetworkIssue(error)) {
				return super.getMessageResId(error);
			} else {
				return R.string.error_bad_email_or_password;
			}
		}
	};
}
