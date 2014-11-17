package com.gorecode.vk.activity.chat;

import java.util.concurrent.Executor;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;

import com.google.common.base.Preconditions;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.data.Dialog;
import com.gorecode.vk.data.TypingNotification;
import com.uva.log.Log;
import com.uva.log.Message;

public class TypingNotificationEmitter implements TextWatcher {
	private static final String TAG = "TypingNotificationEmitter";

	private static final long DEFAULT_COMPOSE_TIMEOUT = 2500;
	private static final long DISPATCH_THRESOLD = 4500;

	private final VkModel mModel;
	private final Handler mHandler;
	private final Executor mRequestDispatchExecutor;
	private final Dialog mDialog;

	private long mComposeTimeout = DEFAULT_COMPOSE_TIMEOUT;

	private long mSendTypingNotificationLastAttemptTime;

	public TypingNotificationEmitter(Dialog dialog, VkModel model, Executor requestDispatchExecutor) {
		Preconditions.checkNotNull(dialog);
		Preconditions.checkNotNull(model);
		Preconditions.checkNotNull(requestDispatchExecutor);

		mDialog = dialog;
		mModel = model;
		mRequestDispatchExecutor = requestDispatchExecutor;

		mHandler = new Handler(Looper.getMainLooper());
	}

	public void setComposeTimeout(long composeTimeout) {
		Preconditions.checkArgument(composeTimeout > 0);

		mComposeTimeout = composeTimeout;
	}

	public long getComposeTimeout() {
		return DEFAULT_COMPOSE_TIMEOUT;
	}

	@Override
	public void afterTextChanged(Editable s) {
		;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		;
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		final boolean isErasing = before > count;
		final boolean isStopped = s.length() == 0;

		cancelPendingPause();

		if (isStopped) {
			sendTypingNotificationAsync(TypingNotification.STOPPED);
		} else {
			pauseLater();

			if (isErasing) {
				sendTypingNotificationAsync(TypingNotification.ERASING);
			} else {
				sendTypingNotificationAsync(TypingNotification.TYPING);
			}
		}
	}

	private void sendTypingNotificationAsync(final TypingNotification notification) {
		if (notification != TypingNotification.TYPING) return;

		if (System.currentTimeMillis() - mSendTypingNotificationLastAttemptTime < DISPATCH_THRESOLD) return;

		mSendTypingNotificationLastAttemptTime = System.currentTimeMillis();

		mRequestDispatchExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					if (mDialog.isConference()) {
						mModel.sendTypingNotification(mDialog.getCid());
					} else {
						mModel.sendTypingNotification();
					}
				} catch (Exception e) {
					Log.exception(TAG, Message.WARNING, "Unable to send typing notification", e);
				}
			}
		});
	}

	private void pauseLater() {
		mHandler.postDelayed(mPauseRunnable, mComposeTimeout);
	}

	private void cancelPendingPause() {
		mHandler.removeCallbacks(mPauseRunnable);
	}

	private final Runnable mPauseRunnable = new Runnable() {
		@Override
		public void run() {
			sendTypingNotificationAsync(TypingNotification.PAUSED);
		}
	};
}
