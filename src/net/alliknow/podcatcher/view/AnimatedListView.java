package net.alliknow.podcatcher.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class AnimatedListView extends ListView {

    List<Animation> mAnimations = new ArrayList<Animation>();

    public AnimatedListView(Context context) {
        super(context);
    }

    public AnimatedListView(Context context, AttributeSet attr) {
        super(context, attr);
    }

    public void notifyAnimationStarted(Animation animation) {
        mAnimations.add(animation);
    }

    public void notifyAnimationFinished(Animation animation) {
        mAnimations.remove(animation);
    }

    public boolean isAnimated() {
        return mAnimations.size() > 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isAnimated()) {
            setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
