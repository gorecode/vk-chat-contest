package com.danikula.aibolit.injector;

import java.lang.reflect.Field;

import roboguice.activity.RoboAccountAuthenticatorActivity;
import roboguice.activity.RoboActivity;
import roboguice.activity.RoboActivityGroup;
import roboguice.activity.RoboListActivity;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.danikula.aibolit.InjectingException;
import com.danikula.aibolit.InjectionContext;

class GuiceViewInjector extends AbstractFieldInjector<roboguice.inject.InjectView>{
	@Override
	public void doInjection(Object fieldOwner, InjectionContext injectionContext, Field field, roboguice.inject.InjectView annotation) {
		int viewId = annotation.value();

		if (isGuiceContext(injectionContext.getAndroidContext())) {
			// Field will be injected by RoboGuice, skip injection.
			return;
		}

		View view = getViewById(injectionContext.getRootView(), viewId);
		if (view == null) {
			String errorPattern = "View with id 0x%s for field named '%s' with type %s not found";
			throw new InjectingException(String.format(errorPattern, Integer.toHexString(viewId), field.getName(),
					field.getType()));
		}
		checkIsFieldAssignable(field, field.getType(), view.getClass());
		setValue(fieldOwner, field, view);
	}

	private boolean isGuiceContext(Context context) {
		if (context instanceof Activity) {
			Activity activity = (Activity)context;

			if (activity instanceof RoboAccountAuthenticatorActivity) return true;
			if (activity instanceof RoboActivity) return true;
			if (activity instanceof RoboListActivity) return true;
			if (activity instanceof RoboActivityGroup) return true;
		}

		return false;
	}
}
