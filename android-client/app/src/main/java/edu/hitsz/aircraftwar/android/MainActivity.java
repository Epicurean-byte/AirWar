package edu.hitsz.aircraftwar.android;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import edu.hitsz.aircraftwar.android.ui.FeatureFragment;
import edu.hitsz.aircraftwar.android.ui.GameFragment;
import edu.hitsz.aircraftwar.android.ui.LoginFragment;
import edu.hitsz.aircraftwar.android.ui.MainMenuFragment;
import edu.hitsz.game.core.mode.Difficulty;

public class MainActivity extends AppCompatActivity {
    private String currentPlayerName = "Player";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            showLogin();
        }
    }

    public void onLoginSubmitted(String playerName) {
        if (playerName != null && !playerName.trim().isEmpty()) {
            currentPlayerName = playerName.trim();
        }
        showMainMenu();
    }

    public void showLogin() {
        replaceFragment(new LoginFragment(), false);
    }

    public void showMainMenu() {
        replaceFragment(MainMenuFragment.newInstance(currentPlayerName), false);
    }

    public void showFriends() {
        replaceFragment(FeatureFragment.newInstance(
                "好友",
                "这里将接入好友列表、好友申请和在线状态。服务端准备好后可直接改为请求内存态接口。"
        ), true);
    }

    public void showShop() {
        replaceFragment(FeatureFragment.newInstance(
                "商城",
                "这里预留皮肤、音效包和道具购买入口。BitmapSkinManager 已经留出了换皮接口。"
        ), true);
    }

    public void showRooms() {
        replaceFragment(FeatureFragment.newInstance(
                "对战房间",
                "这里预留房间大厅、匹配和邀请逻辑。后续接 Spring Boot 内存房间服务即可。"
        ), true);
    }

    public void showGame(Difficulty difficulty) {
        replaceFragment(GameFragment.newInstance(difficulty, currentPlayerName), true);
    }

    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        var transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        transaction.commit();
    }
}
