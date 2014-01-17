package net.alliknow.podcatcher.view.anim;

import android.view.animation.Animation;

import java.util.ArrayList;
import java.util.List;

public class AnimationLocker {

    protected List<Animation> locks;
    protected final Integer lock = 0;

    public AnimationLocker() {
        locks = new ArrayList<Animation>();
    }

    public void addAnimationLock(Animation animation) {
        synchronized (lock) {
            locks.add(animation);
        }
    }

    public void releaseAnimationLock(Animation animation) {
        synchronized (lock) {
            locks.remove(animation);
        }
    }

    public boolean isFree() {
        synchronized (lock) {
            return locks.size() == 0;
        }
    }

}
