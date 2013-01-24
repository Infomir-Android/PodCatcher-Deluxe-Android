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

package net.alliknow.podcatcher;

import android.os.Bundle;

import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;

import java.util.ArrayList;

/**
 * @author Kevin Hausmann
 */
public class ShowEpisodeListActivity extends EpisodeListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if we need this activity at all
        if (viewMode != SMALL_PORTRAIT_VIEW) {
            finish();
        } else {
            setContentView(R.layout.main);

            if (savedInstanceState == null)
                // During initial setup, plug in the episode list fragment.
                getFragmentManager()
                        .beginTransaction()
                        .add(R.id.content, new EpisodeListFragment(),
                                getResources().getString(R.string.episode_list_fragment_tag))
                        .commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Prepare UI
        episodeListFragment.resetAndSpin();

        // Get the load mode
        multiplePodcastsMode = getIntent().getExtras().getBoolean(PodcastActivity.MODE_KEY);

        // We are in select all mode
        if (multiplePodcastsMode) {
            currentEpisodeList = new ArrayList<Episode>();

            for (Podcast podcast : podcastManager.getPodcastList())
                podcastManager.load(podcast);
        } // Single podcast to load
        else {
            // Get URL of podcast to load
            String podcastUrl = getIntent().getExtras().getString(PODCAST_URL_KEY);

            // Find the podcast object
            for (Podcast podcast : podcastManager.getPodcastList())
                if (podcast.getUrl().toString().equals(podcastUrl))
                    this.currentPodcast = podcast;

            // Go load it if found
            if (currentPodcast != null)
                podcastManager.load(currentPodcast);
        }
    }

    @Override
    protected void updatePlayer() {
        super.updatePlayer();

        if (playerFragment != null) {
            playerFragment.setLoadMenuItemVisibility(false, false);
            playerFragment.setPlayerTitleVisibility(true);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
    }
}
