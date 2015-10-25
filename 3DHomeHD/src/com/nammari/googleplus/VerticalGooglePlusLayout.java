package com.nammari.googleplus;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.ScrollView;

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
public class VerticalGooglePlusLayout extends ScrollView implements
        ScrollHandlerInterface {

	ScrollHandler mScrollHandler;

	public VerticalGooglePlusLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();

	}

	public VerticalGooglePlusLayout(Context context) {
		super(context);
		init();
	}

	private void init() {
		mScrollHandler = new ScrollHandler(this, true);
	}

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);

		if (mScrollHandler != null) {
			mScrollHandler.onScrollChange();
		}
	}

	@Override
	public void saveState(Bundle args) {
		// delegate to scrollHandler object
		if (mScrollHandler != null) {
			mScrollHandler.saveState(args);
		}

	}

	@Override
	public void restoreState(Bundle args) {
		// delegate to scrollHandler object
		if (mScrollHandler != null) {
			mScrollHandler.restoreState(args);
		}

	}

	@Override
	public boolean didAnimationPlayedOnChild(int position) {
		// delegate to scrollHandler object
		if (mScrollHandler != null) {
			return mScrollHandler.didAnimationPlayedOnChild(position);
		}
		return false;
	}
}//end class
