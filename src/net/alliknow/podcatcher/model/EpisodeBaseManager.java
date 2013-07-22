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

package net.alliknow.podcatcher.model;

import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.listeners.OnLoadEpisodeMetadataListener;
import net.alliknow.podcatcher.model.tasks.StoreEpisodeMetadataTask;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.EpisodeMetadata;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Base for the episode manager's class hierarchy. This sets things up by
 * defining the basic data structures.
 * 
 * @see EpisodeManager
 */
public abstract class EpisodeBaseManager implements OnLoadEpisodeMetadataListener {

    /** The file name to store local episode metadata information under */
    public static final String METADATA_FILENAME = "episodes.xml";

    /** The application itself (used e.g. as context in tasks) */
    protected Podcatcher podcatcher;

    /** The metadata information held for episodes */
    protected Map<URL, EpisodeMetadata> metadata;
    /** Flag to indicate whether metadata is dirty */
    protected boolean metadataChanged;

    /** Latch we use to block all threads until we have our data */
    private CountDownLatch latch = new CountDownLatch(1);

    /**
     * Init the base episode manager.
     * 
     * @param app The podcatcher application object (also a singleton).
     */
    protected EpisodeBaseManager(Podcatcher app) {
        // We use some of its method below, so we keep a reference to the
        // application object.
        this.podcatcher = app;
    }

    @Override
    public void onEpisodeMetadataLoaded(Map<URL, EpisodeMetadata> metadata) {
        // We want our metadata to be thread safe, since we might load some
        // clean-up work off to other threads.
        this.metadata = new ConcurrentHashMap<URL, EpisodeMetadata>(metadata);
        this.metadataChanged = false;

        // Here we need to release all threads (AsyncTasks) that might be
        // waiting for the episode metadata to become available
        latch.countDown();
    }

    /**
     * This blocks the calling thread until the episode metadata has become
     * available during the application's start-up. Once the metadata is read,
     * this method returns immediately.
     * 
     * @throws InterruptedException When the thread is interrupted while
     *             waiting.
     */
    public void blockUntilEpisodeMetadataIsLoaded() throws InterruptedException {
        latch.await();
    }

    /**
     * Persist the manager's data to disk.
     */
    @SuppressWarnings("unchecked")
    public void saveState() {
        // Store cleaned metadata if dirty
        if (metadataChanged && metadata != null) {
            // Store a copy of the actual map, since there might come in changes
            // to the metadata while the task is running and that would lead to
            // a concurrent modification exception.
            new StoreEpisodeMetadataTask(podcatcher)
                    .execute(new HashMap<URL, EpisodeMetadata>(metadata));

            // Reset the flag, so the list will only be saved if changed again.
            // TODO Storing the metadata might fail?
            metadataChanged = false;
        }
    }

    /**
     * Utility method to populate an episode's metadata object.
     * 
     * @param episode Episode to take data from
     * @param meta Metadate holder to populate
     */
    protected void putAdditionalEpisodeInformation(Episode episode, EpisodeMetadata meta) {
        // We need all there object to be present: episode, podcast and holder
        if (episode != null && meta != null && episode.getPodcast() != null) {
            meta.episodeName = episode.getName();
            meta.episodePubDate = episode.getPubDate();
            meta.episodeDescription = episode.getDescription();
            meta.podcastName = episode.getPodcast().getName();
            meta.podcastUrl = episode.getPodcast().getUrl().toString();
        }
    }
}
