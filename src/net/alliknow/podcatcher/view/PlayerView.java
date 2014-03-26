package net.alliknow.podcatcher.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.PlaybackListener;
import net.alliknow.podcatcher.model.types.Episode;

public class PlayerView extends RelativeLayout implements PlaybackListener {

    protected TextView tvTitle;
    protected TextView tvCurrentTime;
    protected TextView tvFullTime;
    protected ProgressBar progressBar;

    public static String secondsToString(int seconds) {
        if (seconds <= 0) {
            return "00:00:00";
        }
        int k = seconds;
        int s = k % 60;
        k /= 60;
        int m = k % 60;
        k /= 60;
        return unitToString(k) + ":" + unitToString(m) + ":" + unitToString(s);

    }

    public static int stringToSecond(String string) {
        String[] parts = string.split(":");
        int seconds = 0;
        for (int i = parts.length - 1, k = 1; i >= 0; --i, k *= 60) {
            seconds += Integer.valueOf(parts[i]) * k;
        }
        return seconds;
    }

    public static String unitToString(int unit) {
        if (unit <= 0) {
            return "00";
        }
        return (unit <= 9 ? "0" : "") + unit;
    }

    public PlayerView(Context context) {
        super(context);
        inflateView();
    }

    public PlayerView(Context context, AttributeSet attr) {
        super(context, attr);
        inflateView();
    }

    protected void inflateView() {
        View.inflate(getContext(), R.layout.player_view, this);
    }

    @Override
    protected void onFinishInflate() {
        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvCurrentTime = (TextView) findViewById(R.id.tv_current_time);
        tvFullTime = (TextView) findViewById(R.id.tv_full_time);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        setEmptyInfo();
        progressBar.setEnabled(false);
    }

    @Override
    public void onSetNewEpisode(final Episode episode, final int duration) {
        post(new Runnable() {
            @Override
            public void run() {
                int seconds = episode.getDuration();
                if (seconds < 0) {
                    seconds = duration / 1000;       // convert from milliseconds to seconds
//                    seconds /= 1000;
                }
                if (tvTitle != null) {
                    tvTitle.setText(episode.getName());
                }
                progressBar.setMax(seconds);
                progressBar.setProgress(0);
                tvCurrentTime.setText(secondsToString(0));
                tvFullTime.setText(secondsToString(seconds));
            }
        });
    }

    @Override
    public void onUpdateProgress(int position) {
        final int progress = position / 1000;
        post(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(progress);
                tvCurrentTime.setText(secondsToString(progress));
            }
        });
    }

    @Override
    public void onNothingSet() {
        post(new Runnable() {
            @Override
            public void run() {
                setEmptyInfo();
            }
        });
    }

    @Override
    public void onPlay() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onStop() {
        post(new Runnable() {
            @Override
            public void run() {
                setEmptyInfo();
            }
        });
    }

    protected void setEmptyInfo() {
        progressBar.setProgress(0);
        tvCurrentTime.setText(secondsToString(0));
        tvFullTime.setText(secondsToString(0));
        if (tvTitle != null) {
            tvTitle.setText("");
        }
    }

}