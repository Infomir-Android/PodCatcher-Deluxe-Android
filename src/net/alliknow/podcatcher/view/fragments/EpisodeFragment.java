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

package net.alliknow.podcatcher.view.fragments;

import static android.view.View.FOCUSABLES_ALL;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.alliknow.podcatcher.PodcastActivity;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.adapters.EpisodeListAdapter;
import net.alliknow.podcatcher.listeners.OnDownloadEpisodeListener;
import net.alliknow.podcatcher.listeners.OnRequestFullscreenListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.services.PlayEpisodeService;
import net.alliknow.podcatcher.view.AnimatedListView;
import net.alliknow.podcatcher.view.ContextMenuView;
import net.alliknow.podcatcher.view.Utils;

/**
 * Fragment showing episode details.
 */
public class EpisodeFragment extends Fragment implements View.OnFocusChangeListener, View.OnKeyListener {

    /** Delay after the fullscreen hint is take off the video */
//    private static final int HIDE_FULLSCREEN_HINT_DELAY = 3000;

    /**
     * The listener for the menu item
     */
    private OnDownloadEpisodeListener downloadListener;
    /**
     * The listener for the video view
     */
    private OnRequestFullscreenListener fullscreenListener;
    /**
     * The currently shown episode
     */
    private Episode currentEpisode;

    private PlayEpisodeService service;

    /**
     * Flag for show download menu item state
     */
    private boolean showDownloadMenuItem = false;
    /**
     * Flag for the state of the download menu item
     */
    private boolean downloadMenuItemState = true;
    /**
     * Flag to indicate whether the episode date should be shown
     */
    private boolean showEpisodeDate = false;
    /**
     * Flag for show new icon state
     */
    private boolean showNewStateIcon = false;
    /**
     * Flag for show download icon state
     */
    private boolean showDownloadIcon = false;
    /**
     * Flag for the state of the download icon
     */
    private boolean downloadIconState = true;
    /**
     * Flag for the video view visibility
     */
    private boolean showVideo = false;
    /**
     * Flag for the video fill space option
     */
    private boolean videoFillsSpace = false;

    /**
     * Separator for date and podcast name
     */
    private static final String SEPARATOR = " • ";

    /**
     * Status flag indicating that our view is created
     */
    private boolean viewCreated = false;
    /**
     * Flag for transition animation fix
     */
    private boolean needsLayoutTransitionFix = true;

    /**
     * The download episode menu bar item
     */
    private MenuItem downloadMenuItem;

    /**
     * The empty view
     */
    private View emptyView;
    /**
     * The episode title view
     */
    private TextView titleView;
    /**
     * The podcast title view
     */
    private TextView subtitleView;
    /**
     * The state icon view
     */
    private ImageView stateIconView;
    /**
     * The download icon view
     */
    private ImageView downloadIconView;
    /**
     * The divider view between title and description
     */
    private View dividerView;
    private ImageView logoImageView;

    private ContextMenuView contextMenuView;
    /** The actual video surface */
//    private SurfaceView surfaceView;
    /** The go to fullscreen icon */
//    private ImageView fullscreenButton;
    /**
     * The episode description web view
     */
    private WebView descriptionView;

    private ViewGroup descriptionLayout;

    /** Flag to indicate whether video surface is available */
//    private boolean videoSurfaceAvailable = false;
    /** Our video surface holder callback to update availability */
//    private VideoCallback videoCallback = new VideoCallback();

    /**
     * The callback implementation
     */
//    private final class VideoCallback implements SurfaceHolder.Callback {
//
//        @Override
//        public void surfaceCreated(SurfaceHolder holder) {
//            videoSurfaceAvailable = true;
//
//            // We need to hide the fullscreen hint after a while
//            new Handler().postDelayed(new Runnable() {
//
//                @Override
//                public void run() {
//                    fullscreenButton.setVisibility(GONE);
//                }
//            }, HIDE_FULLSCREEN_HINT_DELAY);
//        }
//
//        @Override
//        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//            // pass
//        }
//
//        @Override
//        public void surfaceDestroyed(SurfaceHolder holder) {
//            videoSurfaceAvailable = false;
//        }
//    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.downloadListener = (OnDownloadEpisodeListener) activity;
            this.fullscreenListener = (OnRequestFullscreenListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnDownloadEpisodeListener and OnRequestFullscreenListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.episode, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find UI widgets
        emptyView = getView().findViewById(android.R.id.empty);
        titleView = (TextView) getView().findViewById(R.id.episode_title);
        subtitleView = (TextView) getView().findViewById(R.id.podcast_title);
        stateIconView = (ImageView) getView().findViewById(R.id.state_icon);
        downloadIconView = (ImageView) getView().findViewById(R.id.download_icon);
        dividerView = getView().findViewById(R.id.episode_divider);
        logoImageView = (ImageView) getView().findViewById(R.id.logo);

//        videoView = getView().findViewById(R.id.episode_video);
//        videoView.setOnTouchListener(new OnTouchListener() {
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (event.getAction() == MotionEvent.ACTION_DOWN)
//                    fullscreenListener.onRequestFullscreen();
//
//                return event.getAction() == MotionEvent.ACTION_DOWN;
//            }
//        });

//        surfaceView = (SurfaceView) getView().findViewById(R.id.surface);
//        surfaceView.getHolder().addCallback(videoCallback);
//        fullscreenButton = (ImageView) getView().findViewById(R.id.fullscreen);

        descriptionView = (WebView) getView().findViewById(R.id.episode_description);
        descriptionLayout = (ViewGroup) getView().findViewById(R.id.description_layout);

        contextMenuView = (ContextMenuView) getView().findViewById(R.id.context_menu);

        viewCreated = true;

        // This will make sure we show the right information once the view
        // controls are established (the episode might have been set earlier)
        if (currentEpisode != null && service != null) {
            setEpisode(currentEpisode, service);
            setNewIconVisibility(showNewStateIcon);
            setDownloadIconVisibility(showDownloadIcon, downloadIconState);
            setShowVideoView(showVideo, videoFillsSpace);
        }

        contextMenuView.setOnFocusChangeListener(this);
        descriptionView.setOnFocusChangeListener(this);
        getView().setOnKeyListener(this);
        descriptionLayout.setOnKeyListener(this);
        descriptionView.setOnHoverListener(UPDATE_BACKGROUND_ON_HOVER_LISTENER);

//        contextMenuView.setNextFocusDownId(R.id.episode_description);
//        descriptionView.setNextFocusUpId(R.id.context_menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.episode, menu);

        downloadMenuItem = menu.findItem(R.id.episode_download_menuitem);
        setDownloadMenuItemVisibility(showDownloadMenuItem, downloadMenuItemState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.episode_download_menuitem:
                // Tell activity to load/unload the current episode
                downloadListener.onToggleDownload(null);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroyView() {
//        surfaceView.getHolder().removeCallback(videoCallback);
        viewCreated = false;

        super.onDestroyView();
    }

    /**
     * Set the displayed episode, all UI will be updated.
     *
     * @param selectedEpisode Episode to show.
     */
    public void setEpisode(Episode selectedEpisode, PlayEpisodeService service) {
        // Set handle to episode in case we are not resumed
        this.currentEpisode = selectedEpisode;

        this.contextMenuView.initialize(selectedEpisode, service);

        // If the fragment's view is actually visible and the episode is
        // valid,
        // show episode information
        if (viewCreated && currentEpisode != null) {
            // Title and sub-title
            titleView.setText(currentEpisode.getName());
            subtitleView.setText(currentEpisode.getPodcast().getName());
            // Set logo
            // TODO: logo!
//            if (selectedEpisode.getLogo() != null) {
//                logoImageView.setVisibility(VISIBLE);
//                logoImageView.setImageBitmap(selectedEpisode.getLogo());
//            } else {
//                logoImageView.setVisibility(GONE);
//            }
            // Episode publication data
            if (showEpisodeDate && currentEpisode.getPubDate() != null)
                subtitleView.setText(subtitleView.getText() + SEPARATOR
                        + Utils.getRelativePubDate(currentEpisode));
            // Episode duration
            if (currentEpisode.getDurationString() != null)
                subtitleView.setText(subtitleView.getText() + SEPARATOR
                        + currentEpisode.getDurationString());
            // Find valid episode description
            String description = currentEpisode.getLongDescription();
            if (description == null)
                description = currentEpisode.getDescription();
            if (description == null)
                description = getString(R.string.episode_no_description);
            // Set episode description
            descriptionView.loadDataWithBaseURL(null, description, "text/html", "utf-8", null);

//            descriptionView.setBackgroundColor(0x000000);
            WebSettings settings = descriptionView.getSettings();
            settings.setDefaultFontSize(getResources().getDimensionPixelSize(R.dimen.font_size_default));
            settings.setMinimumFontSize(getResources().getDimensionPixelSize(R.dimen.font_size_small));

            descriptionView.setBackgroundColor(0);
//            descriptionView.setLayerType(WebView.LAYER_TYPE_NONE, descriptionView.getPaint());
            updateDescriptionViewBackground(descriptionView.hasFocus());

        }

        // Update the UI widget's visibility to reflect state
        updateUiElementVisibility();

        // This is a workaround for the fact that declaring animateLayoutChanges
        // in combination with a webview breaks the fragment on some devices
        // (such as the HP Touchpad). Activating the layout transition after the
        // view has been shown once, works.
        if (needsLayoutTransitionFix && viewCreated) {
            ViewGroup parent = (ViewGroup) getView().getParent();

            // In small view we need to go two steps up
            if (!(parent instanceof LinearLayout))
                parent = (ViewGroup) parent.getParent();

            if (parent.getLayoutTransition() == null) {
                parent.setLayoutTransition(new LayoutTransition());
                needsLayoutTransitionFix = false;
            }
        }
    }

    private final View.OnHoverListener UPDATE_BACKGROUND_ON_HOVER_LISTENER = new View.OnHoverListener() {
        @Override
        public boolean onHover(View v, MotionEvent event) {
            // In the case we use first remote, then mouse -- change background of the description elements.
            switch (event.getAction()) {
                case MotionEvent.ACTION_HOVER_ENTER:
                    updateDescriptionViewBackground(true);
                    break;
//                case MotionEvent.ACTION_HOVER_EXIT:
//                    updateDescriptionViewBackground(false);
//                    break;
            }
            return false;
        }
    };

    public void updateDescriptionViewBackground(boolean focused) {
        int color;
        if (descriptionView.isInTouchMode()) {
            color = getResources().getColor(R.color.description_web_view_bg_focused);
        } else {
            if (focused) {
                color = getResources().getColor(R.color.description_web_view_bg_focused);
            } else {
                color = getResources().getColor(R.color.description_web_view_bg_unfocused);
            }
        }
        descriptionLayout.setBackgroundColor(color);
    }

    public void focusOnMenu() {
//        if (contextMenuView != null)
//            contextMenuView.requestFocus();
        onFocusChange(contextMenuView, true);
        if (contextMenuView != null)
            contextMenuView.requestFocus();
    }

    /**
     * Set whether the fragment should show the download menu item. You can call
     * this any time and can expect it to happen on menu creation at the latest.
     * You also have to set the download menu state, <code>true</code> for
     * "Download" and <code>false</code> for "Delete".
     *
     * @param show     Whether to show the download menu item.
     * @param download State of the download menu item (download / delete)
     */
    public void setDownloadMenuItemVisibility(boolean show, boolean download) {
        this.showDownloadMenuItem = show;
        this.downloadMenuItemState = download;

        // Only do it right away if resumed and menu item is available,
        // otherwise onResume or the menu creation callback will call us.
        if (downloadMenuItem != null) {
            downloadMenuItem.setVisible(show);

            downloadMenuItem.setTitle(download ? R.string.download : R.string.remove);
            downloadMenuItem.setIcon(download ? R.drawable.ic_menu_download
                    : R.drawable.ic_menu_delete);
        }
    }

    /**
     * Set whether the fragment should show the episode state icon to indicate
     * that the episode is new (not marked old).
     *
     * @param show Whether to show the new icon.
     */
    public void setNewIconVisibility(boolean show) {
        this.showNewStateIcon = show;

        if (viewCreated)
            stateIconView.setVisibility(show ? VISIBLE : GONE);
    }

    /**
     * Set whether the fragment should show the download icon. You can call this
     * any time and can expect it to happen on fragment resume at the latest.
     * You also have to set the download icon state, <code>true</code> for
     * "is downloaded" and <code>false</code> for "is currently downloading".
     *
     * @param show       Whether to show the download menu item.
     * @param downloaded State of the download menu item (download / delete)
     */
    public void setDownloadIconVisibility(boolean show, boolean downloaded) {
        this.showDownloadIcon = show;
        this.downloadIconState = downloaded;

        // Only do it right away if resumed and menu item is available,
        // otherwise onResume or the menu creation callback will call us.
        if (viewCreated) {
            downloadIconView.setVisibility(show ? VISIBLE : GONE);
//            downloadIconView.setImageResource(downloaded ?
//                    R.drawable.ic_media_downloaded : R.drawable.ic_media_downloading);
        }
    }

    /**
     * Set whether the fragment should show the episode date for the episode
     * shown. Change will be reflected upon next call of
     * {@link #setEpisode(Episode, PlayEpisodeService)}
     *
     * @param show Whether to show the episode date.
     */
    public void setShowEpisodeDate(boolean show) {
        this.showEpisodeDate = show;
    }

    /**
     * Set whether the fragment should make the video surface visible. If the
     * view is created, the effect will be immediate.
     *
     * @param showVideo Set to <code>true</code> to make the video surface
     *                  appear.
     * @param fillSpace If set to <code>true</code>, the video view will consume
     *                  all the space available and the episode description is hidden.
     */
    public void setShowVideoView(boolean showVideo, boolean fillSpace) {
        this.showVideo = showVideo;
        this.videoFillsSpace = fillSpace;

        if (viewCreated)
            updateUiElementVisibility();
    }

//    @Override
//    public SurfaceHolder getVideoSurface() {
//        return surfaceView.getHolder();
//    }

//    @Override
//    public boolean isVideoSurfaceAvailable() {
//        return videoSurfaceAvailable;
//    }

//    @Override
//    public void adjustToVideoSize(int width, int height) {
//        if (viewCreated) {
//            LinearLayout.LayoutParams layoutParams =
//                    (LinearLayout.LayoutParams) videoView.getLayoutParams();
//
//            if (videoFillsSpace) {
//                layoutParams.height = 0;
//                layoutParams.weight = 1;
//            }
//            else {
//                layoutParams.height = (int) (((float) height / (float) width) *
//                        (float) getView().getWidth());
//                layoutParams.weight = 0;
//            }
//
//            videoView.setLayoutParams(layoutParams);
//        }
//    }

    private void updateUiElementVisibility() {
        if (viewCreated) {
            emptyView.setVisibility(currentEpisode == null ? VISIBLE : GONE);

            titleView.setVisibility(currentEpisode == null ? GONE : VISIBLE);
            subtitleView.setVisibility(currentEpisode == null ? GONE : VISIBLE);
            dividerView.setVisibility(currentEpisode == null ? GONE : VISIBLE);
//            videoView.setVisibility(showVideo ? VISIBLE : GONE);
            descriptionView.setVisibility(currentEpisode == null ||
                    (showVideo && videoFillsSpace) ? GONE : VISIBLE);
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
//            focusOnMenu();
            ((PodcastActivity) getActivity()).fragmentSelected(this);
        }
        if (!v.isInTouchMode()) {
            if (v == descriptionView) {
                updateDescriptionViewBackground(hasFocus);
            } else {
                updateDescriptionViewBackground(false);
            }
        }
    }

    public void updateBackground(boolean hasFocus) {
        int bgResource;
        if (hasFocus) {
            bgResource = R.color.fragment_bg_focused;
        } else {
            bgResource = R.color.fragment_bg_default;
        }
        getView().setBackgroundColor(getResources().getColor(bgResource));
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (contextMenuView.hasFocus()) {
                    descriptionView.requestFocus();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (descriptionView.hasFocus()) {
                    contextMenuView.requestFocus();
                    return true;
                }
        }
        return false;
    }
}
