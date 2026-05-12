package edu.hitsz.aircraftwar.android.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AlertDialog;
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
                    render(scoreList, scores, "score", "分数");
                    render(coinList, coins, "coins", "金币");
                });
            } catch (Exception e) {
                activity.toast(e.getMessage() == null ? "请求失败" : e.getMessage());
            }
        });
    }

    private void render(LinearLayout container, List<LeaderboardEntry> data, String boardType, String title) {
        container.removeAllViews();
        if (data.isEmpty()) {
            container.addView(UiUtils.createBody(requireContext(), "暂无数据"));
            return;
        }
        int rank = 1;
        for (LeaderboardEntry entry : data) {
            LinearLayout row = UiUtils.createPanel(requireContext(), 10);
            row.addView(UiUtils.createBody(requireContext(),
                    rank + ". " + entry.getNickname() + " " + title + "=" + entry.getValue()
                            + " skin=#" + entry.getEquippedSkinId() + " user=#" + entry.getUserId()));

            Button delete = UiUtils.createSmallButton(requireContext(), "删除该" + title + "榜数据");
            delete.setOnClickListener(v -> confirmDelete(entry, boardType, title));
            UiUtils.setTopMargin(delete, requireContext(), 8);
            row.addView(delete);
            container.addView(row);
            rank++;
        }
    }

    private void confirmDelete(LeaderboardEntry entry, String boardType, String title) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除排行榜数据")
                .setMessage("确认删除 " + entry.getNickname() + " 的" + title + "榜数据？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> deleteEntry(entry.getUserId(), boardType))
                .show();
    }

    private void deleteEntry(long targetUserId, String boardType) {
        MainActivity activity = (MainActivity) requireActivity();
        NetworkExecutor.run(() -> {
            try {
                activity.getApiClient().deleteLeaderboardEntry(targetUserId, boardType);
                activity.toast("排行榜数据已删除");
                activity.runOnUiThread(this::loadBoard);
            } catch (Exception e) {
                activity.toast(e.getMessage() == null ? "删除失败" : e.getMessage());
            }
        });
    }
}
