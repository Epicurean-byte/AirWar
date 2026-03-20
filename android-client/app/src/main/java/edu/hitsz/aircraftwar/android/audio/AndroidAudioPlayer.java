package edu.hitsz.aircraftwar.android.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.util.EnumMap;

import edu.hitsz.game.core.event.GameEvent;

public final class AndroidAudioPlayer {
    private final Context appContext;
    private final SoundPool soundPool;
    private final EnumMap<GameEvent.Type, Integer> effectSoundIds = new EnumMap<>(GameEvent.Type.class);
    private MediaPlayer stageBgmPlayer;
    private MediaPlayer bossBgmPlayer;

    public AndroidAudioPlayer(Context context) {
        this.appContext = context.getApplicationContext();
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        this.soundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(6)
                .build();
    }

    public void loadEffect(GameEvent.Type type, int rawResId) {
        effectSoundIds.put(type, soundPool.load(appContext, rawResId, 1));
    }

    public void bindStageMusic(Integer stageRawResId, Integer bossRawResId) {
        releasePlayers();
        if (stageRawResId != null) {
            stageBgmPlayer = MediaPlayer.create(appContext, stageRawResId);
            if (stageBgmPlayer != null) {
                stageBgmPlayer.setLooping(true);
            }
        }
        if (bossRawResId != null) {
            bossBgmPlayer = MediaPlayer.create(appContext, bossRawResId);
            if (bossBgmPlayer != null) {
                bossBgmPlayer.setLooping(true);
            }
        }
    }

    public void startStageBgm() {
        if (stageBgmPlayer != null && !stageBgmPlayer.isPlaying()) {
            stageBgmPlayer.start();
        }
    }

    public void playEvent(GameEvent event) {
        switch (event.getType()) {
            case BOSS_SPAWN -> {
                pauseStageBgm();
                if (bossBgmPlayer != null && !bossBgmPlayer.isPlaying()) {
                    bossBgmPlayer.start();
                }
            }
            case BOSS_DEFEATED, GAME_OVER -> {
                if (bossBgmPlayer != null && bossBgmPlayer.isPlaying()) {
                    bossBgmPlayer.pause();
                    bossBgmPlayer.seekTo(0);
                }
                startStageBgm();
            }
            default -> playEffect(event.getType());
        }
    }

    public void pauseAll() {
        pauseStageBgm();
        if (bossBgmPlayer != null && bossBgmPlayer.isPlaying()) {
            bossBgmPlayer.pause();
        }
    }

    public void release() {
        soundPool.release();
        releasePlayers();
    }

    private void playEffect(GameEvent.Type type) {
        Integer soundId = effectSoundIds.get(type);
        if (soundId != null) {
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    private void pauseStageBgm() {
        if (stageBgmPlayer != null && stageBgmPlayer.isPlaying()) {
            stageBgmPlayer.pause();
        }
    }

    private void releasePlayers() {
        if (stageBgmPlayer != null) {
            stageBgmPlayer.release();
            stageBgmPlayer = null;
        }
        if (bossBgmPlayer != null) {
            bossBgmPlayer.release();
            bossBgmPlayer = null;
        }
    }
}
