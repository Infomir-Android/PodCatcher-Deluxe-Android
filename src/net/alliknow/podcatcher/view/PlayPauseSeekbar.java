package net.alliknow.podcatcher.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import net.alliknow.podcatcher.R;

public class PlayPauseSeekbar extends AbsoluteLayout {

    protected SeekBar seekBar;
    protected ImageButton thumb;

    protected ControlListener listener;
    protected OnKeyUpListener onKeyUpListener;

    protected int thumbStatus;
    protected boolean playing;

    public static final int STATUS_MINIMIZED = 0;
    public static final int STATUS_PLAY = 1;
    public static final int STATUS_PAUSE = 2;

    public PlayPauseSeekbar(Context context) {
        super(context);
        prepare();
    }

    public PlayPauseSeekbar(Context context, AttributeSet set) {
        super(context, set);
        prepare();
    }

    private void prepare() {
        View.inflate(getContext(), R.layout.play_pause_seekbar, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        seekBar = (SeekBar) findViewById(R.id.seekbar);
        thumb = (ImageButton) findViewById(R.id.thumb);

        seekBar.setOnSeekBarChangeListener(ON_SEEK_BAR_CHANGE_LISTENER);
        thumb.setOnClickListener(THUMB_BUTTON_ON_CLICK_LISTENER);

        // just don't touch onKeyDown events
        seekBar.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    return onKeyDown(keyCode, event);
                }
                return false;
            }
        });
        seekBar.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    tryChangeSeekBarThumb(STATUS_MINIMIZED);
                } else {
                    tryChangeSeekBarThumb(playing ? STATUS_PAUSE : STATUS_PLAY);
                }
            }
        });
    }

    public void setListener(ControlListener listener) {
        this.listener = listener;
    }

    public void setOnKeyUpListener(OnKeyUpListener listener) {
        this.onKeyUpListener = listener;
    }

    private void tryChangeSeekBarThumb(int statusTo) {
        switch (statusTo) {
            case STATUS_PLAY:
                if (!needsToExtendThumb()) {
                    if (thumbStatus != STATUS_MINIMIZED) {
                        tryChangeSeekBarThumb(STATUS_MINIMIZED);
                    }
                    return;
                }
                thumb.setImageResource(R.drawable.thumb_play);
                thumbStatus = STATUS_PLAY;
                break;
            case STATUS_PAUSE:
                if (!needsToExtendThumb()) {
                    if (thumbStatus != STATUS_MINIMIZED) {
                        tryChangeSeekBarThumb(STATUS_MINIMIZED);
                    }
                    return;
                }
                thumb.setImageResource(R.drawable.thumb_pause);
                thumbStatus = STATUS_PAUSE;
                break;
            default:
                thumb.setImageResource(R.drawable.menu_seekbar_thumb_default);
                thumbStatus = STATUS_MINIMIZED;
        }
    }

    public boolean needsToExtendThumb() {
        return isInTouchMode() || hasFocus();
    }

    private void relocateThumb(int progress) {
        double percent = ((double) progress) / ((double) seekBar.getMax());
        if (percent > 1.0) {
            percent = 1.0;
        }
        int width = getMeasuredWidth();
        int thumbWidth = thumb.getMeasuredWidth();
//        ((LayoutParams) thumb.getLayoutParams()).x = (int) (percent * (double) width);
        LayoutParams params = (LayoutParams) thumb.getLayoutParams();
        params.x = (int) (percent * (double) (width - thumbWidth));
        thumb.setLayoutParams(params);
    }

    /** HERE VIEW LISTENS TO PLAYBACK **/

    /**
     * Method to be called when service starts playing.
     * Updates layout.
     */
    public void onPlay() {
        tryChangeSeekBarThumb(STATUS_PAUSE);
        playing = true;
    }

    /**
     * Method to be called when service pauses.
     * Updates layout.
     */
    public void onPause() {
        tryChangeSeekBarThumb(STATUS_PLAY);
        playing = false;
    }

    /**
     * Method to be called when service progress changes.
     * Updates layout.
     */
    public void onProgressUpdate(final int progress) {
        seekBar.setProgress(progress);
        thumb.post(new Runnable() {
            @Override
            public void run() {
                relocateThumb(progress);
            }
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
//        if (enabled) {
//            seekBar.setEnabled(true);
//            thumb.setEnabled(true);
//        } else {
//            seekBar.setProgress(0);
//            tryChangeSeekBarThumb(STATUS_MINIMIZED);
//            seekBar.setEnabled(false);
//            thumb.setEnabled(false);
//        }
//        super.setEnabled(enabled);
    }

    /**
     * Method to be called when service is prepared with new track.
     * Updates layout.
     */
    public void setMax(int max) {
        seekBar.setMax(max);
    }

    /**
     * HERE VIEW CONTROLS PLAYBACK *
     */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (onKeyUpListener != null) {
            onKeyUpListener.onKeyUpRaised(keyCode, new KeyEvent(
                    event.getDownTime(),
                    event.getEventTime(),
                    KeyEvent.ACTION_UP,
                    keyCode,
                    event.getRepeatCount(),
                    event.getMetaState()
            ));
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                seekBar.onKeyDown(keyCode, event);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                thumb.callOnClick();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    protected final OnClickListener THUMB_BUTTON_ON_CLICK_LISTENER = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (thumbStatus == STATUS_PLAY) {
                listener.onPlayPressed();
            } else if (thumbStatus == STATUS_PAUSE) {
                listener.onPausePressed();
            }
        }
    };

    protected final SeekBar.OnSeekBarChangeListener ON_SEEK_BAR_CHANGE_LISTENER = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                listener.onSeekTo(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        tryChangeSeekBarThumb(playing ? STATUS_PAUSE : STATUS_PLAY);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        return super.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (!gainFocus) {
            tryChangeSeekBarThumb(STATUS_MINIMIZED);
        } else {
            tryChangeSeekBarThumb(playing ? STATUS_PAUSE : STATUS_PLAY);
        }
    }

    /**
     * Interface which listener implements in order to receive callbacks from user actions.
     */
    public static interface ControlListener {
        void onPlayPressed();

        void onPausePressed();

        void onSeekTo(int position);
    }

    public static interface OnKeyUpListener {
        void onKeyUpRaised(int keyCode, KeyEvent event);
    }
}
