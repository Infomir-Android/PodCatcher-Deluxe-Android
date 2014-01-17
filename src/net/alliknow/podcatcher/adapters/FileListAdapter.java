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

package net.alliknow.podcatcher.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.SelectFileActivity;
import net.alliknow.podcatcher.view.DirectoryListItemView;
import net.alliknow.podcatcher.view.FileListItemView;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

/**
 * The file list adapter to provide the data for the list in the file/folder
 * selection dialog.
 */
public class FileListAdapter extends PodcatcherBaseListAdapter {

    SelectFileActivity.SelectionMode mode;

    /**
     * The current path items
     */
    private final File[] files;
    /**
     * The default file filter to apply
     */
    private static FileFilter filter;

    private DirectoryListItemView.DirectoryListener directoryListener;

    /**
     * Create new adapter. Sub-files of given path will be sorted. Hidden files
     * are excluded.
     *
     * @param context Context we live in.
     * @param path    Path to represent children of.
     */
    public FileListAdapter(Context context, File path, SelectFileActivity.SelectionMode mode) {
        super(context);
        this.mode = mode;

        switch (mode) {
            case FILE:
                filter = new FileFilter() {

                    @Override
                    public boolean accept(File pathname) {
                        return !pathname.isHidden();
                    }
                };
                break;
            case FOLDER:
                filter = new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return !pathname.isHidden() && pathname.isDirectory();
                    }
                };
                break;
        }

        this.files = path.listFiles(filter);
        Arrays.sort(files);
    }

    @Override
    public int getCount() {
        return files.length;
    }

    @Override
    public Object getItem(int position) {
        return files[position];
    }

    @Override
    public long getItemId(int position) {
        return files[position].hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int layout = mode == SelectFileActivity.SelectionMode.FILE ? R.layout.file_list_item
                : R.layout.file_list_item_directory;

        final FileListItemView returnView = (FileListItemView)
                findReturnView(convertView, parent, layout);

        // Make sure the coloring is right
        setBackgroundColorForPosition(returnView, position);
        // Make the view represent file at given position
        returnView.show((File) getItem(position));

        if (mode == SelectFileActivity.SelectionMode.FOLDER) {
            ((DirectoryListItemView) returnView).setDirectoryListener(directoryListener);
        }

        return returnView;
    }

    public void setDirectoryListener(DirectoryListItemView.DirectoryListener listener) {
        this.directoryListener = listener;
    }
}
