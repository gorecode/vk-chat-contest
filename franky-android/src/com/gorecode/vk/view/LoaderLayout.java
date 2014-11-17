package com.gorecode.vk.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.gorecode.vk.R;


/**
 * A better replacement for AsyncTaskView.
 *
 *
 * XML Attributes:
 *
 * @franky:id/load_view_resource (reference) layout id of view that will be inflated for load state, default value is loading_item.
 * @franky:id/error_view_resource (reference) layout id for view that will be inflated for error state, default value is error_abstract_view.
 *
 * Expected behaviour:
 * If, for example, found a child view with load_view id and load_view_resource attr is specified, than load_view_resource attribute must be ignored.
 * same rule for error_view & error_view_resource.
 *
 * If view with content_view id not found, but also there is a child of LoaderLayout with id that not equals to
 * { load_view, error_view } then, this view must be a content view.
 */
public class LoaderLayout extends RelativeLayout {
	public static final int HIDE_METHOD_SET_GONE = 0x0;
	public static final int HIDE_METHOD_SET_INVISIBLE = 0x1;

	private static final int DEFAULT_LOAD_VIEW_RESOURCE = R.layout.view_loading;
	private static final int DEFAULT_ERROR_VIEW_RESOURCE = R.layout.view_error;
	
	private LayoutInflater inflater;
    
	private ViewGroup contentView;
    private View loadView;
    private View errorView;

    private boolean isFinishInflateCalled;

    private int hideMethod = HIDE_METHOD_SET_GONE;

	public LoaderLayout(Context context, AttributeSet attrs) {
    	super(context, attrs);
    	
    	inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	
    	int errorViewResource = DEFAULT_ERROR_VIEW_RESOURCE;
    	int loadViewResource = DEFAULT_LOAD_VIEW_RESOURCE;

    	if (attrs != null) {
    		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LoaderLayout);
    		loadViewResource = a.getResourceId(R.styleable.LoaderLayout_loadView, DEFAULT_LOAD_VIEW_RESOURCE);
    		errorViewResource = a.getResourceId(R.styleable.LoaderLayout_errorView, DEFAULT_ERROR_VIEW_RESOURCE);
    		a.recycle();
    	}

       	errorView = inflateInnerView(errorViewResource, R.id.error_view);
       	loadView = inflateInnerView(loadViewResource, R.id.load_view);

    	contentView = new FrameLayout(context);
    	contentView.setId(R.id.content_view);
    	contentView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

    	addView(contentView);
    }
	
    public LoaderLayout(Context context) {
       	this(context, null); 
    }

	@Override
	public void addView(View view, ViewGroup.LayoutParams params) {
		if (contentView != null && !isFinishInflateCalled) {
			contentView.addView(view, params);
		} else {
			super.addView(view, params);
		}
	}

    public int getHideMethod() {
    	return hideMethod;
    }

    public void setHideMethod(int hideMethod) {
    	this.hideMethod = hideMethod;
    }

    public boolean isLoadViewVisible() {
		return loadView.getVisibility() == VISIBLE;
    }
    
    public boolean isErrorViewVisible() {
		return errorView.getVisibility() == VISIBLE;
    }
    
    public void displayLoadView() {
    	hideAllExcept(loadView);
    }
    
    public void displayErrorView() {
    	hideAllExcept(errorView);
    }
    
    public void displayContent() {
    	for (int i = 0; i < getChildCount(); i++) {
    		View child = getChildAt(i);

    		if (child == errorView || child == loadView) {
    			hideView(child);
    		} else {
    			showView(child);
    		}
    	}
    }

    public View getErrorView() {
    	return errorView;
    }
    
    public View getLoadView() {
    	return loadView;
    }

    @Override
	protected void onFinishInflate() {
    	super.onFinishInflate();

    	isFinishInflateCalled = true;

    	addView(errorView);

    	addView(loadView);
    }

    private void hideAllExcept(View view) {
    	for (int i = 0; i < getChildCount(); i++) {
    		View child = getChildAt(i);

    		if (child == view) {
    			showView(child);
    		} else {
    			hideView(child);
    		}
    	}
    }

    private void showView(View view) {
    	view.setVisibility(VISIBLE);
    }

    private void hideView(View view) {
    	if (hideMethod == HIDE_METHOD_SET_GONE) {
    		view.setVisibility(View.GONE);
    	} else if (hideMethod == HIDE_METHOD_SET_INVISIBLE) {
    		view.setVisibility(View.INVISIBLE);
    	}
    }

    private View inflateInnerView(int resource, int viewId) {
    	View view = inflater.inflate(resource, null);
    	view.setId(viewId);
    	view.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    	view.setVisibility(View.GONE);
    	return view;
    }
}