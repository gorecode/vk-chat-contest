package com.gorecode.vk.activity;

import java.util.Hashtable;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.danikula.aibolit.Aibolit;
import com.danikula.aibolit.annotation.InjectOnClickListener;
import com.gorecode.vk.R;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.task.LongAction;
import com.gorecode.vk.utilities.ErrorAnalyzer;
import com.gorecode.vk.utilities.ErrorHandlingUtilities;
import com.gorecode.vk.utilities.GenericErrorAnalyzer;
import com.gorecode.vk.utilities.Toasts;
import com.perm.kate.api.KException;
import com.uva.log.Log;

@ContentView(R.layout.registration_activity)
public class RegistrationActivity extends VkActivity implements TextWatcher {
	private static final String TAG = RegistrationActivity.class.getSimpleName();

	private static final boolean DEBUG = false;

	public static final String EXTRA_LOGIN = "login";
	public static final String EXTRA_PASSWORD = "password";

	@InjectView(R.id.registrationPage)
	private View mRegistrationPage;
	@InjectView(R.id.confirmationPage)
	private View mConfirmationPage;

	@InjectView(R.id.phone)
	private EditText mPhoneView;
	@InjectView(R.id.phoneErrorImage)
	private ImageView mPhoneErrorView;
	@InjectView(R.id.firstName)
	private EditText mFirstNameView;
	@InjectView(R.id.firstNameErrorImage)
	private ImageView mFirstNameErrorView;
	@InjectView(R.id.lastName)
	private EditText mLastNameView;
	@InjectView(R.id.lastNameErrorImage)
	private ImageView mLastNameErrorView;
	@InjectView(R.id.registration_button)
	private Button mRegisterButton;
	@InjectView(R.id.confirmationCode)
	private EditText mConfirmationCodeView;
	@InjectView(R.id.confirmationCodeImage)
	private ImageView mConfirmationCodeErrorView;
	@InjectView(R.id.password)
	private EditText mPasswordView;
	@InjectView(R.id.passwordImage)
	private ImageView mPasswordErrorView;
	@InjectView(R.id.complete_registration_button)
	private Button mCompleteRegistrationButton;
	@InjectView(R.id.resend_confirmation_code)
	private Button mResendConfirmationCodeButton;

	@Inject
	private VkModel mVk;

	private final ErrorAnalyzer mErrorAnalyzer = new ErrorsAnalyzer();

	private String mPhone;
	private String mFirstName;
	private String mLastName;

	private String mConfirmationCode;
	private String mPassword;

	private String mSid;

	private Exception mLastError;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setAnimations(ANIMATIONS_SLIDE_BOTTOM);

		setResult(RESULT_CANCELED);

		setUpViews();

		updateViews();
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		updateViews();
	}

	@InjectOnClickListener(R.id.registration_button)
	public void onRegisterButtonClicked(View v) {
		updateModel();

		final String phone = mPhone; 
		final String firstName = mFirstName;
		final String lastName = mLastName;

		LongAction<?, ?> action = new LongAction<Void, String>(this) {
			@Override
			protected String doInBackgroundOrThrow(Void params) throws Exception {
				try {
					return mVk.signup(phone, firstName, lastName, DEBUG);
				} catch (Exception e) {
					Log.exception(TAG, "Error sending registration data", e);

					throw e;
				}
			}

			@Override
			public void displayError(Throwable error) {
				;
			}

			@Override
			public void onError(Exception error) {
				mLastError = error;

				handleError(error);
			}

			@Override
			public void onSuccess(String sid) {
				mLastError = null;

				mSid = sid;

				Toasts.makeText(RegistrationActivity.this, R.string.registration_toast_wait_for_sms).show();

				updateViews();
			}
		};
		action.wrapWithProgress(false);
		action.execute();
	}

	@InjectOnClickListener(R.id.resend_confirmation_code)
	public void onResendConfirmationCodeButtonClicked(View v) {
		updateModel();

		LongAction<?, ?> action = new LongAction<Void, String>(this) {
			@Override
			protected String doInBackgroundOrThrow(Void params) throws Exception {
				try {
					return mVk.signup(mPhone, mFirstName, mLastName, DEBUG, mSid);
				} catch (Exception e) {
					Log.exception(TAG, "Error resending registration data", e);

					throw e;
				}
			}

			@Override
			public void displayError(Throwable error) {
				ErrorHandlingUtilities.displayErrorSoftly(RegistrationActivity.this, error, mErrorAnalyzer);
			}

			@Override
			public void onSuccess(String sid) {
				Toasts.makeText(RegistrationActivity.this, R.string.registration_toast_wait_for_sms).show();

				updateViews();
			}
		};
		action.wrapWithBlockedViews(mResendConfirmationCodeButton);
		action.execute();
	}

	@InjectOnClickListener(R.id.complete_registration_button)
	public void onConfirmRegistrationButtonClicked(View v) {
		updateModel();

		final String code = mConfirmationCode;
		final String password = mPassword;

		LongAction<?, ?> action = new LongAction<Void, Long>(this) {
			@Override
			protected Long doInBackgroundOrThrow(Void params) throws Exception {
				try {
					return mVk.confirmSignup(mPhone, code, password, DEBUG);
				} catch (Exception e) {
					Log.exception(TAG, "Error confirming registration", e);
					throw e;
				}
			}

			@Override
			public void displayError(Throwable error) {
				;
			}

			@Override
			public void onError(Exception error) {
				mLastError = error;

				handleError(error);
			}

			@Override
			public void onSuccess(Long uid) {
				Intent data = new Intent();
				data.putExtra(EXTRA_LOGIN, mPhone);
				data.putExtra(EXTRA_PASSWORD, mPassword);
				setResult(RESULT_OK, data);
				finish();
			}
		};
		action.wrapWithProgress(false);
		action.execute();
	}

	@InjectOnClickListener(R.id.phoneErrorImage)
	public void onPhoneErrorViewClicked(View v) {
		handleError(mLastError);
	}

	@InjectOnClickListener(R.id.confirmationCodeImage)
	public void onConfirmationCodeErrorViewClicked(View v) {
		handleError(mLastError);
	}

	private void updateModel() {
		mPhone = mPhoneView.getText().toString();
		mFirstName = mFirstNameView.getText().toString().trim();
		mLastName = mLastNameView.getText().toString().trim();
		mConfirmationCode = mConfirmationCodeView.getText().toString();
		mPassword = mPasswordView.getText().toString();
	}

	private int getLastErrorCode() {
		if (mLastError instanceof KException) {
			return ((KException)mLastError).error_code;
		}
		return -1;
	}

	private void handleError(Exception error) {
		ErrorHandlingUtilities.displayErrorSoftly(RegistrationActivity.this, error, mErrorAnalyzer);

		updateViews();

		if (shouldShowPhoneErrorView()) {
			mPhoneView.requestFocus();
		}
		if (shouldShowConfirmationCodeErrorView()) {
			mConfirmationCodeView.requestFocus();
		}
	}

	private boolean shouldShowConfirmationCodeErrorView() {
		if (mSid == null) return false;

		int code = getLastErrorCode();

		return (code == KException.ERROR_CODE_INVALID_PARAMETERS || code == KException.ERROR_CODE_INVALID_CONFIRMATION_CODE);
	}

	private boolean shouldShowPhoneErrorView() {
		if (mSid != null) return false;

		int code = getLastErrorCode();

		return code == KException.ERROR_CODE_INVALID_PARAMETERS || code == KException.ERROR_CODE_PHONE_IS_TAKEN;
	}

	private void updateViews() {
		updateModel();

		if (mSid == null) {
			mFirstNameErrorView.setVisibility(isEmpty(mFirstName) ? View.GONE : View.VISIBLE);
			mFirstNameErrorView.setImageResource(isCorrectName(mFirstName) ? R.drawable.ok : R.drawable.error);

			mLastNameErrorView.setVisibility(isEmpty(mLastName) ? View.GONE : View.VISIBLE);
			mLastNameErrorView.setImageResource(isCorrectName(mLastName) ? R.drawable.ok : R.drawable.error);

			mPhoneErrorView.setVisibility(isEmpty(mPhone) ? View.GONE : View.VISIBLE);
			mPhoneErrorView.setImageResource(isEmpty(mPhone) || shouldShowPhoneErrorView() ? R.drawable.error : R.drawable.ok);

			mRegisterButton.setEnabled(mPhone.length() > 0 && isCorrectName(mFirstName) && isCorrectName(mLastName));
		}

		if (mSid != null) {
			mConfirmationCodeErrorView.setVisibility(Strings.isNullOrEmpty(mConfirmationCode) ? View.GONE : View.VISIBLE);
			mConfirmationCodeErrorView.setImageResource(!isValidConfirmationCode(mConfirmationCode) || shouldShowConfirmationCodeErrorView() ? R.drawable.error : R.drawable.ok);

			mPasswordErrorView.setVisibility(Strings.isNullOrEmpty(mPassword) ? View.GONE : View.VISIBLE);
			mPasswordErrorView.setImageResource(isValidPassword(mPassword) ? R.drawable.ok : R.drawable.error);

			mCompleteRegistrationButton.setEnabled(isValidConfirmationCode(mConfirmationCode) && isValidPassword(mPassword));
		}

		mRegistrationPage.setVisibility(mSid == null ? View.VISIBLE : View.GONE);
		mConfirmationPage.setVisibility(mSid != null ? View.VISIBLE : View.GONE);
	}

	private void setUpViews() {
		mPhoneView.addTextChangedListener(this);
		mPhoneErrorView.setClickable(true);
		mFirstNameView.addTextChangedListener(this);
		mLastNameView.addTextChangedListener(this);
		mConfirmationCodeView.addTextChangedListener(this);
		mPasswordView.addTextChangedListener(this);

		Aibolit.doInjections(this);
	}

	private static boolean isValidConfirmationCode(String code) {
		return !Strings.isNullOrEmpty(code);
	}

	private static boolean isValidPassword(String password) {
		if (Strings.isNullOrEmpty(password)) return false;

		final int MIN_PASSWORD_LENGTH = 6;

		return password.length() >= MIN_PASSWORD_LENGTH;
	}

	private static boolean isEmpty(String string) {
		return Strings.isNullOrEmpty(string) || Strings.isNullOrEmpty(string.trim());
	}

	private static final boolean isCorrectName(String name) {
		if (Strings.isNullOrEmpty(name)) return false;

		name = name.trim();

		final int MIN_NAME_LENGTH = 2;

		if (name.length() < MIN_NAME_LENGTH) {
			return false;
		}

		// TODO: Check for invalid symbols.

		return true;
	}

	/**
	 * Unused methods.
	 */

	@Override
	public void afterTextChanged(Editable s) {
		;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		;
	}

	private class ErrorsAnalyzer extends GenericErrorAnalyzer {
		private final Hashtable<Integer, Integer> ERROR_CODE_DESCRIPTIONS = new Hashtable<Integer, Integer>();

		public ErrorsAnalyzer() {
			ERROR_CODE_DESCRIPTIONS.put(KException.ERROR_CODE_PHONE_IS_TAKEN, R.string.error_phone_taken);
			ERROR_CODE_DESCRIPTIONS.put(KException.ERROR_CODE_REGISTRATION_IS_PENDING, R.string.error_registration_is_pending);
			ERROR_CODE_DESCRIPTIONS.put(KException.ERROR_CODE_INVALID_CONFIRMATION_CODE, R.string.error_registration_confirmation_invalid_input);
			ERROR_CODE_DESCRIPTIONS.put(KException.ERROR_CODE_TRY_AFTER_5_SECONDS, R.string.error_try_later);			
		}

		@Override
		public int getMessageResId(Throwable error) {
			if (error instanceof KException) {
				int code = ((KException)error).error_code;

				if (code == KException.ERROR_CODE_INVALID_PARAMETERS) {
					if (mSid == null) {
						return R.string.error_registration_invalid_input;
					} else {
						return R.string.error_registration_confirmation_invalid_input;
					}
				}

				Integer messageId = ERROR_CODE_DESCRIPTIONS.get(code);

				if (messageId != null) {
					return messageId;
				}
			}

			return super.getMessageResId(error);
		}
	}
}
