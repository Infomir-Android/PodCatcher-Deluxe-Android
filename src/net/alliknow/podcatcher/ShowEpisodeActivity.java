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

import net.alliknow.podcatcher.view.fragments.EpisodeFragment;
import android.os.Bundle;

/**
 * 
 */
public class ShowEpisodeActivity extends PodcatcherBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        switch (viewMode) {
        // In large layouts we do not need this activity at all
            case LARGE_PORTRAIT_VIEW:
            case LARGE_LANDSCAPE_VIEW:
                finish();
                break;
            case SMALL_LANDSCAPE_VIEW:
                // To recover from configuration changes here, we have
                // to send an intent to the main activity and tell it
                // to show the episode
                // TODO Send intent
                finish();
                break;
            case SMALL_PORTRAIT_VIEW:
                if (savedInstanceState == null) {
                    // During initial setup, plug in the details fragment.
                    EpisodeFragment episode = new EpisodeFragment();
                    // Set episode with URL from intent
                    // (getIntent().getExtras());
                    getFragmentManager().beginTransaction()
                            .add(android.R.id.content, episode, episodeFragmentTag).commit();
                }
        }
    }
}
