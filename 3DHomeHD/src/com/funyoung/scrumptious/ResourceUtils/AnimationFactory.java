package com.funyoung.scrumptious.ResourceUtils;


import com.borqs.freehdhome.R;

/**
 * Created by yangfeng on 13-7-3.
 */
public class AnimationFactory {
    private AnimationFactory() {
        // no instance
    }

    public static int getSlideUpAnimation() {
        return R.anim.slide_up;
    }

    public static int getRotateAnimation() {
        return R.animator.rotate_animation;
    }
}
