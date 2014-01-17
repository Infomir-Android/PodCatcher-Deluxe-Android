package net.alliknow.podcatcher.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.*;
import net.alliknow.podcatcher.PodcastActivity;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.ContextMenuListener;
import net.alliknow.podcatcher.listeners.OnDeleteDownloadsConfirmationListener;
import net.alliknow.podcatcher.listeners.PlaybackListener;
import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.services.PlayEpisodeService;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ContextMenuView extends LinearLayout
        implements AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener, View.OnHoverListener {

    public static final int ITEM_PLAY = 0;
    public static final int ITEM_REVERSE_MARKER = 1;
    public static final int ITEM_ADD_TO_PLAYLIST = 2;
    public static final int ITEM_DOWNLOAD = 3;

    private static final int HINT_APPEARS_DELAY = 500;
    private static final int HINT_DISAPPEARS_DELAY = 2000;

    ContextMenuListener episodeContextMenuListener;
    private boolean isNew;
    private boolean isInPlaylist;
    private boolean isDownloaded;
    private Episode episode;

    private ListView lvMenu;
    private LinearLayout layoutHints;
    private int hintShownForPosition = -1;

    private Timer mTimer;

    /**
     * The episode manager handle
     */
    private EpisodeManager episodeManager;

    private PlayEpisodeService service;

    public ContextMenuView(Context context) {
        super(context);
        prepare(context);
    }

    public ContextMenuView(Context context, AttributeSet set) {
        super(context, set);
        prepare(context);
    }

    public ContextMenuView(Context context, AttributeSet set, int defStyle) {
        super(context, set, defStyle);
        prepare(context);
    }

    private void prepare(Context context) {
        LayoutInflater.from(context).inflate(R.layout.context_menu, this, true);
        episodeContextMenuListener = (ContextMenuListener) context;

        lvMenu = (ListView) findViewById(R.id.context_menu_list);
        layoutHints = (LinearLayout) findViewById(R.id.layout_hints);
    }

    public void initialize(Episode episode, PlayEpisodeService service) {
        this.episode = episode;
        this.service = service;
        service.addPlayBackListener(PLAY_BACK_LISTENER);

        refreshState();

        ContextMenuAdapter adapter = new ContextMenuAdapter(formList());
        lvMenu.setAdapter(adapter);
        lvMenu.setOnItemClickListener(this);
        lvMenu.setOnItemSelectedListener(this);
        lvMenu.setOnFocusChangeListener(HIDE_HINT_FOCUS_CHANGE_LISTENER);
    }

    private void addHints() {
        layoutHints.removeAllViews();
        addHint(getContext().getString(
                service.isLoadedEpisode(episode) && service.isPlaying() ? R.string.stop : R.string.context_play
        ));
        addHint(getContext().getString(
                isNew ? R.string.context_mark_old : R.string.context_mark_new
        ));
        addHint(getContext().getString(
                isInPlaylist ? R.string.context_playlist_remove : R.string.context_playlist_add
        ));
        addHint(
                isDownloaded ?
                        getResources().getQuantityString(R.plurals.downloads_remove_title, 1) :
                        getResources().getString(R.string.context_download)
        );
    }

    private void addHint(String text) {
        TextView view = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.context_menu_hint, layoutHints, false);
        view.setText(text);
        layoutHints.addView(view);
    }

    private List<Drawable> formList() {
        List<Drawable> list = new ArrayList<Drawable>(4);
        list.add(getResources().getDrawable(
                service.isLoadedEpisode(episode) && service.isPlaying() ? R.drawable.ic_stop : R.drawable.ic_play
        ));
        list.add(getResources().getDrawable(
                isNew ? R.drawable.ic_unnew : R.drawable.ic_new
        ));
        list.add(getResources().getDrawable(
                isInPlaylist ? R.drawable.ic_remove_playlist : R.drawable.ic_playlist
        ));
        list.add(getResources().getDrawable(
                isDownloaded ? R.drawable.ic_delete : R.drawable.ic_downloaded
        ));
        addHints();
        return list;
    }

    protected void refreshState() {
        this.episodeManager = EpisodeManager.getInstance();
        this.isNew = !episodeManager.isOld(episode);
        this.isInPlaylist = episodeManager.isInPlaylist(episode);
        this.isDownloaded = episodeManager.isDownloadingOrDownloaded(episode);
    }

    protected void refreshList() {
        refreshState();
        addHints();
        int selection = lvMenu.getSelectedItemPosition();
        lvMenu.setAdapter(new ContextMenuAdapter(formList()));
        lvMenu.setSelection(selection);
    }

    final PlaybackListener PLAY_BACK_LISTENER = new PlaybackListener() {
        @Override
        public void onPlay() {
            if (service.isLoadedEpisode(episode)) {
                refreshList();
            }
        }

        @Override
        public void onPause() {
        }

        @Override
        public void onStop() {
            if (service.isLoadedEpisode(episode)) {
                refreshList();
            }
        }

        @Override
        public void onUpdateProgress(int progress) {
        }

        @Override
        public void onSetNewEpisode(Episode episode, int duration) {
            refreshList();
        }

        @Override
        public void onNothingSet() {
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PodcastActivity activity = (PodcastActivity) getContext();

        switch (position) {

            case ITEM_PLAY:
                activity.onToggleLoad();
                break;

            case ITEM_REVERSE_MARKER:
                episodeManager.setState(episode, isNew);
                activity.onStateChanged(episode);
                refreshList();
                break;

            case ITEM_DOWNLOAD:
                boolean nowRemoving = isDownloaded;
                activity.onToggleDownload(new OnDeleteDownloadsConfirmationListener() {
                    @Override
                    public void onConfirmDeletion() {
                        refreshList();
                    }

                    @Override
                    public void onCancelDeletion() {
                    }
                });
                if (!nowRemoving) {
                    refreshList();
                }
                break;

            case ITEM_ADD_TO_PLAYLIST:
                if (episodeManager.isInPlaylist(episode))
                    episodeManager.removeFromPlaylist(episode);
                else
                    episodeManager.appendToPlaylist(episode);
                activity.onPlaylistChanged();
                refreshList();
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
        if (!parent.hasFocus()) {
            return;
        }
        showHint(position);
    }


    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        if (!parent.isInTouchMode()) {
            hideHint();
        }
        // pass
    }

    @Override
    public boolean onHover(View v, MotionEvent event) {
        int position = (Integer) v.getTag();
        switch (event.getAction()) {
            case MotionEvent.ACTION_HOVER_ENTER:
                Log.v("hover", "onHover: action ENTER, position " + position);
                showHint(position);
                break;
            case MotionEvent.ACTION_HOVER_EXIT:
                Log.v("hover", "onHover: action EXIT, position " + position);
                hideHint(position);
                break;
//            default:
//                Log.v("hover", "onHover: action " + event.getAction() + ", position " + position);
        }
        return false;
    }

    private void showHint(final int position) {
        hideHint();
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (hintShownForPosition >= 0) {
                    hideHint();
                }
                layoutHints.post(new Runnable() {
                    @Override
                    public void run() {
                        layoutHints.getChildAt(position).setVisibility(VISIBLE);
                        layoutHints.setVisibility(VISIBLE);
                        hintShownForPosition = position;
                    }
                });
                mTimer = new Timer();
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        hideHint();
                    }
                }, HINT_DISAPPEARS_DELAY);
            }
        }, HINT_APPEARS_DELAY);
    }

    private void hideHint() {
        if (mTimer != null) {
            mTimer.cancel();
        }
        if (hintShownForPosition < 0) {
            return;
        }
        layoutHints.post(new Runnable() {
            @Override
            public void run() {
                if (hintShownForPosition < 0) {
                    return;
                }
                layoutHints.getChildAt(hintShownForPosition).setVisibility(INVISIBLE);
                layoutHints.setVisibility(INVISIBLE);
                hintShownForPosition = -1;
            }
        });
    }

    private void hideHint(final int position) {
        if (mTimer != null) {
            mTimer.cancel();
        }
        if (position < 0) {
            return;
        }
        layoutHints.post(new Runnable() {
            @Override
            public void run() {
                layoutHints.getChildAt(position).setVisibility(INVISIBLE);
                boolean hasVisibleHints = false;
                for (int i = 0; i < layoutHints.getChildCount(); ++i) {
                    if (layoutHints.getChildAt(i).getVisibility() == VISIBLE) {
                        hasVisibleHints = true;
                        break;
                    }
                }
                if (!hasVisibleHints) {
                    layoutHints.setVisibility(INVISIBLE);
                }
            }
        });
    }

    /**
     * Listens to whether user uses a remote and has left context menu area. If true, hides ever shown hint.
     */
    private final OnFocusChangeListener HIDE_HINT_FOCUS_CHANGE_LISTENER = new OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!lvMenu.isInTouchMode()) {
                if (lvMenu.hasFocus()) {
                    if (lvMenu.getSelectedItemPosition() >= 0) {
                        showHint(lvMenu.getSelectedItemPosition());
                    }
                } else {
                    hideHint();
                }
            }
        }
    };

    public class ContextMenuAdapter extends BaseAdapter {

        List<Drawable> items;
        LayoutInflater inflater;
        private static final int ITEM_RESOURCE = R.layout.context_menu_item;
        private static final int COMPOUND_DRAWABLE_PADDING = 5;

        public ContextMenuAdapter(List<Drawable> icons) {
            super();
            this.items = icons;
            inflater = LayoutInflater.from(getContext());
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Drawable getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v;
            if (convertView == null) {
                v = inflater.inflate(ITEM_RESOURCE, parent, false);
            } else {
                v = convertView;
            }

            ImageView imageView = (ImageView) v;
            imageView.setImageDrawable(items.get(position));

            v.setTag(position);
            v.setOnHoverListener(ContextMenuView.this);

            // lifehack caused by redrawing of all the item views when hint appears of disappears
            // (layout visibility switches from 'gone' to 'visible')
            // removing this will cause to wrong color of drawables displaying
            if (!imageView.isInTouchMode()) {
                imageView.setSelected(lvMenu.hasFocus() && lvMenu.getSelectedItemPosition() == position);
            }

            return v;
        }
    }
}
