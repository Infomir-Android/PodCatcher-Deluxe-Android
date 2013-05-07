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
import android.view.MenuItem;

import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.view.fragments.EpisodeFragment;

/**
 * Activity to show only the episode and possibly the player. Used in small
 * portrait view mode only.
 */
public class ShowEpisodeActivity extends EpisodeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // In large or landscape layouts we do not need this activity at
        // all, so finish it off
        if (!viewMode.isSmallPortrait())
            finish();
        else {
            // Set the content view
            setContentView(R.layout.main);
            // Set fragment members
            findFragments();

            // During initial setup, plug in the episode details fragment.
            if (savedInstanceState == null && episodeFragment == null) {
                episodeFragment = new EpisodeFragment();
                getFragmentManager()
                        .beginTransaction()
                        .add(R.id.content, episodeFragment,
                                getString(R.string.episode_fragment_tag))
                        .commit();
            }

            // Set it
            if (selection.getEpisode() != null)
                onEpisodeSelected(selection.getEpisode());
        }
    }

    @Override
    public void onEpisodeSelected(Episode selectedEpisode) {
        super.onEpisodeSelected(selectedEpisode);

        episodeFragment.setEpisode(selectedEpisode);
        episodeFragment.setShowEpisodeDate(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed
                finish();
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void updateActionBar() {
        getActionBar().setTitle(R.string.app_name);
        getActionBar().setSubtitle(null);

        // Enable navigation
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
