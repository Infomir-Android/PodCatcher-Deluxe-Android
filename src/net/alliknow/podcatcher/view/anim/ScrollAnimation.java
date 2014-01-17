package net.alliknow.podcatcher.view.anim;

import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AbsoluteLayout;

public class ScrollAnimation extends Animation {

    public static final int DIRECTION_TO_RIGHT = 0;
    public static final int DIRECTION_TO_LEFT = 1;

    protected int direction;
    protected View[] views;
    protected int delta;
    protected int sum;
    protected float prevTime;

    public ScrollAnimation(int direction, int delta, View... views) {
        super();
        this.direction = direction;
        this.views = views;
        this.delta = delta;
        if (direction == DIRECTION_TO_LEFT) {
            this.delta *= -1;
        }
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        int deltaX = Math.round((interpolatedTime - prevTime) * (float) delta);
        Log.v("///", "" + interpolatedTime);
        if (interpolatedTime == 1.0f) {
            deltaX = delta - sum;
            sum = delta;
        } else {
            if (Math.abs(sum + deltaX) > Math.abs(delta)) {
                deltaX = delta - sum;
                sum = delta;
            } else {
                sum += deltaX;
            }
        }
        if (deltaX != 0) {
            for (View view : views) {
                AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) view.getLayoutParams();
                params.x += deltaX;
                view.setLayoutParams(params);
            }
        }
        prevTime = interpolatedTime;
    }


}
