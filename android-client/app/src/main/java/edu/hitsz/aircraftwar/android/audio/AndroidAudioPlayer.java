package edu.hitsz.aircraftwar.android.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

import edu.hitsz.aircraftwar.android.R;
import edu.hitsz.game.core.event.GameEvent;

public final class AndroidAudioPlayer {
    private static final float EFFECT_VOLUME = 0.85f;
    private static final float BGM_VOLUME = 0.65f;

    private final Context appContext;
    private final AudioManager audioManager;
    private final AudioSettingsManager audioSettingsManager;
    private final SoundPool soundPool;
    private final EnumMap<GameEvent.Type, Integer> effectSoundIds = new EnumMap<>(GameEvent.Type.class);
    private final Set<Integer> loadedEffectIds = new HashSet<>();
    private final AudioAttributes gameAudioAttributes;
    private AudioFocusRequest audioFocusRequest;
    private MediaPlayer stageBgmPlayer;
    private MediaPlayer bossBgmPlayer;

    public AndroidAudioPlayer(Context context) {
        this.appContext = context.getApplicationContext();
        this.audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
        this.audioSettingsManager = new AudioSettingsManager(appContext);
        this.gameAudioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        this.soundPool = new SoundPool.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .setMaxStreams(8)
                .build();
        this.soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status == 0) {
                loadedEffectIds.add(sampleId);
            }
        });
        bindDefaultResources();
    }

    public void loadEffect(GameEvent.Type type, int rawResId) {
        effectSoundIds.put(type, soundPool.load(appContext, rawResId, 1));
    }

    public void bindStageMusic(Integer stageRawResId, Integer bossRawResId) {
        releasePlayers();
        if (stageRawResId != null) {
            stageBgmPlayer = createLoopingPlayer(stageRawResId);
            if (stageBgmPlayer != null) {
                stageBgmPlayer.setVolume(BGM_VOLUME, BGM_VOLUME);
            }
        }
        if (bossRawResId != null) {
            bossBgmPlayer = createLoopingPlayer(bossRawResId);
            if (bossBgmPlayer != null) {
                bossBgmPlayer.setVolume(BGM_VOLUME, BGM_VOLUME);
            }
        }
    }

    public void startStageBgm() {
        if (!audioSettingsManager.isAudioEnabled()) {
            pauseAll();
            return;
        }
        requestAudioFocus();
        if (stageBgmPlayer != null && !stageBgmPlayer.isPlaying()) {
            stageBgmPlayer.start();
        }
    }

    public void playEvent(GameEvent event) {
        if (!audioSettingsManager.isAudioEnabled()) {
            return;
        }
        switch (event.getType()) {
            case BOSS_SPAWN -> {
                pauseStageBgm();
                if (bossBgmPlayer != null && !bossBgmPlayer.isPlaying()) {
                    bossBgmPlayer.start();
                }
            }
            case BOSS_DEFEATED -> {
                if (bossBgmPlayer != null && bossBgmPlayer.isPlaying()) {
                    bossBgmPlayer.pause();
                    bossBgmPlayer.seekTo(0);
                }
                startStageBgm();
                playEffect(event.getType());
            }
            case GAME_OVER -> {
                pauseStageBgm();
                if (bossBgmPlayer != null && bossBgmPlayer.isPlaying()) {
                    bossBgmPlayer.pause();
                    bossBgmPlayer.seekTo(0);
                }
                playEffect(event.getType());
            }
            default -> playEffect(event.getType());
        }
    }

    public void pauseAll() {
        pauseStageBgm();
        if (bossBgmPlayer != null && bossBgmPlayer.isPlaying()) {
            bossBgmPlayer.pause();
        }
        abandonAudioFocus();
    }

    public void release() {
        soundPool.release();
        releasePlayers();
        abandonAudioFocus();
    }

    private void playEffect(GameEvent.Type type) {
        if (!audioSettingsManager.isAudioEnabled()) {
            return;
        }
        Integer soundId = effectSoundIds.get(type);
        if (soundId != null && loadedEffectIds.contains(soundId)) {
            requestAudioFocus();
            soundPool.play(soundId, EFFECT_VOLUME, EFFECT_VOLUME, 1, 0, 1.0f);
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

    private void bindDefaultResources() {
        bindStageMusic(R.raw.bgm, R.raw.bgm_boss);
        loadEffect(GameEvent.Type.SHOOT, R.raw.bullet);
        loadEffect(GameEvent.Type.BULLET_HIT, R.raw.bullet_hit);
        loadEffect(GameEvent.Type.BOMB_EXPLOSION, R.raw.bomb_explosion);
        loadEffect(GameEvent.Type.GET_SUPPLY, R.raw.get_supply);
        loadEffect(GameEvent.Type.GAME_OVER, R.raw.game_over);
    }

    private MediaPlayer createLoopingPlayer(int rawResId) {
        try {
            AssetFileDescriptor afd = appContext.getResources().openRawResourceFd(rawResId);
            if (afd == null) {
                return null;
            }
            MediaPlayer player = new MediaPlayer();
            player.setAudioAttributes(gameAudioAttributes);
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            player.setLooping(true);
            player.prepare();
            return player;
        } catch (Exception e) {
            return null;
        }
    }

    private void requestAudioFocus() {
        if (audioManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(gameAudioAttributes)
                        .setAcceptsDelayedFocusGain(false)
                        .build();
            }
            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void abandonAudioFocus() {
        if (audioManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            audioManager.abandonAudioFocus(null);
        }
    }
}
