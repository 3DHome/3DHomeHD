/**
 * 
 */
package com.nammari.googleplus;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import com.funyoung.common.LowPerformanceUtils;
import com.funyoung.scrumptious.ResourceUtils.AnimationFactory;

/*
 * Copyright (C) 2013 Ahmed Nammari
 * 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * @author nammari
 * 
 */

class ScrollHandler implements OnGlobalLayoutListener, ScrollHandlerInterface {
    private static final String TAG = "ScrollHandler";

    private static final boolean LowPerformance = true;
    private void logEnter(String method) {
        if (LowPerformance) {
            LowPerformanceUtils.logEnter(TAG, method, null);
        }
    }
    private void logExit(String method) {
        if (LowPerformance) {
            LowPerformanceUtils.logExit(TAG, method, null);
        }
    }

	private ViewGroup rootView;
	private int[] mutualInteger = new int[] { INVALID_CHILD_INDEX };// this
																		// variable
																		// to
																		// keep
	// tracking the
	// animation on
	// each child so we
	// won't
	// play it twice on the
	// same
	// child . Recall , the
	// animation happened in
	// a
	// serial order and only once .

	// this is a helper class that guarantee that only one animation run at a
	// time on it queue
	private AnimationHelper mAnimationHelper = new AnimationHelper();

	// flag
	private final boolean isVerticalScrollView;

	/**
	 * 
	 */
	public ScrollHandler(View rootView, boolean isVerticalScrollView) {
		if (rootView == null) {
			throw new IllegalArgumentException("Root view can't be null!!");
		}
		if (!(rootView instanceof HorizontalScrollView)
				&& !(rootView instanceof ScrollView)) {
			throw new IllegalArgumentException(
					"what are you sending to us ??? this should be a scroll view !!!");
		}
		this.rootView = (ViewGroup) rootView;
		this.isVerticalScrollView = isVerticalScrollView;
		registerGlobalLayoutListenerOnRootView();
	}

	private void registerGlobalLayoutListenerOnRootView() {
		rootView.getViewTreeObserver().addOnGlobalLayoutListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onGlobalLayout() {

		if (rootView.getChildCount() == 0) {// this to handle the case if the
											// Child is
			// loaded at runtime .
			return;
		}
		ViewGroup directChild = (ViewGroup) rootView.getChildAt(0);
		final int childCount = ((ViewGroup) directChild).getChildCount();

		if (childCount == 0) {
			// we would like to keep listening ,perhaps the views will be added
			// at runtime of the child child
			return;
		}
		// now we don't want to keep listening because childs of child is
		// added now so we can start the initial animation .
		if (Util.isJellyBean()) {
			rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
		} else {
			rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
		}

		// fake scroll
		// this to start the animation on the first visible item(s) in the
		// scroll view i.e will cause the onScrollChange to be invoked
		// (Initially needed)
		if (isVerticalScrollView) {
			rootView.scrollTo(0, 1);// scroll the view in the y-axis
		} else {
			// horizontal
			rootView.scrollTo(1, 0); // scroll in the x axis
		}
	}

	/*
	 * package visible only to prevent clients from calling it and bring bugs to
	 * their life
	 */
	void onScrollChange() {
        logEnter("onScrollChange");
		// this will
		// loop all children
		// if the child top is less or equal the bottom of the this view (
		// scroll view) and do not played the animation
		// play it
		if (mutualInteger[0] < 0) {
			mutualInteger[0] = 0;
		}
		View child;
		boolean flipAnimation = false;
		final int[] location = new int[2];
		final ViewGroup directChild = (ViewGroup) rootView.getChildAt(0);
		final int childCount = ((ViewGroup) directChild).getChildCount();
		for (int i = mutualInteger[0]; i < childCount; ++i) {
			child = directChild.getChildAt(i);
			if (child != null) {
				child.getLocationOnScreen(location);
				if (animationNeedToBeRunOnChild(location, child.getWidth(), child.getHeight())) {
					++mutualInteger[0];
					child.setVisibility(View.INVISIBLE);// hide child and don't
														// worry animation have
														// fillAfter = true
					playAnimation(child, flipAnimation);

				} else {
					// no need to continue checking child
					break;
				}
			}
			flipAnimation = !flipAnimation;
		}
        logExit("onScrollChange");
	}

	private boolean animationNeedToBeRunOnChild(int[] childLocationOnScreen, int w, int h) {
		boolean result = false;
		if (isVerticalScrollView) {
            final int top = childLocationOnScreen[1];
            final int bottom = top + h;
			if (top <= rootView.getBottom()) {
//				result = true;
                result = bottom > 0;
			}
		} else {
            final int left = childLocationOnScreen[0];
            final int right = left + w;
			if (left <= rootView.getRight()) {
//				result = true;
                result = right > 0;
			}
		}

		return result;

	}

	private void playAnimation(View child, boolean flipAnimation) {

		mAnimationHelper.playAnimation(child, AnimationFactory.getSlideUpAnimation(),
				AnimationFactory.getRotateAnimation());

	}

	// use only when you want to save state for animation
	public void saveState(Bundle args) {
        logEnter("saveState");
		if (args != null) {
			args.putInt("__mutable_integer__", mutualInteger[0]);
		}
        logExit("saveState");;
	}

	public void restoreState(Bundle args) {
        logEnter("restoreState");
		if (args != null) {
			mutualInteger[0] = args.getInt(BUNDLE_KEY_MUTABLE_INTEGER,
					INVALID_CHILD_INDEX);
		}
        logExit("restoreState");
	}

	private static final int INVALID_CHILD_INDEX = -1;
	private static final String BUNDLE_KEY_MUTABLE_INTEGER = "__mutable_integer__";

	public boolean didAnimationPlayedOnChild(int position) {
        logEnter("didAnimationPlayedOnChild");
		boolean result = false;

		if (mutualInteger[0] >= position) {
			result = true;
		}
        logExit("didAnimationPlayedOnChild");
		return result;
	}

}
