package com.pr0gramm.app.ui.views.viewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import com.pr0gramm.app.R;
import com.pr0gramm.app.Settings;
import com.pr0gramm.app.ui.views.BusyIndicator;
import com.squareup.picasso.Downloader;

import javax.inject.Inject;

import pl.droidsonroids.gif.GifDrawable;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import static com.pr0gramm.app.AndroidUtility.checkMainThread;
import static com.pr0gramm.app.ui.dialogs.ErrorDialogFragment.defaultOnError;

/**
 */
@SuppressLint("ViewConstructor")
public class GifMediaView extends MediaView {
    @Inject
    private Downloader downloader;

    @Inject
    private Settings settings;

    @InjectView(R.id.image)
    private ImageView imageView;

    // the gif that is shown
    private GifDrawable gif;

    private Subscription dlGifSubscription;

    public GifMediaView(Context context, Binder binder, MediaUri url, Runnable onViewListener) {
        super(context, binder, R.layout.player_gif, url, onViewListener);

        loadGif();
    }

    private void loadGif() {
        Observable<GifLoader.DownloadStatus> loader = GifLoader
                .loader(downloader, getContext().getCacheDir(), getEffectiveUri())
                .subscribeOn(Schedulers.io());

        dlGifSubscription = binder.bind(loader).subscribe(state -> {
            onDownloadProgress(state.getProgress());

            if (state.isFinished()) {
                logger.info("loading finished");

                hideBusyIndicator();

                gif = state.getDrawable();
                imageView.setImageDrawable(this.gif);
                setViewAspect((float) gif.getIntrinsicWidth() / gif.getIntrinsicHeight());

                if (isPlaying()) {
                    onMediaShown();
                } else {
                    gif.stop();
                }
            }
        }, defaultOnError());
    }

    private void onDownloadProgress(float progress) {
        checkMainThread();

        // logger.info("Download at " + ((int) (100 * progress)) + " percent.");

        View progressView = getProgressView();
        if (progressView instanceof BusyIndicator) {
            BusyIndicator bar = (BusyIndicator) progressView;
            bar.setProgress(progress);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (gif != null && isPlaying()) {
            gif.start();
            onMediaShown();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (gif != null && isPlaying())
            gif.pause();
    }

    @Override
    public void playMedia() {
        super.playMedia();
        if (gif != null && isPlaying())
            gif.start();
    }

    @Override
    public void stopMedia() {
        super.stopMedia();
        if (gif != null)
            gif.stop();
    }

    @Override
    public void onDestroy() {
        // unsubscribe and cancel downloader
        if (dlGifSubscription != null)
            dlGifSubscription.unsubscribe();

        imageView.setImageDrawable(null);

        if (gif != null)
            gif.recycle();

        super.onDestroy();
    }
}
