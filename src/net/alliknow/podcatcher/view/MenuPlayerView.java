package net.alliknow.podcatcher.view;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.PlaybackListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.services.PlayEpisodeService;

public class MenuPlayerView extends LinearLayout implements PlaybackListener, PlayPauseSeekbar.ControlListener,
        PlayPauseSeekbar.OnKeyUpListener {

    protected EditText tvCurrentTime;
    protected TextView tvFullTime;
    protected PlayPauseSeekbar seekbar;

    protected PlayEpisodeService service;

    InputMethodManager inputMethodManager;

    private boolean editingStarted = false;

    public MenuPlayerView(Context context) {
        super(context);
        inflateView();
    }

    public MenuPlayerView(Context context, AttributeSet set) {
        super(context, set);
        inflateView();
    }

    protected void inflateView() {
        View.inflate(getContext(), R.layout.menu_seekbar, this);
    }

    public void connectToService(PlayEpisodeService service) {
        this.service = service;
        service.addPlayBackListener(this);
//        seekbar.setEnabled(true);
        setEnabled(true);
    }

    public void disconnectFromService() {
        this.service = null;
        seekbar.onPause();
//        seekbar.setEnabled(false);
        setEnabled(false);
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (Character.isDigit(event.getUnicodeChar())) {
//            return tvCurrentTime.onKeyDown(keyCode, event);
//        }
//        return super.onKeyDown(keyCode, event);
//    }

    private void trySeekToEntered() {
        inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
//        tvCurrentTime.setFocusableInTouchMode(false);
        int second = PlayerView.stringToSecond(tvCurrentTime.getText().toString());
        long ms = second * 1000;
        if (service.getDuration() > ms) {
            onSeekTo(second);
        } else {
            tvCurrentTime.setText(PlayerView.secondsToString(service.getCurrentPosition() / 1000));
        }
        finishEditing();
    }

    private void startEditing() {
        if (!editingStarted) {
            editingStarted = true;
            tvCurrentTime.setText(PlayerView.secondsToString(0));
        }
    }

    private void finishEditing() {
        editingStarted = false;
    }

    @Override
    public void onKeyUpRaised(int keyCode, KeyEvent event) {
        if (service == null || !service.isPrepared()) {
            return;
        }
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            trySeekToEntered();
            return;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            onPlayPressed();
            finishEditing();
            return;
        }
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            tvCurrentTime.setText(shiftToRight(tvCurrentTime.getText()));
            return;
        }
        char c = (char) event.getUnicodeChar();
        if (!Character.isDigit(c)) {
            return;
        }
        onPausePressed();
        startEditing();

        Editable text = tvCurrentTime.getText();
        text = shiftToLeft(text, c);
        tvCurrentTime.setText(text);
    }

    private Editable shiftToLeft(Editable text, char newChar) {
        String result = text.toString().replaceAll(":", "");
        result = result.substring(1);
        Editable editable = Editable.Factory.getInstance().newEditable(result);
        editable = editable.append(newChar);
        editable.insert(editable.length() - 2, ":");
        editable.insert(editable.length() - 5, ":");
        return editable;
    }

    private Editable shiftToRight(Editable text) {
        String result = text.toString().replaceAll(":", "");
        result = result.substring(0, result.length() - 1);
        result = "0" + result;
        Editable editable = Editable.Factory.getInstance().newEditable(result);
        editable.insert(editable.length() - 2, ":");
        editable.insert(editable.length() - 5, ":");
        return editable;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        seekbar.setEnabled(enabled);
        tvCurrentTime.setEnabled(enabled);
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        return seekbar.requestFocus();
    }

    /**
     * PLAY_PAUSE_SEEKBAR.ON_CONTROL_LISTENER *
     */

    @Override
    public void onPlayPressed() {
        if (service != null && service.isPrepared()) {
            service.start();
        }
        finishEditing();
    }

    @Override
    public void onPausePressed() {
        if (service != null && service.isPlaying()) {
            service.pause();
        }
    }

    @Override
    public void onSeekTo(int position) {
        if (service != null) {
            int milli = position * 1000;
            if (service.getDuration() <= milli) {
                milli = service.getDuration() - 1;
            }
            service.seekTo(milli);
        }
    }

    /**
     * ***********************************************
     */

    private void endEdit() {
        seekbar.requestFocus();
    }

    @Override
    protected void onFinishInflate() {
        inputMethodManager = InputMethodManager.getInstance(getContext());

        tvCurrentTime = (EditText) findViewById(R.id.tv_current_time);
//        tvCurrentTime.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onPausePressed();
//            }
//        });
        tvCurrentTime.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    onPausePressed();
                    startEditing();
                }
            }
        });
        tvCurrentTime.setFilters(new InputFilter[]{CURRENT_TIME_INPUT_FILTER});
        tvCurrentTime.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_UP) {
                    return true;
                }
                int cursor = tvCurrentTime.getSelectionStart();
                if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    trySeekToEntered();
                    onPlayPressed();
                    endEdit();
                    finishEditing();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    onPlayPressed();
                    endEdit();
                    finishEditing();
                    return true;
                }
                char c = (char) event.getUnicodeChar();
                if (!Character.isDigit(c)) {
                    return false;
                }
                Editable text = tvCurrentTime.getText();
                if (text.charAt(cursor) == ':') {
                    cursor++;
                    tvCurrentTime.setSelection(cursor);
                    return onKey(v, keyCode, event);
                }
                text = text.replace(cursor, cursor + 1, String.valueOf(c));
                tvCurrentTime.setText(text);
                tvCurrentTime.setSelection(cursor + 1);
                return true;
            }
        });

        tvFullTime = (TextView) findViewById(R.id.tv_full_time);
        seekbar = (PlayPauseSeekbar) findViewById(R.id.play_pause_seekbar);

        seekbar.setListener(this);
        seekbar.setOnKeyUpListener(this);

        setEmptyInfo();
    }

    private final InputFilter CURRENT_TIME_INPUT_FILTER = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            String result = dest.subSequence(0, dstart)
                    + source.subSequence(start, end).toString()
                    + dest.subSequence(dend, dest.length());
            final String PATTERN = "[0-9]+:[0-9]{2}:[0-9]{2}";
            if (result.matches(PATTERN)) {
                return source.subSequence(start, end);
            } else {
                return dest.subSequence(dstart, dend);
            }
        }
    };

    protected void setEmptyInfo() {
        seekbar.onProgressUpdate(0);
        tvCurrentTime.setText(PlayerView.secondsToString(0));
        tvFullTime.setText(PlayerView.secondsToString(0));
    }

    private void togglePlayer() {
        if (service.isPlaying()) {
            service.pause();
        } else {
            service.start();
        }
    }

    @Override
    public void onSetNewEpisode(final Episode episode, final int duration) {
        post(new Runnable() {
            @Override
            public void run() {
//                seekbar.setEnabled(true);
                setEnabled(true);
                int seconds = episode.getDuration();
                if (seconds < 0) {
                    seconds = duration / 1000;       // convert from milliseconds to seconds
                }
                seekbar.setMax(seconds);
                seekbar.onProgressUpdate(0);

                tvCurrentTime.setText(PlayerView.secondsToString(0));
//                tvCurrentTime.setEnabled(true);

                tvFullTime.setText(PlayerView.secondsToString(seconds));
            }
        });
    }

    @Override
    public void onNothingSet() {
        setEmptyInfo();
        setEnabled(false);
    }

    @Override
    public void onUpdateProgress(int progress) {
        final int sec = progress / 1000;
        seekbar.onProgressUpdate(sec);
        tvCurrentTime.post(new Runnable() {
            @Override
            public void run() {
                tvCurrentTime.setText(PlayerView.secondsToString(sec));
            }
        });
    }

    @Override
    public void onPlay() {
        setEnabled(true);
        seekbar.onPlay();
    }

    @Override
    public void onPause() {
        seekbar.onPause();
    }

    @Override
    public void onStop() {
        seekbar.onPause();
        seekbar.onProgressUpdate(0);
        tvCurrentTime.setText(PlayerView.secondsToString(0));
    }
}
