package edu.hitsz.aircraftwar.android.network;

public final class ServerConfig {
    private ServerConfig() {
    }

    // Android emulator can use 10.0.2.2 to access localhost on host machine.
    public static final String HTTP_BASE_URL = "http://10.0.2.2:18080";
    public static final String WS_GAME_URL = "ws://10.0.2.2:18080/ws/game";
}
