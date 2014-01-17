/** Copyright 2012, 2013 Kevin Hausmann
 *
 * This file is part of PodCatcher Deluxe.
 *
 * PodCatcher Deluxe is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * PodCatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PodCatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */

package net.alliknow.podcatcher.view.fragments;

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link View.OnTouchListener} that makes the list items in a
 * {@link ListView} reorderable by swipe. This is based on Roman Nurik's
 * swipe-to-dismiss: https://github.com/romannurik/android-swipetodismiss
 * (originally Apache licensed).
 */
public class SwipeReorderListViewTouchListener implements View.OnTouchListener {
    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;

    // Fixed properties
    private ListView mListView;
    private ReorderCallback mCallbacks;
    private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero

    // Transient properties
    private List<PendingReorderData> mPendingDismisses = new ArrayList<PendingReorderData>();
    private int mDismissAnimationRefCount = 0;
    private float mDownX;
    private boolean mSwiping;
    private VelocityTracker mVelocityTracker;
    private int mDownPosition;
    private View mDownView;
    private boolean mPaused;

    /**
     * The callback interface used by {@link SwipeReorderListViewTouchListener}
     * to inform its client about a successful swipe of a list item positions.
     */
    public interface ReorderCallback {
        /**
         * Find out whether a certain position is swipeable.
         *
         * @param position Position in question.
         * @return <code>true</code> if the item at the given position can be
         *         swiped to re-order.
         */
        public boolean canReorder(int position);

        /**
         * Called on the listener on left swipes.
         *
         * @param position Index flinged.
         */
        public void onMoveUp(int position);

        /**
         * Called on the listener if the user swipes items to the right.
         *
         * @param position Index flinged.
         */
        public void onMoveDown(int position);
    }

    /**
     * Constructs a new swipe-to-reorder touch listener for the given list view.
     *
     * @param listView The list view whose items should be reorderable.
     * @param callback The callback to trigger when the user has indicated that
     *                 she would like to reorder one or more list items.
     */
    public SwipeReorderListViewTouchListener(ListView listView, ReorderCallback callback) {
        ViewConfiguration vc = ViewConfiguration.get(listView.getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mListView = listView;
        mCallbacks = callback;
    }

    /**
     * Enables or disables (pauses or resumes) watching for swipe-to-reorder
     * gestures.
     *
     * @param enabled Whether or not to watch for gestures.
     */
    private void setEnabled(boolean enabled) {
        mPaused = !enabled;
    }

    /**
     * @return An {@link android.widget.AbsListView.OnScrollListener} to be
     *         added to the {@link ListView} using
     *         {@link ListView#setOnScrollListener(AbsListView.OnScrollListener)}
     *         . If a scroll listener is already assigned, the caller should
     *         still pass scroll changes through to this listener. This will
     *         ensure that this {@link SwipeReorderListViewTouchListener} is
     *         paused during list view scrolling.
     * @see SwipeReorderListViewTouchListener
     */
    public AbsListView.OnScrollListener makeScrollListener() {
        return new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
            }
        };
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (mViewWidth < 2) {
            mViewWidth = mListView.getWidth();
        }

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (mPaused) {
                    return false;
                }

                // Find the child view that was touched (perform a hit test)
                Rect rect = new Rect();
                int childCount = mListView.getChildCount();
                int[] listViewCoords = new int[2];
                mListView.getLocationOnScreen(listViewCoords);
                int x = (int) motionEvent.getRawX() - listViewCoords[0];
                int y = (int) motionEvent.getRawY() - listViewCoords[1];
                View child;
                for (int i = 0; i < childCount; i++) {
                    child = mListView.getChildAt(i);
                    child.getHitRect(rect);
                    if (rect.contains(x, y)) {
                        mDownView = child;
                        break;
                    }
                }

                if (mDownView != null) {
                    mDownX = motionEvent.getRawX();
                    mDownPosition = mListView.getPositionForView(mDownView);
                    if (mCallbacks.canReorder(mDownPosition)) {
                        mVelocityTracker = VelocityTracker.obtain();
                        mVelocityTracker.addMovement(motionEvent);
                    } else {
                        mDownView = null;
                    }
                }
                view.onTouchEvent(motionEvent);
                return true;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null) {
                    break;
                }

                float deltaX = motionEvent.getRawX() - mDownX;
                mVelocityTracker.addMovement(motionEvent);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityX = mVelocityTracker.getXVelocity();
                float absVelocityX = Math.abs(velocityX);
                float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());
                boolean dismiss = false;
                boolean dismissRight = false;
                if (Math.abs(deltaX) > mViewWidth / 2) {
                    dismiss = true;
                    dismissRight = deltaX > 0;
                } else if (mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity
                        && absVelocityY < absVelocityX) {
                    // dismiss only if flinging in the same direction as
                    // dragging
                    dismiss = (velocityX < 0) == (deltaX < 0);
                    dismissRight = mVelocityTracker.getXVelocity() > 0;
                }
                if (dismiss) {
                    // dismiss
                    final View downView = mDownView; // mDownView gets null'd
                    // before animation ends
                    final int downPosition = mDownPosition;
                    final boolean up = !dismissRight;
                    ++mDismissAnimationRefCount;

                    // TODO: make an actual swipe
                    performReorder(downView, downPosition, up);
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownX = 0;
                mDownView = null;
                mDownPosition = ListView.INVALID_POSITION;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mVelocityTracker == null || mPaused) {
                    break;
                }

                mVelocityTracker.addMovement(motionEvent);
                float deltaX = motionEvent.getRawX() - mDownX;
                if (Math.abs(deltaX) > mSlop) {
                    mSwiping = true;
                    mListView.requestDisallowInterceptTouchEvent(true);

                    // Cancel ListView's touch (un-highlighting the item)
                    MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                            (motionEvent.getActionIndex()
                                    << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    mListView.onTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                }

                if (mSwiping) {
                    return true;
                }
                break;
            }
        }
        return false;
    }

    class PendingReorderData implements Comparable<PendingReorderData> {
        public int position;
        public View view;

        public PendingReorderData(int position, View view) {
            this.position = position;
            this.view = view;
        }

        @Override
        public int compareTo(PendingReorderData other) {
            // Sort by descending position
            return other.position - position;
        }
    }

    private void performReorder(final View dismissView, final int dismissPosition, final boolean up) {
        // Animate the dismissed list item to zero-height and fire the reorder
        // callback when all dismissed list item animations have completed. This
        // triggers layout on each animation frame; in the future we may want to
        // do something smarter and more performant.

        if (up)
            mCallbacks.onMoveUp(dismissPosition);
        else
            mCallbacks.onMoveDown(dismissPosition);
    }
}
