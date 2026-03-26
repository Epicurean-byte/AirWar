package edu.hitsz.aircraftwar.android.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import edu.hitsz.aircraftwar.android.MainActivity;
import edu.hitsz.aircraftwar.android.network.NetworkExecutor;
import edu.hitsz.aircraftwar.android.network.model.LeaderboardEntry;

public class LeaderboardFragment extends Fragment {
    private LinearLayout scoreList;
    private LinearLayout coinList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout root = UiUtils.createScreenColumn(requireContext());
        scrollView.addView(root);

        root.addView(UiUtils.createTitle(requireContext(), "排行榜"));
        var refresh = UiUtils.createActionButton(requireContext(), "刷新排行榜");
        refresh.setOnClickListener(v -> loadBoard());
        root.addView(refresh);

        root.addView(UiUtils.createBody(requireContext(), "分数榜 Top 50"));
        scoreList = new LinearLayout(requireContext());
        scoreList.setOrientation(LinearLayout.VERTICAL);
        root.addView(scoreList);

        root.addView(UiUtils.createBody(requireContext(), "金币榜 Top 50"));
        coinList = new LinearLayout(requireContext());
        coinList.setOrientation(LinearLayout.VERTICAL);
        root.addView(coinList);

        var back = UiUtils.createActionButton(requireContext(), "返回主菜单");
        back.setOnClickListener(v -> ((MainActivity) requireActivity()).showMainMenu());
        root.addView(back);

        loadBoard();
        return scrollView;
    }

    private void loadBoard() {
        MainActivity activity = (MainActivity) requireActivity();
        NetworkExecutor.run(() -> {
            try {
                List<LeaderboardEntry> scores = activity.getApiClient().scoreLeaderboard();
                List<LeaderboardEntry> coins = activity.getApiClient().coinLeaderboard();
                activity.runOnUiThread(() -> {
                    render(scoreList, scores, "score");
                    render(coinList, coins, "coins");
                });
            } catch (Exception e) {
                activity.toast(e.getMessage() == null ? "请求失败" : e.getMessage());
            }
        });
    }

    private void render(LinearLayout container, List<LeaderboardEntry> data, String title) {
        container.removeAllViews();
        if (data.isEmpty()) {
            container.addView(UiUtils.createBody(requireContext(), "暂无数据"));
            return;
        }
        int rank = 1;
        for (LeaderboardEntry entry : data) {
            container.addView(UiUtils.createBody(requireContext(),
                    rank + ". " + entry.getNickname() + " " + title + "=" + entry.getValue()
                            + " skin=#" + entry.getEquippedSkinId()));
            rank++;
        }
    }
}
