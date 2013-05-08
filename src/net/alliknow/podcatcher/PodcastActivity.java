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

import android.app.ActionBar;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;

import net.alliknow.podcatcher.listeners.OnChangePodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;
import net.alliknow.podcatcher.view.fragments.PodcastListFragment;
import net.alliknow.podcatcher.view.fragments.PodcastListFragment.LogoViewMode;

import java.util.List;

/**
 * Our main activity class. Works as the main controller. Depending on the view
 * state, other activities cooperate.
 */
public class PodcastActivity extends EpisodeListActivity implements OnBackStackChangedListener,
        OnLoadPodcastListListener, OnChangePodcastListListener, OnSelectPodcastListener {

    /** The current podcast list fragment */
    protected PodcastListFragment podcastListFragment;

    /**
     * Flag indicating whether the app should show the add podcast dialog if the
     * list of podcasts is empty.
     */
    private boolean showAddPodcastOnEmptyPodcastList = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable strict mode when on debug
        if (((Podcatcher) getApplication()).isInDebugMode())
            StrictMode.enableDefaults();

        // 1. Create the UI via XML layouts and fragments
        // Inflate the main content view (depends on view mode)
        setContentView(R.layout.main);
        // Make sure all fragment member handles are properly set
        findFragments();
        // Add extra fragments needed in some view modes
        plugFragments();

        // 2. Register listeners (done after the fragments are available so we
        // do not end up getting call-backs without the possibility to act on
        // them).
        registerListeners();

        // 3. Init/restore the app as needed
        // If we are newly starting up and the podcast list is empty, show add
        // podcast dialog (this is used in onPodcastListLoaded(), since we only
        // know then, whether the list is actually empty. Also do not show it if
        // we are given an URL in the intent, because this will trigger the
        // dialog anyway
        showAddPodcastOnEmptyPodcastList =
                (savedInstanceState == null && getIntent().getData() == null);
        // Check if podcast list is available - if so, set it
        List<Podcast> podcastList = podcastManager.getPodcastList();
        if (podcastList != null) {
            onPodcastListLoaded(podcastList);

            // We only reset our state if the podcast list is available, because
            // otherwise we will not be able to select anything.
            if (getIntent().hasExtra(MODE_KEY))
                onNewIntent(getIntent());
        }

        // Finally we might also be called freshly with a podcast feed to add
        if (getIntent().getData() != null)
            onNewIntent(getIntent());
    }

    @Override
    protected void findFragments() {
        super.findFragments();

        // The podcast list fragment to use
        if (podcastListFragment == null)
            podcastListFragment = (PodcastListFragment) findByTagId(R.string.podcast_list_fragment_tag);
    }

    /**
     * In certain view modes, we need to add some fragments because they are not
     * set in the layout XML files. Member variables will be set if needed.
     */
    private void plugFragments() {
        // On small screens, add the podcast list fragment
        if (view.isSmall() && podcastListFragment == null) {
            podcastListFragment = new PodcastListFragment();
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.content, podcastListFragment,
                            getString(R.string.podcast_list_fragment_tag))
                    .commit();
        }
        // On small screens in landscape mode, add the episode list fragment
        if (view.isSmallLandscape() && episodeListFragment == null) {
            episodeListFragment = new EpisodeListFragment();
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.right, episodeListFragment,
                            getString(R.string.episode_list_fragment_tag))
                    .commit();
        }
    }

    @Override
    protected void registerListeners() {
        super.registerListeners();

        // Register as listener to the podcast data manager
        podcastManager.addLoadPodcastListListener(this);
        podcastManager.addChangePodcastListListener(this);
        // Make sure we are alerted on back stack changes
        getFragmentManager().addOnBackStackChangedListener(this);
    };

    @Override
    protected void onNewIntent(Intent intent) {
        // This is an external call to add a new podcast
        if (intent.getData() != null) {
            Intent addPodcast = new Intent(this, AddPodcastActivity.class);
            addPodcast.setData(intent.getData());

            // We need to cut back the selection here when is small portrait
            // mode to prevent other activities from covering the add podcast
            // dialog
            if (view.isSmallPortrait())
                selection.reset();

            startActivity(addPodcast);
            // Reset data to prevent this intent from fire again on the next
            // configuration change
            intent.setData(null);
        }
        // This is an internal call to update the selection
        else if (intent.hasExtra(MODE_KEY)) {
            selection.setMode((ContentMode) intent.getSerializableExtra(MODE_KEY));
            selection.setPodcast(podcastManager.findPodcastForUrl(
                    intent.getStringExtra(PODCAST_URL_KEY)));
            selection.setEpisode(podcastManager.findEpisodeForUrl(
                    intent.getStringExtra(EPISODE_URL_KEY)));

            // onResume() will be called after this and do the actual selection
        }
    }

    protected void onResume() {
        super.onResume();

        // Restore UI to match selection:
        // Re-select previously selected podcast(s)
        if (selection.isAll())
            onAllPodcastsSelected();
        else if (selection.isSingle() && selection.isPodcastSet())
            onPodcastSelected(selection.getPodcast());
        else if (ContentMode.DOWNLOADS.equals(selection.getMode()))
            onDownloadsSelected();
        else if (ContentMode.PLAYLIST.equals(selection.getMode()))
            onPlaylistSelected();
        else
            onNoPodcastSelected();

        // Re-select previously selected episode
        if (selection.isEpisodeSet())
            onEpisodeSelected(selection.getEpisode());
        else
            onNoEpisodeSelected();

        // Set podcast logo view mode
        updateLogoViewMode();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Make sure we persist the podcast manager state
        podcastManager.saveState();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Make sure our http cache is written to disk
        ((Podcatcher) getApplication()).flushHttpCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the listeners
        podcastManager.removeLoadPodcastListListener(this);
        podcastManager.removeChangePodcastListListener(this);
        getFragmentManager().removeOnBackStackChangedListener(this);
    }

    @Override
    public void onBackStackChanged() {
        // This only needed in small landscape mode and in case
        // we go back to the episode list
        if (view.isSmallLandscape()
                && getFragmentManager().getBackStackEntryCount() == 0) {
            onNoEpisodeSelected();
        }
    }

    @Override
    public void onPodcastListLoaded(List<Podcast> podcastList) {
        // Make podcast list show
        podcastListFragment.setPodcastList(podcastList);

        // Make action bar show number of podcasts
        updateActionBar();

        // If podcast list is empty we show dialog on startup
        if (podcastManager.size() == 0 && showAddPodcastOnEmptyPodcastList) {
            showAddPodcastOnEmptyPodcastList = false;
            startActivity(new Intent(this, AddPodcastActivity.class));
        }
    }

    @Override
    public void onPodcastAdded(Podcast podcast) {
        // Update podcast list
        podcastListFragment.setPodcastList(podcastManager.getPodcastList());
        // Update UI
        updateActionBar();

        // Set the selection to the new podcast
        if (view.isSmallPortrait())
            selection.reset();
        else {
            selection.setMode(ContentMode.SINGLE_PODCAST);
            selection.setPodcast(podcast);
            if (view.isSmallLandscape())
                selection.resetEpisode();
        }
    }

    @Override
    public void onPodcastRemoved(Podcast podcast) {
        // Update podcast list
        podcastListFragment.setPodcastList(podcastManager.getPodcastList());
        // Update UI
        updateActionBar();

        // Reset selection if deleted
        if (podcast.equals(selection.getPodcast()))
            selection.resetPodcast();
    }

    @Override
    public void onPodcastSelected(Podcast podcast) {
        super.onPodcastSelected(podcast);

        if (view.isSmallPortrait())
            showEpisodeListActivity();
        else
            // Select in podcast list
            podcastListFragment.select(podcastManager.indexOf(podcast));

        // Update UI
        updateLogoViewMode();
    }

    @Override
    public void onAllPodcastsSelected() {
        super.onAllPodcastsSelected();

        // Prepare podcast list fragment
        podcastListFragment.selectAll();

        if (view.isSmallPortrait())
            showEpisodeListActivity();

        // Update UI
        updateLogoViewMode();
    }

    @Override
    public void onDownloadsSelected() {
        super.onDownloadsSelected();

        // Prepare podcast list fragment
        podcastListFragment.selectNone();

        if (view.isSmallPortrait())
            showEpisodeListActivity();

        // Update UI
        updateLogoViewMode();
    }

    @Override
    public void onPlaylistSelected() {
        super.onPlaylistSelected();

        // Prepare podcast list fragment
        podcastListFragment.selectNone();

        if (view.isSmallPortrait())
            showEpisodeListActivity();

        // Update UI
        updateLogoViewMode();
    }

    @Override
    public void onNoPodcastSelected() {
        super.onNoPodcastSelected();

        // Reset podcast list fragment
        podcastListFragment.selectNone();
        // Update UI
        updateLogoViewMode();
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        // Only react on progress here, if the activity is visible
        if (!view.isSmallPortrait()) {
            super.onPodcastLoadProgress(podcast, progress);

            // We are in select all mode, show progress in podcast list
            if (selection.isAll())
                podcastListFragment.showProgress(podcastManager.indexOf(podcast), progress);
        }
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        // This will display the number of episodes
        podcastListFragment.refresh();

        // Tell the podcast manager to load podcast logo
        podcastManager.loadLogo(podcast);

        // In small portrait mode, work is done in separate activity
        if (!view.isSmallPortrait())
            super.onPodcastLoaded(podcast);
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast) {
        podcastListFragment.refresh();

        // In small portrait mode, work is done in separate activity
        if (!view.isSmallPortrait())
            super.onPodcastLoadFailed(failedPodcast);
    }

    @Override
    public void onPodcastLogoLoaded(Podcast podcast) {
        super.onPodcastLogoLoaded(podcast);

        updateLogoViewMode();
    }

    /**
     * Update the logo view mode according to current app state.
     */
    protected void updateLogoViewMode() {
        LogoViewMode logoViewMode = LogoViewMode.NONE;

        if (view.isLargeLandscape() && selection.isSingle())
            logoViewMode = LogoViewMode.LARGE;
        else if (view.isSmallPortrait())
            logoViewMode = LogoViewMode.SMALL;

        podcastListFragment.setLogoVisibility(logoViewMode);
    }

    @Override
    protected void updateActionBar() {
        final ActionBar bar = getActionBar();
        bar.setTitle(R.string.app_name);

        // Disable the home button (only used in overlaying activities)
        bar.setHomeButtonEnabled(false);

        if (podcastManager.getPodcastList() != null) {
            int podcastCount = podcastManager.size();
            bar.setSubtitle(podcastCount == 1 ? getString(R.string.one_podcast_selected)
                    : podcastCount + " " + getString(R.string.podcasts_selected));
        } else {
            bar.setSubtitle(null);
        }
    }

    @Override
    protected void updateDownloadUi() {
        if (!view.isSmallPortrait())
            super.updateDownloadUi();
    }

    @Override
    protected void updatePlaylistUi() {
        if (!view.isSmallPortrait())
            super.updatePlaylistUi();
    }

    @Override
    protected void updateStateUi() {
        if (!view.isSmallPortrait())
            super.updateStateUi();

        podcastListFragment.refresh();
    }

    @Override
    protected void updatePlayerUi() {
        super.updatePlayerUi();

        if (view.isSmallPortrait()) {
            playerFragment.setLoadMenuItemVisibility(false, false);
            playerFragment.setPlayerTitleVisibility(true);
        }
    }

    private void showEpisodeListActivity() {
        // We need to launch a new activity to display the episode list
        Intent intent = new Intent(this, ShowEpisodeListActivity.class);

        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
    }
}
