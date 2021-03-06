package com.icapps.vkmusic.fragment;

import android.databinding.DataBindingUtil;
import android.databinding.ObservableBoolean;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.icapps.vkmusic.R;
import com.icapps.vkmusic.VkApplication;
import com.icapps.vkmusic.base.BaseMusicFragment;
import com.icapps.vkmusic.databinding.FragmentNowPlayingBinding;
import com.icapps.vkmusic.service.MusicService;
import com.icapps.vkmusic.util.GraphicsUtil;
import com.vk.sdk.api.model.VKApiAudio;

import javax.inject.Inject;
import javax.inject.Named;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class NowPlayingFragment extends BaseMusicFragment {
    @Inject
    @Named("shuffle")
    ObservableBoolean shuffleSetting;
    @Inject
    @Named("repeat")
    ObservableBoolean repeatSetting;

    private FragmentNowPlayingBinding binding;

    public NowPlayingFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_now_playing, container, false);

        binding.next.setOnClickListener(v -> onNextClicked());
        binding.previous.setOnClickListener(v -> onPreviousClicked());
        binding.playPause.setOnClickListener(v -> onPlayPauseClicked());
        binding.playPauseTop.setOnClickListener(v -> onPlayPauseClicked());
        binding.shuffle.setOnClickListener(v -> onShuffleClicked());
        binding.repeat.setOnClickListener(v -> onRepeatClicked());
        binding.playbackPosition.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Do nothing
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                musicService.stopPlaybackPositionUpdating();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                musicService.seek(seekBar.getProgress());
            }
        });

        updateShuffleColor();
        updateRepeatColor();

        binding.setCurrentAudio(currentAudio.get());
        return binding.getRoot();
    }

    private void onRepeatClicked() {
        boolean newRepeatSetting = !repeatSetting.get();
        repeatSetting.set(newRepeatSetting);
        updateRepeatColor();
    }

    private void onShuffleClicked() {
        boolean newShuffleSetting = !shuffleSetting.get();
        shuffleSetting.set(newShuffleSetting);
        updateShuffleColor();
    }

    private void updateShuffleColor(){
        if(shuffleSetting.get()){
            binding.shuffle.setColorRes(R.color.colorAccent);
        } else {
            binding.shuffle.setColorRes(R.color.md_grey_500);
        }
    }

    private void updateRepeatColor(){
        if(repeatSetting.get()){
            binding.repeat.setColorRes(R.color.colorAccent);
        } else {
            binding.repeat.setColorRes(R.color.md_grey_500);
        }
    }

    private void onPlayPauseClicked() {
        musicService.playPause();
    }

    private void onPreviousClicked() {
        musicService.playPreviousTrackInQueue();
    }

    private void onNextClicked() {
        musicService.playNextTrackInQueue();
    }

    public void setPlaybackPosition(int playbackPosition) {
        binding.setPlaybackPosition(playbackPosition);
    }

    public void setPlaybackState(MusicService.PlaybackState playbackState) {
        binding.setPlaybackState(playbackState);
    }

    @Override
    protected void injectDependencies() {
        ((VkApplication) getActivity().getApplication()).getUserComponent().inject(this);
    }

    @Override
    protected void onCurrentAudioChanged(VKApiAudio currentAudio) {
        binding.setCurrentAudio(currentAudio);
    }

    @Override
    protected void onCurrentAlbumArtChanged(Bitmap currentAlbumArt) {
        new Thread(() -> {
            Drawable currentDrawable = binding.albumLarge.getDrawable();
            if (currentDrawable == null) {
                currentDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_album_placeholder, null);
            }
            BitmapDrawable albumArtDrawable = new BitmapDrawable(getResources(), currentAlbumArt);

            TransitionDrawable transitionDrawableSmall = new TransitionDrawable(new Drawable[]{currentDrawable, albumArtDrawable});
            transitionDrawableSmall.setCrossFadeEnabled(true);

            TransitionDrawable transitionDrawableLarge = new TransitionDrawable(new Drawable[]{currentDrawable, albumArtDrawable});
            transitionDrawableLarge.setCrossFadeEnabled(true);

            getActivity().runOnUiThread(() -> {
                binding.albumSmall.setImageDrawable(transitionDrawableSmall);
                binding.albumLarge.setImageDrawable(transitionDrawableLarge);

                transitionDrawableSmall.startTransition(getResources().getInteger(android.R.integer.config_mediumAnimTime));
                transitionDrawableLarge.startTransition(getResources().getInteger(android.R.integer.config_mediumAnimTime));
            });
        }).start();

        GraphicsUtil.isBottomDark(currentAlbumArt)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isDark -> {
                    binding.playbackPositionLabel.setTextColor(isDark ? Color.WHITE : Color.BLACK);
                    binding.playbackRemainingLabel.setTextColor(isDark ? Color.WHITE : Color.BLACK);
                });
    }
}
