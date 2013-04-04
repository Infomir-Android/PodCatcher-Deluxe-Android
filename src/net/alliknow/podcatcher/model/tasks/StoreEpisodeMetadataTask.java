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

import static net.alliknow.podcatcher.model.tags.METADATA.DOWNLOAD_ID;
import static net.alliknow.podcatcher.model.tags.METADATA.EPISODE_DATE;
import static net.alliknow.podcatcher.model.tags.METADATA.EPISODE_DESCRIPTION;
import static net.alliknow.podcatcher.model.tags.METADATA.EPISODE_NAME;
import static net.alliknow.podcatcher.model.tags.METADATA.EPISODE_RESUME_AT;
import static net.alliknow.podcatcher.model.tags.METADATA.EPISODE_URL;
import static net.alliknow.podcatcher.model.tags.METADATA.LOCAL_FILE_PATH;
import static net.alliknow.podcatcher.model.tags.METADATA.METADATA;
import static net.alliknow.podcatcher.model.tags.METADATA.PODCAST_NAME;
import static net.alliknow.podcatcher.model.tags.METADATA.PODCAST_URL;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.types.EpisodeMetadata;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Stores the episode metadata information to the file system.
 */
public class StoreEpisodeMetadataTask extends StoreFileTask<Map<URL, EpisodeMetadata>> {

    /** Our context */
    protected Context context;

    /**
     * Create a new persistence task.
     * 
     * @param context Context to use for file writing.
     */
    public StoreEpisodeMetadataTask(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(Map<URL, EpisodeMetadata>... params) {
        try {
            // 1. Do house keeping and remove all metadata instances without
            // data
            cleanMetadata(params[0]);

            // 2. Open the file and get a writer
            OutputStream fileStream =
                    context.openFileOutput(EpisodeManager.METADATA_FILENAME, Context.MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(fileStream, FILE_ENCODING));

            // 3. Write new file content
            writeHeader();
            for (Entry<URL, EpisodeMetadata> entry : params[0].entrySet())
                writeRecord(entry.getKey(), entry.getValue());
            writeFooter();
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Cannot store episode metadata file", e);
        } finally {
            // Make sure we close the file stream
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e) {
                    /* Nothing we can do here */
                    Log.w(getClass().getSimpleName(),
                            "Failed to close episode metadata file writer!", e);
                }
        }

        return null;
    }

    private void writeRecord(URL key, EpisodeMetadata value) throws IOException {
        writeLine(1, "<" + METADATA + " " + EPISODE_URL + "=\"" + key.toString() + "\">");

        writeData(value.episodeName, EPISODE_NAME);
        if (value.episodePubDate != null)
            writeData(value.episodePubDate.getTime(), EPISODE_DATE);
        writeData(value.episodeDescription, EPISODE_DESCRIPTION);
        writeData(value.podcastName, PODCAST_NAME);
        writeData(value.podcastUrl, PODCAST_URL);
        writeData(value.downloadId, DOWNLOAD_ID);
        writeData(value.filePath, LOCAL_FILE_PATH);
        writeData(value.resumeAt, EPISODE_RESUME_AT);

        writeLine(1, "</" + METADATA + ">");
    }

    private void writeData(String data, String tag) throws IOException {
        // For all fields: only write data that is actually there!
        if (data != null)
            writeLine(2, "<" + tag + ">" + TextUtils.htmlEncode(data) + "</" + tag + ">");
    }

    private void writeData(Long data, String tag) throws IOException {
        // For all fields: only write data that is actually there!
        if (data != null)
            writeLine(2, "<" + tag + ">" + data + "</" + tag + ">");
    }

    private void writeData(Integer data, String tag) throws IOException {
        writeData(Long.valueOf(data), tag);
    }

    private void writeHeader() throws IOException {
        writeLine(0, "<?xml version=\"1.0\" encoding=\"" + FILE_ENCODING + "\"?>");
        writeLine(0, "<xml dateModified=\"" + new Date().getTime() + "\">");
    }

    private void writeFooter() throws IOException {
        writeLine(0, "</xml>");
    }

    private void cleanMetadata(Map<URL, EpisodeMetadata> metadata) {
        Iterator<Entry<URL, EpisodeMetadata>> iterator = metadata.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<URL, EpisodeMetadata> entry = iterator.next();

            if (!entry.getValue().hasData())
                iterator.remove();
        }
    }
}