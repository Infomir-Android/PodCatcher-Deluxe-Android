package net.alliknow.podcatcher.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;

public class FileListView extends ListView {

    public FileListView(Context context) {
        super(context);
    }

    public FileListView(Context context, AttributeSet attr) {
        super(context, attr);
    }

    public void clearAll() {
        View selected = getSelectedView();
        if (selected != null) {
            selected.setSelected(false);
        }
    }

    public void restoreSelection() {
        View selected = getSelectedView();
        if (selected != null) {
            selected.setSelected(true);
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        View selected = getSelectedView();
        if (selected != null) {
            selected.setSelected(gainFocus);
        }
    }
}
