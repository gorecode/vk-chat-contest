package com.gorecode.vk.activity;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

import com.danikula.aibolit.Aibolit;
import com.danikula.aibolit.annotation.InjectOnClickListener;
import com.gorecode.vk.R;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.gorecode.vk.application.VkApplication;
import com.gorecode.vk.event.CaptchaEnterCompletedEvent;
import com.gorecode.vk.view.WebImageView;
import com.perm.kate.api.Api;
import com.perm.kate.api.Captcha;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

@ContentView(R.layout.captcha_activity)
public class CaptchaActivity extends VkActivity {
	private static final String EXTRA_CAPTCHA = "captcha";

	@InjectView(R.id.captcha_image)
	private WebImageView mCaptchaImageView;
	@InjectView(R.id.captcha_key)
	private EditText mCaptchaKeyEdit;

	@Inject
	private EventBus mBus;

	private Captcha mCaptcha;

	private String mCaptchaKey;

	public static Intent getDisplayIntent(Context context, Captcha captcha) {
		Intent intent = new Intent(context, CaptchaActivity.class);
		intent.putExtra(EXTRA_CAPTCHA, captcha);
		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setAnimations(ANIMATIONS_SLIDE_BOTTOM);

		mCaptcha = (Captcha)getIntent().getExtras().getSerializable(EXTRA_CAPTCHA);

		setUpViews();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (isFinishing()) {
			mBus.post(new CaptchaEnterCompletedEvent(mCaptcha, mCaptchaKey));
		}
	}

	@InjectOnClickListener(R.id.close_button)
	protected void onCloseButtonClick(View v) {
		finish();
	}

	@InjectOnClickListener(R.id.submit_captcha)
	protected void onSubmitCaptchaKeyButtonClick(View view) {
		mCaptchaKey = mCaptchaKeyEdit.getText().toString();

		finish();
	}

	private void setUpViews() {
		mCaptchaImageView.setImageUrl(mCaptcha.img);

		Aibolit.doInjections(this);
	}

	public static class CaptchaKeyProvider implements Api.CaptchaCallback {
		private final Context mContext;
		
		public CaptchaKeyProvider(Context context) {
			mContext = context;
		}

		@Override
		public String enterCaptchaKey(Captcha captcha) {
			final VkApplication application = VkApplication.from(mContext);

			CaptchaEnterCompletedEventWaiter awaiter = new CaptchaEnterCompletedEventWaiter();

			EventBus bus = application.getEventBus();

			bus.register(awaiter);

			final Intent intent = CaptchaActivity.getDisplayIntent(application, captcha);

			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_FROM_BACKGROUND);

			application.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					application.startActivity(intent);
				}
			});

			try {
				return awaiter.waitForEvent().getKey();
			} catch (InterruptedException e) {
				return null;
			} finally {
				bus.unregister(awaiter);
			}
		}

		private static class CaptchaEnterCompletedEventWaiter {
			private CaptchaEnterCompletedEvent mEvent;

			public CaptchaEnterCompletedEvent waitForEvent() throws InterruptedException {
				synchronized (this) {
					while (mEvent == null) wait();
				}

				return mEvent;
			}

			@SuppressWarnings("unused")
			@Subscribe
			public void onCaptchaResult(CaptchaEnterCompletedEvent event) {
				synchronized (this) {
					mEvent = event;

					notifyAll();				
				}
			}
		}
	}
}
