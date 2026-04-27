package edu.hitsz.aircraftwar.android.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

import edu.hitsz.aircraftwar.android.R;
import edu.hitsz.game.core.event.GameEvent;

public final class AndroidAudioPlayer {
    private static final float EFFECT_VOLUME = 0.85f;
    private final Context appContext;
    private final AudioManager audioManager;
    private final AudioSettingsManager audioSettingsManager;
    private final GlobalBgmManager globalBgmManager;
    private final SoundPool soundPool;
    private final EnumMap<GameEvent.Type, Integer> effectSoundIds = new EnumMap<>(GameEvent.Type.class);
    private final Set<Integer> loadedEffectIds = new HashSet<>();
    private final AudioAttributes gameAudioAttributes;
    private AudioFocusRequest audioFocusRequest;

    public AndroidAudioPlayer(Context context) {
        this.appContext = context.getApplicationContext();
        this.audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
        this.audioSettingsManager = new AudioSettingsManager(appContext);
        this.globalBgmManager = GlobalBgmManager.getInstance(appContext);
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
        // Background music is managed globally from app launch.
    }

    public void startStageBgm() {
        if (!audioSettingsManager.isAudioEnabled()) {
            pauseAll();
            return;
        }
        globalBgmManager.startMainBgm();
    }

    public void playEvent(GameEvent event) {
        if (!audioSettingsManager.isAudioEnabled()) {
            return;
        }
        switch (event.getType()) {
            case BOSS_SPAWN -> globalBgmManager.startBossBgm();
            case BOSS_DEFEATED -> globalBgmManager.startMainBgm();
            case GAME_OVER -> {
                globalBgmManager.startMainBgm();
                playEffect(event.getType());
            }
            default -> playEffect(event.getType());
        }
    }

    public void pauseAll() {
        globalBgmManager.pause();
        abandonAudioFocus();
    }

    public void release() {
        soundPool.release();
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

    private void bindDefaultResources() {
        loadEffect(GameEvent.Type.SHOOT, R.raw.bullet);
        loadEffect(GameEvent.Type.BULLET_HIT, R.raw.bullet_hit);
        loadEffect(GameEvent.Type.BOMB_EXPLOSION, R.raw.bomb_explosion);
        loadEffect(GameEvent.Type.GET_SUPPLY, R.raw.get_supply);
        loadEffect(GameEvent.Type.GAME_OVER, R.raw.game_over);
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
