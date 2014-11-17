/*
 * TouchImageView.java
 * By: Michael Ortiz
 * Updated By: Patrick Lackemacher
 * -------------------
 * Extends Android ImageView to include pinch zooming and panning.
 */

package com.gorecode.vk.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;

import com.uva.log.Log;

public class TouchImageView extends ImageView {
	// We can be in one of these 3 states
	public static final int MODE_NONE = 0;
	public static final int MODE_DRAG = 1;
	public static final int MODE_ZOOM = 2;

	private static final int TOUCH_SLOT = ViewConfiguration.getTouchSlop();

	private static final PointF MOVEMENT_TO_LEFT_DELTA = new PointF(10, 0);
	private static final PointF MOVEMENT_TO_RIGHT_DELTA = new PointF(-10, 0);

	private static final float MIN_PAN_TRANSLATION = 0.5f;

	private static final String TAG = TouchImageView.class.getName();

	private static boolean DEBUG = false;

	Matrix matrix = new Matrix();

	int mode = MODE_NONE;

	// Remember some things for zooming
	PointF last;
	PointF start;
	float minScale = 1f;
	float maxScale = 4f;
	float[] m = new float[9];

	float redundantXSpace, redundantYSpace;

	float width, height;
	float saveScale;
	float right, bottom, origWidth, origHeight, bmWidth, bmHeight;

	ScaleGestureDetector scaleDetector;

	Context context;

	public TouchImageView(Context context) {
		super(context);
		sharedConstructing(context);
	}

	public int getMode() {
		return mode;
	}

	public TouchImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		sharedConstructing(context);
	}

	public boolean canScrollToLeft() {
		float dx = getPanTranslationDelta(MOVEMENT_TO_LEFT_DELTA).x;

		return dx >= MIN_PAN_TRANSLATION;
	}

	public boolean canScrollToRight() {
		float dx = getPanTranslationDelta(MOVEMENT_TO_RIGHT_DELTA).x;

		return (dx < 0) && (Math.abs(dx) >= MIN_PAN_TRANSLATION);
	}

	public void resetZoomAndPan() {
		resetZoomAndPan(true);
	}

	private void resetZoomAndPan(boolean requestLayout) {
		matrix.reset();
		matrix.setTranslate(0f, 0f);
		matrix.setScale(1.0f, 1.0f);
		mode = MODE_NONE;
		saveScale = 1.0f;
		start = new PointF();
		last = new PointF();

		setImageMatrix(matrix);

		if (requestLayout) {
			invalidate();

			requestLayout();
		}
	}

	private void sharedConstructing(Context context) {
		super.setClickable(true);

		this.context = context;
		scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		resetZoomAndPan(false);
		setImageMatrix(matrix);
		setScaleType(ScaleType.MATRIX);

		setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				scaleDetector.onTouchEvent(event);

				matrix.getValues(m);

				PointF curr = new PointF(event.getX(), event.getY());

				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					last.set(event.getX(), event.getY());
					start.set(last);
					mode = MODE_DRAG;
					break;
				case MotionEvent.ACTION_MOVE:
					if (mode == MODE_DRAG) {
						float deltaX = curr.x - last.x;
						float deltaY = curr.y - last.y;
						PointF tranlation = getPanTranslationDelta(new PointF(deltaX, deltaY));
						matrix.postTranslate(tranlation.x, tranlation.y);
						last.set(curr.x, curr.y);

						if (DEBUG) {
							DEBUG = false;
							Log.trace(TAG, String.format("canMoveToLeft = %b", canScrollToLeft()));
							Log.trace(TAG, String.format("canMoveToRight = %b", canScrollToRight()));
							DEBUG = true;
						}
					}
					break;

				case MotionEvent.ACTION_UP:
					mode = MODE_NONE;
					int xDiff = (int) Math.abs(curr.x - start.x);
					int yDiff = (int) Math.abs(curr.y - start.y);
					if (xDiff < TOUCH_SLOT && yDiff < TOUCH_SLOT)
						performClick();
					break;

				case MotionEvent.ACTION_POINTER_UP:
					mode = MODE_NONE;
					break;
				}
				setImageMatrix(matrix);
				invalidate();
				return true; // indicate event was handled
			}

		});
	}

	@Override
	public void setImageBitmap(Bitmap bm) { 
		super.setImageBitmap(bm);
		if(bm != null) {
			bmWidth = bm.getWidth();
			bmHeight = bm.getHeight();
		}
	}

	public void setMaxZoom(float x) {
		maxScale = x;
	}

	private PointF getPanTranslationDelta(PointF deltaInViewSpace) {
		matrix.getValues(m);

		float x = m[Matrix.MTRANS_X];
		float y = m[Matrix.MTRANS_Y];

		float scaleWidth = Math.round(origWidth * saveScale);
		float scaleHeight = Math.round(origHeight * saveScale);

		float deltaX = deltaInViewSpace.x;
		float deltaY = deltaInViewSpace.y;

		if (DEBUG) {
			Log.trace(TAG, String.format("delta (before) = (%f, %f)", deltaX, deltaY));
			Log.trace(TAG, String.format("matrix[x,y] = (%f, %f)", x, y));
			Log.trace(TAG, String.format("scaled size = (%f, %f)", scaleWidth, scaleHeight));
			Log.trace(TAG, String.format("right = %f", right));
			Log.trace(TAG, String.format("bottom = %f", bottom));
			Log.trace(TAG, String.format("width = %f", width));
			Log.trace(TAG, String.format("height = %f", height));
		}

		if (scaleWidth < width) {
			deltaX = 0;
			if (y + deltaY > 0)
				deltaY = -y;
			else if (y + deltaY < -bottom)
				deltaY = -(y + bottom); 
		} else if (scaleHeight < height) {
			deltaY = 0;
			if (x + deltaX > 0)
				deltaX = -x;
			else if (x + deltaX < -right)
				deltaX = -(x + right);
		} else {
			if (x + deltaX > 0)
				deltaX = -x;
			else if (x + deltaX < -right)
				deltaX = -(x + right);

			if (y + deltaY > 0)
				deltaY = -y;
			else if (y + deltaY < -bottom)
				deltaY = -(y + bottom);
		}

		if (DEBUG) {
			Log.trace(TAG, String.format("delta (after) = (%f, %f)", deltaX, deltaY));
			Log.trace(TAG, "-------------------------------------------");
		}

		return new PointF(deltaX, deltaY);
	}

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			mode = MODE_ZOOM;
			return true;
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float mScaleFactor = (float)Math.min(Math.max(.8f, detector.getScaleFactor()), 1.2);
			float origScale = saveScale;
			saveScale *= mScaleFactor;
			if (saveScale > maxScale) {
				saveScale = maxScale;
				mScaleFactor = maxScale / origScale;
			} else if (saveScale < minScale) {
				saveScale = minScale;
				mScaleFactor = minScale / origScale;
			}
			right = width * saveScale - width - (2 * redundantXSpace * saveScale);
			bottom = height * saveScale - height - (2 * redundantYSpace * saveScale);
			if (origWidth * saveScale <= width || origHeight * saveScale <= height) {
				matrix.postScale(mScaleFactor, mScaleFactor, width / 2, height / 2);
				if (mScaleFactor < 1) {
					matrix.getValues(m);
					float x = m[Matrix.MTRANS_X];
					float y = m[Matrix.MTRANS_Y];
					if (mScaleFactor < 1) {
						if (Math.round(origWidth * saveScale) < width) {
							if (y < -bottom)
								matrix.postTranslate(0, -(y + bottom));
							else if (y > 0)
								matrix.postTranslate(0, -y);
						} else {
							if (x < -right) 
								matrix.postTranslate(-(x + right), 0);
							else if (x > 0) 
								matrix.postTranslate(-x, 0);
						}
					}
				}
			} else {
				matrix.postScale(mScaleFactor, mScaleFactor, detector.getFocusX(), detector.getFocusY());
				matrix.getValues(m);
				float x = m[Matrix.MTRANS_X];
				float y = m[Matrix.MTRANS_Y];
				if (mScaleFactor < 1) {
					if (x < -right) 
						matrix.postTranslate(-(x + right), 0);
					else if (x > 0) 
						matrix.postTranslate(-x, 0);
					if (y < -bottom)
						matrix.postTranslate(0, -(y + bottom));
					else if (y > 0)
						matrix.postTranslate(0, -y);
				}
			}
			return true;

		}
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		width = MeasureSpec.getSize(widthMeasureSpec);
		height = MeasureSpec.getSize(heightMeasureSpec);
		//Fit to screen.
		float scale;
		float scaleX =  (float)width / (float)bmWidth;
		float scaleY = (float)height / (float)bmHeight;
		scale = Math.min(scaleX, scaleY);
		matrix.setScale(scale, scale);
		setImageMatrix(matrix);
		saveScale = 1f;

		// Center the image
		redundantYSpace = (float)height - (scale * (float)bmHeight) ;
		redundantXSpace = (float)width - (scale * (float)bmWidth);
		redundantYSpace /= (float)2;
		redundantXSpace /= (float)2;

		matrix.postTranslate(redundantXSpace, redundantYSpace);

		origWidth = width - 2 * redundantXSpace;
		origHeight = height - 2 * redundantYSpace;
		right = width * saveScale - width - (2 * redundantXSpace * saveScale);
		bottom = height * saveScale - height - (2 * redundantYSpace * saveScale);
		setImageMatrix(matrix);
	}
}