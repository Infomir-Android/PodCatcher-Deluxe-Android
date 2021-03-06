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

package net.alliknow.podcatcher.model.tasks;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.OnStorePodcastListListener;
import net.alliknow.podcatcher.model.PodcastManager;
import net.alliknow.podcatcher.model.tags.OPML;
import net.alliknow.podcatcher.model.types.Podcast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;

/**
 * Stores the a podcast list to the file system asynchronously. Use
 * {@link OnStorePodcastListListener} to monitor the task.
 */
public class StorePodcastListTask extends StoreFileTask<List<Podcast>> {

    /** Our context */
    protected Context context;
    /** The call-back */
    protected OnStorePodcastListListener listener;

    /** The podcast list to store */
    protected List<Podcast> podcastList;
    /**
     * Flag to indicate whether the task should write authorization information
     * to the resulting file.
     */
    protected boolean writeAuthorization = false;
    /** The file/dir that we write to. */
    protected File exportLocation;
    /** The exception that might have been occurred */
    protected Exception exception;

    /** Content of OPML file title tag */
    protected String opmlFileTitle = "podcast file";

    /**
     * Create new task.
     * 
     * @param context Context to get file handle from (not <code>null</code>).
     * @see PodcastManager#OPML_FILENAME
     */
    public StorePodcastListTask(Context context) {
        this.context = context;

        opmlFileTitle = context.getString(R.string.app_name) + " " + opmlFileTitle;
    }

    /**
     * Create new task with a call-back attached.
     * 
     * @param context Context to get file handle from (not <code>null</code>).
     * @param listener Listener to alert on completion or failure.
     * @see PodcastManager#OPML_FILENAME
     */
    public StorePodcastListTask(Context context, OnStorePodcastListListener listener) {
        this(context);

        this.listener = listener;
    }

    /**
     * Define where the task should store the podcast OPML file. Not setting
     * this (or given <code>null</code> here) will result in the file being
     * stored to the private app directory.
     * 
     * @param location The location to write to.
     */
    public void setCustomLocation(File location) {
        this.exportLocation = location;
    }

    /**
     * Sets the write authorization flag. If set to <code>true</code>, the
     * resulting OPML file will contain extra information on the user's
     * credentials for all podcasts in the list. The default is
     * <code>false</code>. Use with care!
     * 
     * @param write Whether credentials should be written to output.
     */
    public void setWriteAuthorization(boolean write) {
        this.writeAuthorization = write;
    }

    @Override
    protected Void doInBackground(List<Podcast>... params) {
        this.podcastList = params[0];

        try {
            // 1. Open the file and get a writer
            // Store to the default location if nothing else was set
            if (exportLocation == null)
                exportLocation = context.getFilesDir();
            // Make sure the required folders exist
            exportLocation.mkdirs();
            // Make sure we point to the actual file, not the dir
            if (exportLocation.isDirectory())
                exportLocation = new File(exportLocation, PodcastManager.OPML_FILENAME);
            // Create the stream
            OutputStream fileStream = new FileOutputStream(exportLocation);
            // ... and finally the writer
            writer = new BufferedWriter(new OutputStreamWriter(fileStream,
                    PodcastManager.OPML_FILE_ENCODING));

            // 2. Write new file content
            writeHeader(opmlFileTitle);
            for (Podcast podcast : podcastList)
                writePodcast(podcast);
            writeFooter();
        } catch (Exception ex) {
            Log.e(getClass().getSimpleName(), "Cannot store podcast OPML file", ex);
            this.exception = ex;

            cancel(true);
        } finally {
            // Make sure we close the file stream
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e) {
                    /* Nothing we can do here */
                    Log.w(getClass().getSimpleName(), "Failed to close podcast file writer!", e);
                }
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void nothing) {
        if (listener != null)
            listener.onPodcastListStored(podcastList, exportLocation);
    }

    @Override
    protected void onCancelled(Void nothing) {
        if (listener != null)
            listener.onPodcastListStoreFailed(podcastList, exportLocation, exception);
    }

    private void writePodcast(Podcast podcast) throws IOException {
        // Skip, if not a valid podcast
        if (hasNameAndUrl(podcast)) {
            String opmlString = "<" + OPML.OUTLINE + " " + OPML.TEXT + "=\"" +
                    TextUtils.htmlEncode(podcast.getName()) + "\" " +
                    OPML.TYPE + "=\"" + OPML.RSS_TYPE + "\" " +
                    OPML.XMLURL + "=\"" +
                    TextUtils.htmlEncode(podcast.getUrl().toString()) + "\"/>";

            if (writeAuthorization && podcast.getAuthorization() != null) {
                opmlString = opmlString.substring(0, opmlString.length() - 2);

                // We store the podcast password in the app's private folder
                // (but in the clear). This is justified because it is hard to
                // attack the file (unless you get your hands on the device) and
                // the password is not very sensitive since it is only a
                // podcast we are accessing, not personal information.
                opmlString += " " + OPML.EXTRA_USER + "=\"" +
                        TextUtils.htmlEncode(podcast.getUsername()) + "\" " +
                        OPML.EXTRA_PASS + "=\"" +
                        TextUtils.htmlEncode(podcast.getPassword()) + "\"/>";
            }

            writeLine(2, opmlString);
        }
    }

    /**
     * @return Whether given podcast has an non-empty name and an URL.
     */
    private boolean hasNameAndUrl(Podcast podcast) {
        return podcast.getName() != null && podcast.getName().length() > 0
                && podcast.getUrl() != null;
    }

    private void writeHeader(String fileName) throws IOException {
        writeLine(0, "<?xml version=\"1.0\" encoding=\"" + FILE_ENCODING + "\"?>");
        writeLine(0, "<opml version=\"2.0\">");
        writeLine(1, "<head>");
        writeLine(2, "<title>" + fileName + "</title>");
        writeLine(2, "<dateModified>" + new Date().toString() + "</dateModified>");
        writeLine(1, "</head>");
        writeLine(1, "<body>");
    }

    private void writeFooter() throws IOException {
        writeLine(1, "</body>");
        writeLine(0, "</opml>");
    }
}
