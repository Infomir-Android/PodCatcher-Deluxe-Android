package net.alliknow.podcatcher.view.anim;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AbsoluteLayout;

public class ExpandCollapseAnimation extends Animation {

    private int initialWidth;
    private int delta;
    private View view;
    private View[] satellites;

    public static final int DIRECTION_EXPAND = 0;
    public static final int DIRECTION_COLLAPSE = 1;

    public ExpandCollapseAnimation(View view, int targetWidth, int direction, View... satellites) {
        super();
        this.initialWidth = view.getMeasuredWidth();
        this.delta = initialWidth - targetWidth;
        this.view = view;
        this.satellites = satellites;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        int width = initialWidth - (int) (interpolatedTime * delta);
        int deltaWidth = view.getLayoutParams().width - width;
        view.getLayoutParams().width = width;
        view.requestLayout();
        for (View satellite : satellites) {
            AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) satellite.getLayoutParams();
            params.x -= deltaWidth;
            satellite.setLayoutParams(params);
        }
    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }
}
