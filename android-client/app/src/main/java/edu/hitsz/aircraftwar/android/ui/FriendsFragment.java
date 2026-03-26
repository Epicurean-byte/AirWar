package edu.hitsz.aircraftwar.android.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import edu.hitsz.aircraftwar.android.MainActivity;
import edu.hitsz.aircraftwar.android.network.NetworkExecutor;
import edu.hitsz.aircraftwar.android.network.model.FriendRequestItem;
import edu.hitsz.aircraftwar.android.network.model.UserProfile;

public class FriendsFragment extends Fragment {
    private LinearLayout friendsList;
    private LinearLayout requestsList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout root = UiUtils.createScreenColumn(requireContext());
        scrollView.addView(root);

        var title = UiUtils.createTitle(requireContext(), "好友系统");
        root.addView(title);

        EditText searchInput = new EditText(requireContext());
        searchInput.setHint("搜索用户名/昵称");
        searchInput.setInputType(InputType.TYPE_CLASS_TEXT);
        searchInput.setTextColor(0xFF111111);
        searchInput.setHintTextColor(0xFF666666);
        root.addView(searchInput);

        var searchBtn = UiUtils.createActionButton(requireContext(), "搜索并发送好友申请");
        searchBtn.setOnClickListener(v -> searchAndRequest(searchInput.getText().toString().trim()));
        root.addView(searchBtn);

        var refreshBtn = UiUtils.createActionButton(requireContext(), "刷新好友与申请");
        refreshBtn.setOnClickListener(v -> refreshAll());
        root.addView(refreshBtn);

        root.addView(UiUtils.createBody(requireContext(), "好友列表"));
        friendsList = new LinearLayout(requireContext());
        friendsList.setOrientation(LinearLayout.VERTICAL);
        root.addView(friendsList);

        root.addView(UiUtils.createBody(requireContext(), "待处理申请"));
        requestsList = new LinearLayout(requireContext());
        requestsList.setOrientation(LinearLayout.VERTICAL);
        root.addView(requestsList);

        var backButton = UiUtils.createActionButton(requireContext(), "返回主菜单");
        backButton.setOnClickListener(v -> ((MainActivity) requireActivity()).showMainMenu());
        root.addView(backButton);

        refreshAll();
        return scrollView;
    }

    private void refreshAll() {
        MainActivity activity = (MainActivity) requireActivity();
        if (activity.getCurrentUser() == null) {
            activity.toast("请先登录");
            return;
        }
        long userId = activity.getCurrentUser().getUserId();
        NetworkExecutor.run(() -> {
            try {
                List<UserProfile> friends = activity.getApiClient().getFriends(userId);
                List<FriendRequestItem> requests = activity.getApiClient().getFriendRequests(userId);
                activity.runOnUiThread(() -> renderLists(friends, requests));
            } catch (Exception e) {
                activity.toast(msg(e));
            }
        });
    }

    private void renderLists(List<UserProfile> friends, List<FriendRequestItem> requests) {
        friendsList.removeAllViews();
        requestsList.removeAllViews();

        if (friends.isEmpty()) {
            friendsList.addView(UiUtils.createBody(requireContext(), "暂无好友"));
        }
        for (UserProfile f : friends) {
            String line = f.getNickname() + " (" + f.getUsername() + ") online=" + f.isOnline()
                    + " highScore=" + f.getHighScore() + " coins=" + f.getCoins();
            friendsList.addView(UiUtils.createBody(requireContext(), line));
        }

        if (requests.isEmpty()) {
            requestsList.addView(UiUtils.createBody(requireContext(), "暂无好友申请"));
        }
        for (FriendRequestItem req : requests) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.addView(UiUtils.createBody(requireContext(),
                    "来自: " + req.getFromNickname() + "(" + req.getFromUsername() + ")"));

            var accept = UiUtils.createActionButton(requireContext(), "同意#" + req.getRequestId());
            accept.setOnClickListener(v -> respond(req.getRequestId(), true));
            var reject = UiUtils.createActionButton(requireContext(), "拒绝#" + req.getRequestId());
            reject.setOnClickListener(v -> respond(req.getRequestId(), false));

            row.addView(accept);
            row.addView(reject);
            requestsList.addView(row);
        }
    }

    private void searchAndRequest(String keyword) {
        MainActivity activity = (MainActivity) requireActivity();
        if (keyword.isEmpty()) {
            activity.toast("请输入关键词");
            return;
        }
        if (activity.getCurrentUser() == null) {
            activity.toast("请先登录");
            return;
        }
        long myId = activity.getCurrentUser().getUserId();
        NetworkExecutor.run(() -> {
            try {
                List<UserProfile> users = activity.getApiClient().searchUsers(keyword);
                UserProfile target = users.stream()
                        .filter(u -> u.getUserId() != myId)
                        .findFirst()
                        .orElse(null);
                if (target == null) {
                    activity.toast("未找到可添加的用户");
                    return;
                }
                activity.getApiClient().sendFriendRequest(myId, target.getUserId());
                activity.toast("已向 " + target.getNickname() + " 发送申请");
                activity.runOnUiThread(this::refreshAll);
            } catch (Exception e) {
                activity.toast(msg(e));
            }
        });
    }

    private void respond(long requestId, boolean accept) {
        MainActivity activity = (MainActivity) requireActivity();
        NetworkExecutor.run(() -> {
            try {
                activity.getApiClient().respondFriendRequest(requestId, accept);
                activity.toast(accept ? "已同意" : "已拒绝");
                activity.runOnUiThread(this::refreshAll);
            } catch (Exception e) {
                activity.toast(msg(e));
            }
        });
    }

    private static String msg(Exception e) {
        return e.getMessage() == null ? "请求失败" : e.getMessage();
    }
}
