package net.alliknow.podcatcher.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.Spinner;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MySpinner extends Spinner {

    Object mPopupWindow;

    public MySpinner(Context context) {
        this(context, null);
    }

    public MySpinner(Context context, AttributeSet set) {
        super(context, set);

        try {
            Field dropdownPopupField = Spinner.class.getDeclaredField("mPopup");
            AccessibleObject.setAccessible(new AccessibleObject[]{dropdownPopupField}, true);
            Object dropdownPopupWindow = dropdownPopupField.get(this);

            Field popupField = ListPopupWindow.class.getDeclaredField("mPopup");
            AccessibleObject.setAccessible(new AccessibleObject[]{popupField}, true);
            Object popupWindow = popupField.get(dropdownPopupWindow);

            Method setLayoutMode = PopupWindow.class.getMethod("setWindowLayoutMode", int.class, int.class);
            setLayoutMode.invoke(popupWindow, 0, 0);

            mPopupWindow = popupWindow;

        } catch (Exception e) {
            // pass
        }


    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        return super.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        setSelected(gainFocus);
    }

    @Override
    public boolean performClick() {
        boolean result = super.performClick();

        try {
            Method setInputMethodMode = PopupWindow.class.getMethod("setInputMethodMode", int.class);
            setInputMethodMode.invoke(mPopupWindow, PopupWindow.INPUT_METHOD_NEEDED);
        } catch (Exception ignored) {

        }

        return result;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // I don't know why, but this must be done first
        boolean b = super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                break;
        }
        return b;
    }
}
