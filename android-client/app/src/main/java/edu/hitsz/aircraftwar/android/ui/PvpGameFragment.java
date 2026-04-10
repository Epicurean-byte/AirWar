package edu.hitsz.aircraftwar.android.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.hitsz.aircraftwar.android.MainActivity;
import edu.hitsz.aircraftwar.android.game.PvpBattleView;
import edu.hitsz.aircraftwar.android.network.NetworkExecutor;

public class PvpGameFragment extends Fragment {
    private static final String ARG_ROOM_ID = "room_id";
    private static final String ARG_SEED = "seed";
    private static final String ARG_GAME_MODE = "game_mode";
    private static final String ARG_PLAYER1_SKIN = "player1_skin";
    private static final String ARG_PLAYER2_SKIN = "player2_skin";
    private static final String ARG_PLAYER1_ID = "player1_id";
    private static final String ARG_PLAYER2_ID = "player2_id";

    private final AtomicBoolean settled = new AtomicBoolean(false);
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long roomId;
    private long seed;
    private long myUserId;
    private String gameMode = "COOP";
    private long player1Id;
    private long player2Id;
    private int player1SkinId;
    private int player2SkinId;

    private PvpBattleView battleView;
    private TextView statusView;
    private long myScore = 0L;
    private long myCoins = 0L;
    private int currentEnemyCount = 0; // 跟踪当前敌机数量

    // 优化：减少自动开火频率，并只在有敌机时发送
    private final Runnable autoFireTask = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) {
                return;
            }
            // 只在有敌机时才发送FIRE消息，减少网络负担
            if (currentEnemyCount > 0) {
                ((MainActivity) requireActivity()).sendWs("FIRE", roomId, null);
            }
            // 增加间隔到500ms，减少网络消息频率
            handler.postDelayed(this, 500L);
        }
    };

    public static PvpGameFragment newInstance(long roomId, long seed, String gameMode, 
                                              long player1Id, long player2Id, 
                                              int player1SkinId, int player2SkinId) {
        Bundle args = new Bundle();
        args.putLong(ARG_ROOM_ID, roomId);
        args.putLong(ARG_SEED, seed);
        args.putString(ARG_GAME_MODE, gameMode);
        args.putLong(ARG_PLAYER1_ID, player1Id);
        args.putLong(ARG_PLAYER2_ID, player2Id);
        args.putInt(ARG_PLAYER1_SKIN, player1SkinId);
        args.putInt(ARG_PLAYER2_SKIN, player2SkinId);
        PvpGameFragment fragment = new PvpGameFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        roomId = getArguments() == null ? 0L : getArguments().getLong(ARG_ROOM_ID, 0L);
        seed = getArguments() == null ? 0L : getArguments().getLong(ARG_SEED, 0L);
        gameMode = getArguments() == null ? "COOP" : getArguments().getString(ARG_GAME_MODE, "COOP");
        player1Id = getArguments() == null ? 0L : getArguments().getLong(ARG_PLAYER1_ID, 0L);
        player2Id = getArguments() == null ? 0L : getArguments().getLong(ARG_PLAYER2_ID, 0L);
        player1SkinId = getArguments() == null ? 0 : getArguments().getInt(ARG_PLAYER1_SKIN, 0);
        player2SkinId = getArguments() == null ? 0 : getArguments().getInt(ARG_PLAYER2_SKIN, 0);

        MainActivity activity = (MainActivity) requireActivity();
        myUserId = activity.getCurrentUser() == null ? 0L : activity.getCurrentUser().getUserId();
        int myEquippedSkinId = activity.getCurrentUser() == null ? 0 : activity.getCurrentUser().getEquippedSkinId();

        FrameLayout root = new FrameLayout(requireContext());
        battleView = new PvpBattleView(requireContext());
        battleView.setMyUserId(myUserId);
        battleView.setMyPlaneSkinId(myEquippedSkinId);
        battleView.setGameMode(gameMode);
        
        // 设置对手的皮肤
        int enemySkinId = (myUserId == player1Id) ? player2SkinId : player1SkinId;
        battleView.setEnemyPlayerSkinId(enemySkinId);
        
        battleView.setInputListener(new PvpBattleView.InputListener() {
            @Override
            public void onMove(float x, float y) {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("x", x);
                    payload.put("y", y);
                    activity.sendWs("MOVE", roomId, payload);
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onFire() {
                activity.sendWs("FIRE", roomId, null);
            }
        });
        root.addView(battleView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        int margin = UiUtils.dp(requireContext(), 12);

        LinearLayout topPanel = UiUtils.createPanel(requireContext(), 12);
        topPanel.setOrientation(LinearLayout.VERTICAL);
        String modeText = "COOP".equals(gameMode) ? "合作空域" : "对战空域";
        topPanel.addView(UiUtils.createSectionTitle(requireContext(), modeText));
        statusView = UiUtils.createCaption(requireContext(), "房间#" + roomId + " seed=" + seed + " 连接中...");
        UiUtils.setTopMargin(statusView, requireContext(), 6);
        topPanel.addView(statusView);
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        statusParams.gravity = Gravity.TOP | Gravity.START;
        statusParams.setMargins(margin, margin, margin, margin);
        root.addView(topPanel, statusParams);

        Button exitButton = UiUtils.createSecondaryButton(requireContext(), "退出联机");
        exitButton.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).sendWs("GAME_OVER", roomId, null);
            settlePvp();
            ((MainActivity) requireActivity()).showMainMenu();
        });
        FrameLayout.LayoutParams exitParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        exitParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        exitParams.setMargins(margin, margin, margin, margin);
        root.addView(exitButton, exitParams);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) requireActivity();
        activity.setWsListener(new MainActivity.GameWsListener() {
            @Override
            public void onOpen() {
                statusView.setText("房间#" + roomId + " 已建立链路，等待实时对战数据。");
            }

            @Override
            public void onMessage(String type, long msgRoomId, long userId, @Nullable JSONObject payload) {
                if (msgRoomId != 0 && msgRoomId != roomId) {
                    return;
                }
                if ("BATTLE_STATE".equals(type) && payload != null) {
                    onBattleState(payload);
                    return;
                }
                if ("GAME_OVER".equals(type) && payload != null) {
                    onGameOver(payload);
                    return;
                }
                if ("OPPONENT_DISCONNECTED".equals(type)) {
                    activity.toast("对手断线，比赛结束");
                    settlePvp();
                    activity.showMainMenu();
                    return;
                }
                if ("ERROR".equals(type) && payload != null) {
                    activity.toast("WS错误: " + payload.optString("reason", "unknown"));
                }
            }

            @Override
            public void onError(String message) {
                activity.toast("WS错误: " + message);
            }

            @Override
            public void onClosed() {
                activity.toast("WS已断开");
            }
        });
        // 优化：增加初始延迟，等待游戏状态稳定后再开始自动开火
        handler.postDelayed(autoFireTask, 500L);
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(autoFireTask);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(autoFireTask);
        MainActivity activity = (MainActivity) requireActivity();
        activity.setWsListener(null);
        battleView = null;
        super.onDestroyView();
    }

    private void onBattleState(JSONObject payload) {
        JSONArray playersJson = payload.optJSONArray("players");
        JSONArray enemiesJson = payload.optJSONArray("enemies");
        if (playersJson == null || enemiesJson == null || battleView == null) {
            return;
        }

        List<PvpBattleView.PlayerState> players = new ArrayList<>();
        for (int i = 0; i < playersJson.length(); i++) {
            JSONObject item = playersJson.optJSONObject(i);
            if (item == null) continue;
            PvpBattleView.PlayerState p = new PvpBattleView.PlayerState();
            p.userId = item.optLong("userId", 0L);
            p.x = (float) item.optDouble("x", 0.0);
            p.y = (float) item.optDouble("y", 0.0);
            p.hp = item.optInt("hp", 0);
            p.score = item.optLong("score", 0L);
            p.coins = item.optLong("coins", 0L);
            if (p.userId == myUserId) {
                myScore = p.score;
                myCoins = p.coins;
            }
            players.add(p);
        }

        List<PvpBattleView.EnemyState> enemies = new ArrayList<>();
        for (int i = 0; i < enemiesJson.length(); i++) {
            JSONObject item = enemiesJson.optJSONObject(i);
            if (item == null) continue;
            PvpBattleView.EnemyState e = new PvpBattleView.EnemyState();
            e.id = item.optInt("id", 0);
            e.type = item.optInt("type", 0);
            e.x = (float) item.optDouble("x", 0.0);
            e.y = (float) item.optDouble("y", 0.0);
            e.hp = item.optInt("hp", 0);
            enemies.add(e);
        }
        
        // 解析子弹状态
        List<PvpBattleView.BulletState> bullets = new ArrayList<>();
        JSONArray bulletsJson = payload.optJSONArray("bullets");
        if (bulletsJson != null) {
            android.util.Log.d("PvpGameFragment", "Received " + bulletsJson.length() + " bullets from server");
            for (int i = 0; i < bulletsJson.length(); i++) {
                JSONObject item = bulletsJson.optJSONObject(i);
                if (item == null) continue;
                PvpBattleView.BulletState b = new PvpBattleView.BulletState();
                b.id = item.optInt("id", 0);
                b.ownerId = item.optLong("ownerId", 0L);
                b.x = (float) item.optDouble("x", 0.0);
                b.y = (float) item.optDouble("y", 0.0);
                bullets.add(b);
                android.util.Log.d("PvpGameFragment", "Bullet " + b.id + " at (" + b.x + ", " + b.y + ") owner=" + b.ownerId);
            }
        } else {
            android.util.Log.d("PvpGameFragment", "No bullets array in payload");
        }

        // 更新当前敌机数量，用于优化自动开火
        currentEnemyCount = enemies.size();

        battleView.updateState(players, enemies, bullets);
        statusView.setText("房间#" + roomId + "  战绩 " + myScore + "  金币 " + myCoins + "  敌机 " + enemies.size());
    }

    private void onGameOver(JSONObject payload) {
        long winnerUserId = payload.optLong("winnerUserId", 0L);
        MainActivity activity = (MainActivity) requireActivity();
        if (winnerUserId == myUserId) {
            activity.toast("你赢了");
        } else {
            activity.toast("你输了");
        }

        JSONArray playersJson = payload.optJSONArray("players");
        if (playersJson != null) {
            for (int i = 0; i < playersJson.length(); i++) {
                JSONObject p = playersJson.optJSONObject(i);
                if (p == null) continue;
                if (p.optLong("userId", 0L) == myUserId) {
                    myScore = p.optLong("score", myScore);
                    myCoins = p.optLong("coins", myCoins);
                    break;
                }
            }
        }

        settlePvp();
        activity.showMainMenu();
    }

    private void settlePvp() {
        MainActivity activity = (MainActivity) requireActivity();
        if (activity.getCurrentUser() == null || !settled.compareAndSet(false, true)) {
            return;
        }
        long userId = activity.getCurrentUser().getUserId();
        long score = myScore;
        long coins = myCoins;
        NetworkExecutor.run(() -> {
            try {
                activity.getApiClient().settlePvp(roomId, userId, score, coins);
                activity.toast("联机结算已上报 score=" + score + " coins=" + coins);
            } catch (Exception e) {
                activity.toast("联机结算失败: " + (e.getMessage() == null ? "unknown" : e.getMessage()));
            }
        });
    }
}
