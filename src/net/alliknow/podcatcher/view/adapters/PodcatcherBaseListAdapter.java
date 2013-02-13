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

package net.alliknow.podcatcher.view.adapters;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.TextView;

import net.alliknow.podcatcher.R;

/**
 * Abstract super class for this app's list adapters. Handles the
 * selection/choice parts. All lists are single choice or select all and have
 * their background changed for the selected item.
 */
public abstract class PodcatcherBaseListAdapter extends PodcatcherBaseAdapter {

    /** We need to know the selected item positions in the list */
    protected SparseBooleanArray selectedPositions = new SparseBooleanArray();
    /** Also, there might be checked items */
    protected SparseBooleanArray checkedPositions = new SparseBooleanArray();
    /** Flag for whether we are in select all mode */
    protected boolean selectAll = false;

    /**
     * Create new adapter.
     * 
     * @param context The current context.
     */
    public PodcatcherBaseListAdapter(Context context) {
        super(context);
    }

    /**
     * Set the selected item in the list and updates the UI to reflect the
     * selection.
     * 
     * @param position Position selected.
     */
    public void setSelectedPosition(int position) {
        selectAll = false;
        selectedPositions.clear();
        selectedPositions.put(position, true);

        notifyDataSetChanged();
    }

    /**
     * Put adapter in select all mode.
     */
    public void setSelectAll() {
        selectAll = true;
        selectedPositions.clear();

        notifyDataSetChanged();
    }

    /**
     * Put adapter in select none mode.
     */
    public void setSelectNone() {
        selectAll = false;
        selectedPositions.clear();

        notifyDataSetChanged();
    }

    /**
     * @return The selected position, or -1 if none.
     */
    public int getSelectedPosition() {
        if (selectAll || selectedPositions.size() != 1)
            return -1;
        else
            return selectedPositions.keyAt(0);
    }

    /**
     * Set the chosen items in the list.
     * 
     * @param positions The array denoting chosen positions. Give
     *            <code>null</code> to reset.
     */
    public void setCheckedPositions(SparseBooleanArray positions) {
        if (positions == null)
            checkedPositions = new SparseBooleanArray();
        else
            checkedPositions = positions;

        notifyDataSetChanged();
    }

    /**
     * This sets a views background color according to the selection state of
     * the given position.
     * 
     * @param view View to set background for.
     * @param position Position of the view in the list.
     */
    protected void setBackgroundColorForPosition(View view, int position) {
        // Set list item color background
        view.setBackgroundResource(checkedPositions.get(position) ?
                R.color.theme_light : selectedPositions.get(position) ?
                        R.color.theme_dark : R.color.transparent);
    }

    /**
     * Set text for a list item view element.
     * 
     * @param listItem The view representing the whole list item.
     * @param viewId View id of the child view, has to be (a subclass of)
     *            <code>TextView</code>.
     * @param text Text to display.
     */
    protected void setText(View listItem, int viewId, String text) {
        ((TextView) listItem.findViewById(viewId)).setText(text);
    }
}
