package com.gorecode.vk.task;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import com.gorecode.vk.R;
import com.google.common.base.Preconditions;
import com.gorecode.vk.activity.FragmentCallbacks;
import com.gorecode.vk.activity.VkActivityContract;
import com.gorecode.vk.activity.VkFragment;
import com.gorecode.vk.utilities.DialogUtilities;
import com.gorecode.vk.utilities.ErrorHandlingUtilities;
import com.gorecode.vk.view.VkActionBar;
import com.gorecode.vk.view.LoaderLayout;
import com.uva.log.Log;

public abstract class LongAction<Params, Result> implements UiBlocker, ErrorDisplayer, FragmentCallbacks {
	private final String TAG = getClass().getSimpleName();

	private UiBlocker uiBlocker = this;
	private ErrorDisplayer errorDisplayer = this;

	private AsyncTask<Params, Void, LongActionContext<Params, Result>> task = new Task();

	private boolean uiIsBlocked;
	private boolean isAborted;
	private boolean shouldDisplayProgressInActionBar = true;

	private final Context context;

	private VkFragment parentFragment;

	private LoaderLayout attachedLoaderLayout;
	private VkActionBar attachedActionBar;

	public LongAction(Context context) {
		this(context, null);
	}

	public LongAction(Context context, VkFragment parentFragment) {
		this.context = context;
		this.parentFragment = parentFragment;

		if (context instanceof VkActivityContract) {
			VkActivityContract activity = (VkActivityContract)context;

			activity.getAutoCancelPool().add(this);

			attachedActionBar = activity.getVkActionBar(); 
		}
	}

	public Future<Result> asFuture() {
		return new Future<Result>() {
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return LongAction.this.cancel(mayInterruptIfRunning);
			}

			@Override
			public Result get() throws InterruptedException, ExecutionException {
				return toFutureResult(LongAction.this.task.get());
			}

			@Override
			public Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
				return toFutureResult(LongAction.this.task.get(timeout, unit));
			}

			@Override
			public boolean isCancelled() {
				return LongAction.this.isCancelled();
			}

			@Override
			public boolean isDone() {
				return task.getStatus() == AsyncTask.Status.FINISHED;
			}

			private Result toFutureResult(LongActionContext<Params, Result> result) throws ExecutionException {
				if (result.isCompletedSuccessfuly()) {
					return result.result;
				} else {
					throw new ExecutionException(result.error);
				}				
			}
		};
	}

	public void setParentFragment(VkFragment fragment) {
		parentFragment = fragment;
	}

	public void displayProgressInActionBar(boolean enabled) {
		shouldDisplayProgressInActionBar = enabled;
	}

	@Override
	public void blockUi() {
		;
	}

	@Override
	public void unblockUi() {
		;
	}

	public void execute() {
		execute(null);
	}

	@SuppressWarnings("unchecked")
	public void execute(Params arg) {
		task.execute(arg);
	}

	@Override
	public void displayError(Throwable cause) {
		Log.exception(TAG, cause);

		ErrorHandlingUtilities.displayErrorSoftly(context, cause);
	}

	public void setUiBlocker(UiBlocker blocker) {
		this.uiBlocker = blocker;
	}

	public void setErrorDisplayer(ErrorDisplayer errorDisplayer) {
		Preconditions.checkNotNull(errorDisplayer);

		this.errorDisplayer = errorDisplayer;
	}

	public void setErrorToastMessage(int errorMessageId) {
		setErrorToastMessage(getContext().getString(errorMessageId));
	}

	public void setErrorToastMessageAutofindEnabled() {
		setErrorDisplayer(new ErrorDisplayer() {
			@Override
			public void displayError(Throwable error) {
				ErrorHandlingUtilities.displayErrorSoftly(getContext(), error);
			}
		});
	}

	public void setErrorToastMessage(final String errorMessage) {
		if (errorMessage != null) {
			setErrorDisplayer(new ErrorDisplayer() {
				@Override
				public void displayError(Throwable error) {
					ErrorHandlingUtilities.displayErrorSoftly(getContext(), errorMessage, error);
				}
			});
		} else {
			setErrorToastMessageAutofindEnabled();
		}
	}

	public boolean isAborted() {
		return isAborted;
	}

	public boolean isIdle() {
		return getStatus() == AsyncTask.Status.PENDING && !isCancelled();
	}

	public boolean isFinished() {
		return getStatus() == AsyncTask.Status.FINISHED && !isCancelled();
	}

	public boolean isRunning() {
		return getStatus() == AsyncTask.Status.RUNNING && !isCancelled();
	}

	public AsyncTask.Status getStatus() {
		return task.getStatus();
	}

	public void wrapWithBlockedViews(final View... views) {
		setUiBlocker(new UiBlocker() {
			@Override
			public void unblockUi() {
				for (View view : views) {
					view.setEnabled(true);
				}
			}
			
			@Override
			public void blockUi() {
				for (View view : views) {
					view.setEnabled(false);
				}
			}
		});
	}

	public void wrapWithSpinner(final LoaderLayout loaderLayout) {
		this.attachedLoaderLayout = loaderLayout;

		setUiBlocker(new UiBlocker() {
			@Override
			public void blockUi() {
				loaderLayout.displayLoadView();
			}

			@Override
			public void unblockUi() {
				loaderLayout.displayContent();
			}
		});
	}

	public void wrapWithProgress(boolean cancelable) {
		wrapWithProgress(context.getString(R.string.loading_text), cancelable);
	}

	public void wrapWithProgress(int progressMessageId, boolean cancelable) {
		wrapWithProgress(getContext().getString(progressMessageId), cancelable);
	}

	public void wrapWithProgress(final String progressMessage, boolean cancelable) {
		ProgressDialog dialog = new ProgressDialog(context);
		dialog.setMessage(progressMessage);
		dialog.setCancelable(cancelable);
		dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				cancel(true);
			}
		});

		wrapWithDialog(dialog);
	}

	public void wrapWithDialog(final Dialog progress) {
		progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				dialog.dismiss();

				cancel(true);
			}
		});

		setUiBlocker(new UiBlocker() {
			@Override
			public void blockUi() {
				progress.show();
			}

			@Override
			public void unblockUi() {
				DialogUtilities.dismissSafely(progress);
			}
		});
	}

	public boolean isCancelled() {
		return task.isCancelled();
	}

	public boolean abort() {
		Log.trace(getClass().getSimpleName(), "abort()");
	
		isAborted = true;

		return cancel(true);
	}

	public boolean cancel(boolean mayInterruptIfRunning) {
		if (task.isCancelled()) return false;

		return task.cancel(mayInterruptIfRunning);
	}

	public Context getContext() {
		return context;
	}

	protected void onPreExecute() {
		;
	}

	protected void onComplete(LongActionContext<Params, Result> result) {
		;
	}

	protected void onError(Exception error) {
		if (isAborted()) return;

		errorDisplayer.displayError(error);
	}

	protected void onSuccess(Result result) {
		;
	}

	protected boolean shouldKeepArgs() {
		return true;
	}

	abstract protected Result doInBackgroundOrThrow(Params params) throws Exception;

	private void unblockUiIfBlocked() {
		if (uiIsBlocked) {
			uiBlocker.unblockUi();
			uiIsBlocked = false;
		}
	}

	private class Task extends AsyncTask<Params, Void, LongActionContext<Params, Result>> {
		@Override
		final protected void onPreExecute() {
			super.onPreExecute();

			LongAction.this.onPreExecute();

			if (isCancelled()) {
				return;
			}

			if (parentFragment != null) {
				parentFragment.registerCallbacks(LongAction.this);
			}

			if (shouldDisplayProgressInActionBar && attachedActionBar != null) {
				attachedActionBar.incrementIntermediateTasksCount();
			}

			uiBlocker.blockUi();
			uiIsBlocked = true;
		}

		@Override
		final protected LongActionContext<Params, Result> doInBackground(Params... params) {
			LongActionContext<Params, Result> executionContext = new LongActionContext<Params, Result>();

			Params args = params.length == 0 ? null : params[0];

			if (shouldKeepArgs()) {
				executionContext.input = args;
			}

			try {
				executionContext.result = doInBackgroundOrThrow(args);
			} catch (Exception e) {
				executionContext.error = e;
			}

			return executionContext;
		}

		@Override
		final protected void onPostExecute(LongActionContext<Params, Result> executionContext) {
			completed(executionContext);
		}

		@Override
		final protected void onCancelled() {
			super.onCancelled();

			completed(null);
		}

		private void completed(LongActionContext<Params, Result> executionContext) {
			if (parentFragment != null) {
				parentFragment.unregisterCallbacks(LongAction.this);
			}

			if (shouldDisplayProgressInActionBar && attachedActionBar != null) {
				attachedActionBar.decrementIntermediateTasksCount();
			}

			if (isAborted) return;

			unblockUiIfBlocked();
 
			if (isCancelled()) return;

			if (executionContext.error != null) {
				if (attachedLoaderLayout != null) {
					attachedLoaderLayout.displayErrorView();
				}

				onError(executionContext.error);
			} else {
				onSuccess(executionContext.result);
			}

			onComplete(executionContext);
		}
	}

	@Override
	public void onFragmentCreated(Fragment fragment, Bundle savedInstanceState) {
		;
	}

	@Override
	public void onFragmentViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
		;
	}

	@Override
	public void onFragmentViewDestroyed(Fragment fragment) {
		if (fragment == parentFragment) {
			abort();
		}
	}

	@Override
	public void onFragmentDestroyed(Fragment fragment) {
		;
	}
}
