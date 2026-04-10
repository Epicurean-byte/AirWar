package edu.hitsz.aircraftwar.android.audio;

import android.content.Context;
import android.content.SharedPreferences;

public final class AudioSettingsManager {
    private static final String PREFS_NAME = "airwar_audio";
    private static final String KEY_AUDIO_ENABLED = "audio_enabled";

    private final SharedPreferences prefs;

    public AudioSettingsManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isAudioEnabled() {
        return prefs.getBoolean(KEY_AUDIO_ENABLED, true);
    }

    public void setAudioEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUDIO_ENABLED, enabled).apply();
    }
}
