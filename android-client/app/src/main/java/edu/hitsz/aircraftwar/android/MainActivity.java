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

import edu.hitsz.aircraftwar.android.network.HttpApiClient;
import edu.hitsz.aircraftwar.android.network.NetworkExecutor;
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
import edu.hitsz.aircraftwar.android.ui.ShopFragment;
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
    private UserProfile currentUser;
    private WsGameClient wsGameClient;
    private GameWsListener wsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // configureWindow();

        sessionManager = new SessionManager(this);
        currentUser = sessionManager.loadUserOrNull();

        if (savedInstanceState == null) {
            if (currentUser == null) {
                showLogin();
            } else {
                showMainMenu();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
    }

    public HttpApiClient getApiClient() {
        return apiClient;
    }

    public UserProfile getCurrentUser() {
        return currentUser;
    }

    public void onUserAuthenticated(UserProfile user) {
        currentUser = user;
        sessionManager.saveUser(user);
        showMainMenu();
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

    public void showRooms() {
        replaceFragment(new RoomsFragment(), true);
    }

    public void showLeaderboard() {
        replaceFragment(new LeaderboardFragment(), true);
    }

    public void showGame(Difficulty difficulty) {
        String playerName = currentUser == null ? "Player" : currentUser.getNickname();
        long userId = currentUser == null ? 0L : currentUser.getUserId();
        replaceFragment(GameFragment.newInstance(difficulty, playerName, userId), true);
    }

    public void showPvpGame(long roomId, long seed) {
        replaceFragment(PvpGameFragment.newInstance(roomId, seed), true);
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
            wsGameClient.close();
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
