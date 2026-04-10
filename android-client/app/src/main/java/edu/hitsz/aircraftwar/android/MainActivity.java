package edu.hitsz.aircraftwar.android;

import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import edu.hitsz.aircraftwar.android.audio.AudioSettingsManager;
import edu.hitsz.aircraftwar.android.audio.GlobalBgmManager;
import edu.hitsz.aircraftwar.android.network.HttpApiClient;
import edu.hitsz.aircraftwar.android.network.LocalInventoryManager;
import edu.hitsz.aircraftwar.android.network.NetworkExecutor;
import edu.hitsz.aircraftwar.android.network.ServerConfigManager;
import edu.hitsz.aircraftwar.android.network.SessionManager;
import edu.hitsz.aircraftwar.android.network.WsGameClient;
import edu.hitsz.aircraftwar.android.network.model.UserProfile;
import edu.hitsz.aircraftwar.android.ui.FriendsFragment;
import edu.hitsz.aircraftwar.android.ui.GameFragment;
import edu.hitsz.aircraftwar.android.ui.LeaderboardFragment;
import edu.hitsz.aircraftwar.android.ui.LoginFragment;
import edu.hitsz.aircraftwar.android.ui.MainMenuFragment;
import edu.hitsz.aircraftwar.android.ui.PvpGameFragment;
import edu.hitsz.aircraftwar.android.ui.RoomsFragment;
import edu.hitsz.aircraftwar.android.ui.SettingsFragment;
import edu.hitsz.aircraftwar.android.ui.ShopFragment;
import edu.hitsz.aircraftwar.android.ui.WarehouseFragment;
import edu.hitsz.game.core.mode.Difficulty;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    public interface GameWsListener {
        void onOpen();

        void onMessage(String type, long roomId, long userId, @Nullable JSONObject payload);

        void onError(String message);

        void onClosed();
    }

    private final HttpApiClient apiClient = new HttpApiClient();

    private SessionManager sessionManager;
    private AudioSettingsManager audioSettingsManager;
    private LocalInventoryManager localInventoryManager;
    private UserProfile currentUser;
    private WsGameClient wsGameClient;
    private GameWsListener wsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // configureWindow();

        // Initialize ServerConfigManager with context to load saved configuration
        ServerConfigManager.getInstance().initialize(this);

        sessionManager = new SessionManager(this);
        audioSettingsManager = new AudioSettingsManager(this);
        localInventoryManager = new LocalInventoryManager(this);
        currentUser = sessionManager.loadUserOrNull();

        if (savedInstanceState == null) {
            if (currentUser == null) {
                showLogin();
            } else {
                showMainMenu();
            }
        }
        if (isAudioEnabled()) {
            GlobalBgmManager.getInstance(this).startMainBgm();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        if (isAudioEnabled()) {
            GlobalBgmManager.getInstance(this).resumeCurrentMode();
        }
    }

    @Override
    protected void onPause() {
        GlobalBgmManager.getInstance(this).pause();
        super.onPause();
    }

    public HttpApiClient getApiClient() {
        return apiClient;
    }

    public UserProfile getCurrentUser() {
        return currentUser;
    }

    public LocalInventoryManager getLocalInventoryManager() {
        return localInventoryManager;
    }

    public boolean isAudioEnabled() {
        return audioSettingsManager != null && audioSettingsManager.isAudioEnabled();
    }

    public void setAudioEnabled(boolean enabled) {
        if (audioSettingsManager != null) {
            audioSettingsManager.setAudioEnabled(enabled);
        }
        if (enabled) {
            GlobalBgmManager.getInstance(this).resumeCurrentMode();
        } else {
            GlobalBgmManager.getInstance(this).stopAll();
        }
    }

    public void onUserAuthenticated(UserProfile user) {
        currentUser = user;
        sessionManager.saveUser(user);
        showMainMenu();
    }

    public void updateCurrentUser(UserProfile user) {
        currentUser = user;
        sessionManager.saveUser(user);
    }

    public void updateCoinsAndEquippedSkin(long coins, int equippedSkinId) {
        if (currentUser == null) {
            return;
        }
        updateCurrentUser(currentUser.withCoinsAndEquippedSkin(coins, equippedSkinId));
    }

    public void logoutAndBackToLogin() {
        disconnectGameWs();
        UserProfile localUser = currentUser;
        currentUser = null;
        sessionManager.clear();
        showLogin();

        if (localUser == null) {
            return;
        }
        NetworkExecutor.run(() -> {
            try {
                apiClient.logout(localUser.getUserId());
            } catch (Exception ignored) {
                // Local logout remains valid even if server call fails.
            }
        });
    }

    public void showLogin() {
        replaceFragment(new LoginFragment(), false);
    }

    public void showMainMenu() {
        String playerName = currentUser == null ? "Player" : currentUser.getNickname();
        replaceFragment(MainMenuFragment.newInstance(playerName), false);
    }

    public void showFriends() {
        replaceFragment(new FriendsFragment(), true);
    }

    public void showShop() {
        replaceFragment(new ShopFragment(), true);
    }

    public void showWarehouse() {
        replaceFragment(new WarehouseFragment(), true);
    }

    public void showRooms() {
        replaceFragment(new RoomsFragment(), true);
    }

    public void showLeaderboard() {
        replaceFragment(new LeaderboardFragment(), true);
    }

    public void showSettings() {
        replaceFragment(new SettingsFragment(), true);
    }

    public void showGame(Difficulty difficulty) {
        String playerName = currentUser == null ? "Player" : currentUser.getNickname();
        long userId = currentUser == null ? 0L : currentUser.getUserId();
        replaceFragment(GameFragment.newInstance(difficulty, playerName, userId), true);
    }

    public void showPvpGame(long roomId, long seed, String gameMode, long player1Id, long player2Id, int player1SkinId, int player2SkinId) {
        replaceFragment(PvpGameFragment.newInstance(roomId, seed, gameMode, player1Id, player2Id, player1SkinId, player2SkinId), true);
    }

    public void setWsListener(@Nullable GameWsListener listener) {
        this.wsListener = listener;
    }

    public boolean isGameWsConnected() {
        return wsGameClient != null;
    }

    public void connectGameWs() {
        if (currentUser == null) {
            toast("请先登录");
            return;
        }
        disconnectGameWs();
        wsGameClient = new WsGameClient();
        wsGameClient.connect(currentUser.getUserId(), new WsGameClient.Listener() {
            @Override
            public void onOpen() {
                if (wsListener != null) {
                    runOnUiThread(() -> wsListener.onOpen());
                }
            }

            @Override
            public void onMessage(String type, long roomId, long userId, @Nullable JSONObject payload) {
                if (wsListener != null) {
                    runOnUiThread(() -> wsListener.onMessage(type, roomId, userId, payload));
                }
            }

            @Override
            public void onError(String message) {
                if (wsListener != null) {
                    runOnUiThread(() -> wsListener.onError(message));
                }
            }

            @Override
            public void onClosed() {
                if (wsListener != null) {
                    runOnUiThread(() -> wsListener.onClosed());
                }
            }
        });
    }

    public void disconnectGameWs() {
        if (wsGameClient != null) {
            wsGameClient.cleanup();
            wsGameClient = null;
        }
    }

    public boolean sendWs(String type, long roomId, @Nullable JSONObject payload) {
        if (wsGameClient == null) {
            return false;
        }
        wsGameClient.send(type, roomId, payload);
        return true;
    }

    public void toast(String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        disconnectGameWs();
        GlobalBgmManager.getInstance(this).release();
        super.onDestroy();
    }

    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        var transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        transaction.commit();
        getWindow().getDecorView().post(this::hideSystemBars);
    }

    private void configureWindow() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        hideSystemBars();
    }

    private void hideSystemBars() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
    }
}
