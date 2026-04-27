package edu.hitsz.aircraftwar.android.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.LoudnessEnhancer;

import edu.hitsz.aircraftwar.android.R;

public final class GlobalBgmManager {
    private static final float BGM_VOLUME = 1.0f;
    private static final int BGM_GAIN_MB = 1800;

    private static GlobalBgmManager instance;

    public static synchronized GlobalBgmManager getInstance(Context context) {
        if (instance == null) {
            instance = new GlobalBgmManager(context.getApplicationContext());
        }
        return instance;
    }

    private final Context appContext;
    private final AudioManager audioManager;
    private final AudioSettingsManager audioSettingsManager;
    private final AudioAttributes audioAttributes;

    private MediaPlayer mainBgmPlayer;
    private MediaPlayer bossBgmPlayer;
    private LoudnessEnhancer mainBgmEnhancer;
    private LoudnessEnhancer bossBgmEnhancer;
    private boolean bossMode = false;

    private GlobalBgmManager(Context context) {
        this.appContext = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.audioSettingsManager = new AudioSettingsManager(context);
        this.audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        this.mainBgmPlayer = createLoopingPlayer(R.raw.bgm);
        this.bossBgmPlayer = createLoopingPlayer(R.raw.bgm_boss);
        this.mainBgmEnhancer = createEnhancer(mainBgmPlayer);
        this.bossBgmEnhancer = createEnhancer(bossBgmPlayer);
    }

    public void startMainBgm() {
        bossMode = false;
        if (!audioSettingsManager.isAudioEnabled()) {
            stopAll();
            return;
        }
        requestAudioFocus();
        if (bossBgmPlayer != null && bossBgmPlayer.isPlaying()) {
            bossBgmPlayer.pause();
            bossBgmPlayer.seekTo(0);
        }
        if (mainBgmPlayer != null && !mainBgmPlayer.isPlaying()) {
            mainBgmPlayer.start();
        }
    }

    public void startBossBgm() {
        bossMode = true;
        if (!audioSettingsManager.isAudioEnabled()) {
            stopAll();
            return;
        }
        requestAudioFocus();
        if (mainBgmPlayer != null && mainBgmPlayer.isPlaying()) {
            mainBgmPlayer.pause();
        }
        if (bossBgmPlayer != null && !bossBgmPlayer.isPlaying()) {
            bossBgmPlayer.start();
        }
    }

    public void resumeCurrentMode() {
        if (bossMode) {
            startBossBgm();
        } else {
            startMainBgm();
        }
    }

    public void pause() {
        if (mainBgmPlayer != null && mainBgmPlayer.isPlaying()) {
            mainBgmPlayer.pause();
        }
        if (bossBgmPlayer != null && bossBgmPlayer.isPlaying()) {
            bossBgmPlayer.pause();
        }
        abandonAudioFocus();
    }

    public void stopAll() {
        if (mainBgmPlayer != null) {
            if (mainBgmPlayer.isPlaying()) {
                mainBgmPlayer.pause();
            }
            mainBgmPlayer.seekTo(0);
        }
        if (bossBgmPlayer != null) {
            if (bossBgmPlayer.isPlaying()) {
                bossBgmPlayer.pause();
            }
            bossBgmPlayer.seekTo(0);
        }
        abandonAudioFocus();
    }

    public void release() {
        stopAll();
        if (mainBgmPlayer != null) {
            mainBgmPlayer.release();
            mainBgmPlayer = null;
        }
        if (bossBgmPlayer != null) {
            bossBgmPlayer.release();
            bossBgmPlayer = null;
        }
        if (mainBgmEnhancer != null) {
            mainBgmEnhancer.release();
            mainBgmEnhancer = null;
        }
        if (bossBgmEnhancer != null) {
            bossBgmEnhancer.release();
            bossBgmEnhancer = null;
        }
    }

    private MediaPlayer createLoopingPlayer(int rawResId) {
        try {
            AssetFileDescriptor afd = appContext.getResources().openRawResourceFd(rawResId);
            if (afd == null) {
                return null;
            }
            MediaPlayer player = new MediaPlayer();
            player.setAudioAttributes(audioAttributes);
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            player.setLooping(true);
            player.setVolume(BGM_VOLUME, BGM_VOLUME);
            player.prepare();
            return player;
        } catch (Exception e) {
            return null;
        }
    }

    private LoudnessEnhancer createEnhancer(MediaPlayer player) {
        if (player == null) {
            return null;
        }
        try {
            LoudnessEnhancer enhancer = new LoudnessEnhancer(player.getAudioSessionId());
            enhancer.setTargetGain(BGM_GAIN_MB);
            enhancer.setEnabled(true);
            return enhancer;
        } catch (Exception e) {
            return null;
        }
    }

    private void requestAudioFocus() {
        if (audioManager != null) {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void abandonAudioFocus() {
        if (audioManager != null) {
            audioManager.abandonAudioFocus(null);
        }
    }
}
