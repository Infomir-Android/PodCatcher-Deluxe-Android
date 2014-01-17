package net.alliknow.podcatcher.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import net.alliknow.podcatcher.R;

import java.io.File;

public class DirectoryListItemView extends FileListItemView implements View.OnClickListener {

    private Button btEnter;
    private Button btSelect;

    protected DirectoryListener listener;

    public DirectoryListItemView(Context context, AttributeSet set) {
        super(context, set);
    }

    public void setDirectoryListener(DirectoryListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        btEnter = (Button) findViewById(R.id.bt_enter);
        btSelect = (Button) findViewById(R.id.bt_select);

        btEnter.setOnClickListener(this);
        btSelect.setOnClickListener(this);

        updateButtons(isSelected());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_enter:
                listener.onDirectoryEntered(file);
                break;
            case R.id.bt_select:
                listener.onDirectorySelected(file);
                break;
        }
    }

    @Override
    public boolean performClick() {
        update(true);
        return btEnter.performClick();
    }

    @Override
    protected boolean dispatchHoverEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_HOVER_ENTER:
                updateButtons(true);
                break;
            case MotionEvent.ACTION_HOVER_EXIT:
                if (event.getButtonState() > 0) {
                    // there is some strange call from touching, ignore
                    break;
                }
                updateButtons(false);
                break;
        }
        return super.dispatchHoverEvent(event);
    }

    private void updateButtons(boolean selected) {
        int visibility = selected ? VISIBLE : INVISIBLE;
        if (selected) {
            updateButtonsBackground();
        }
        if (!isInTouchMode() && !selected && (btEnter.isFocused() || btSelect.isFocused())) {
            requestFocus();
            requestFocus();
            requestFocus();
        }
        btEnter.setVisibility(visibility);
        btSelect.setVisibility(visibility);
    }

    private void updateButtonsBackground() {
        int backgroundRes;
        int textColorRes;
        if (isInTouchMode()) {
            backgroundRes = R.drawable.directory_button_bg_default;
            textColorRes = R.color.directory_button_text_default;
        } else {
            backgroundRes = R.drawable.directory_button_bg_selected;
            textColorRes = R.color.directory_button_text_selected;
        }

        btEnter.setBackgroundResource(backgroundRes);
        btSelect.setBackgroundResource(backgroundRes);

        btEnter.setTextColor(getResources().getColorStateList(textColorRes));
        btSelect.setTextColor(getResources().getColorStateList(textColorRes));
    }

    public void update(boolean selected) {
        updateBackground(selected);
        updateButtons(selected);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        update(selected);
        btEnter.requestFocus();
    }

    private void updateBackground(boolean selected) {
        int color = selected ? R.color.dialog_item_bg_focused : R.color.dialog_item_bg_default;
        setBackgroundColor(getResources().getColor(color));
//        int textColor = selected ? R.color.dialog_item_text_selected : R.color.dialog_item_text_default;
//        nameTextView.setTextColor(getResources().getColor(textColor));
//        int drawable = selected ? R.drawable.ic_directory_focused : R.drawable.ic_directory_default;
//        iconView.setImageResource(drawable);
    }

    public static interface DirectoryListener {

        void onDirectoryEntered(File file);

        void onDirectorySelected(File file);

    }
}
