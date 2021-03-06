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

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;

import net.alliknow.podcatcher.listeners.OnAddSuggestionListener;
import net.alliknow.podcatcher.listeners.OnLoadSuggestionListener;
import net.alliknow.podcatcher.model.SuggestionManager;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;
import net.alliknow.podcatcher.view.fragments.SuggestionFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Add podcast from suggestions activity.
 */
public class AddSuggestionActivity extends BaseActivity implements
        OnLoadSuggestionListener, OnAddSuggestionListener, OnCancelListener {

    /** The tag we identify our show suggestions fragment with */
    public static final String SHOW_SUGGESTIONS_FRAGMENT_TAG = "show_suggestions";

    /** The fragment containing the add suggestion UI */
    private SuggestionFragment suggestionFragment;

    /** The suggestion manager */
    private SuggestionManager suggestionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get suggestions manager and register call-back
        suggestionManager = SuggestionManager.getInstance();
        suggestionManager.addLoadSuggestionListListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Try to find existing fragment
        suggestionFragment = (SuggestionFragment) getFragmentManager().findFragmentByTag(
                SHOW_SUGGESTIONS_FRAGMENT_TAG);

        // No fragment found, create it
        if (suggestionFragment == null) {
            suggestionFragment = new SuggestionFragment();
            suggestionFragment.setStyle(DialogFragment.STYLE_NORMAL,
                    android.R.style.Theme_Holo_Light_Dialog);

            // Show the fragment
            suggestionFragment.show(getFragmentManager(), SHOW_SUGGESTIONS_FRAGMENT_TAG);
        }

        // Load suggestions (this has to be called after UI fragment is created)
        suggestionManager.load();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        suggestionManager.removeLoadSuggestionListListener(this);
    }

    @Override
    public void onSuggestionsLoadProgress(Progress progress) {
        suggestionFragment.showLoadProgress(progress);
    }

    @Override
    public void onSuggestionsLoaded(List<Podcast> suggestions) {
        // Currently already existing podcasts (to be filtered out)
        List<Podcast> podcastList = podcastManager.getPodcastList();
        // Resulting list
        List<Podcast> filteredSuggestionList = new ArrayList<Podcast>();

        // Do filter!
        for (Podcast suggestion : suggestions)
            if (podcastList == null || !podcastList.contains(suggestion))
                filteredSuggestionList.add(suggestion);

        // Filter list and update UI
        suggestionFragment.setList(filteredSuggestionList);
    }

    @Override
    public void onSuggestionsLoadFailed() {
        suggestionFragment.showLoadFailed();
    }

    @Override
    public void onAddPodcast(Podcast suggestion) {
        podcastManager.addPodcast(suggestion);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }
}
