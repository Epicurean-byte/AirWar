package edu.hitsz.aircraftwar.android.ui;

import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import edu.hitsz.aircraftwar.android.MainActivity;
import edu.hitsz.aircraftwar.android.network.NetworkExecutor;
import edu.hitsz.aircraftwar.android.network.model.FriendRequestItem;
import edu.hitsz.aircraftwar.android.network.model.UserProfile;

public class FriendsFragment extends Fragment {
    private static final int TAB_FRIENDS = 0;
    private static final int TAB_REQUESTS = 1;
    private static final int TAB_SEARCH = 2;

    private Button friendsTab;
    private Button requestsTab;
    private Button searchTab;
    private LinearLayout friendsSection;
    private LinearLayout requestsSection;
    private LinearLayout searchSection;
    private LinearLayout friendsList;
    private LinearLayout requestsList;
    private LinearLayout searchResultsList;
    private EditText searchInput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);

        LinearLayout root = UiUtils.createScreenColumn(requireContext());
        scrollView.addView(root);

        root.addView(UiUtils.createTitle(requireContext(), "战友通讯录"));
        root.addView(UiUtils.createCaption(requireContext(), "管理好友、处理申请、检索玩家，统一从这里完成。"));

        LinearLayout shell = UiUtils.createPanel(requireContext());
        root.addView(shell);

        LinearLayout tabRow = UiUtils.createRow(requireContext());
        friendsTab = UiUtils.createTabButton(requireContext(), "好友列表", true);
        requestsTab = UiUtils.createTabButton(requireContext(), "申请处理", false);
        searchTab = UiUtils.createTabButton(requireContext(), "搜索玩家", false);
        tabRow.addView(friendsTab);
        tabRow.addView(requestsTab);
        searchTab.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        tabRow.addView(searchTab);
        shell.addView(tabRow);

        friendsSection = buildFriendsSection();
        requestsSection = buildRequestsSection();
        searchSection = buildSearchSection();
        shell.addView(friendsSection);
        shell.addView(requestsSection);
        shell.addView(searchSection);

        LinearLayout footer = UiUtils.createRow(requireContext());
        UiUtils.setTopMargin(footer, requireContext(), 16);
        Button refreshButton = UiUtils.createSmallButton(requireContext(), "刷新数据");
        refreshButton.setOnClickListener(v -> refreshAll());
        Button backButton = UiUtils.createSmallButton(requireContext(), "返回主菜单");
        backButton.setOnClickListener(v -> ((MainActivity) requireActivity()).showMainMenu());
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        refreshParams.rightMargin = UiUtils.dp(requireContext(), 8);
        refreshButton.setLayoutParams(refreshParams);
        backButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        footer.addView(refreshButton);
        footer.addView(backButton);
        shell.addView(footer);

        friendsTab.setOnClickListener(v -> selectTab(TAB_FRIENDS));
        requestsTab.setOnClickListener(v -> selectTab(TAB_REQUESTS));
        searchTab.setOnClickListener(v -> selectTab(TAB_SEARCH));

        selectTab(TAB_FRIENDS);
        refreshAll();
        return scrollView;
    }

    private LinearLayout buildFriendsSection() {
        LinearLayout section = UiUtils.createPanel(requireContext(), 14);
        section.addView(UiUtils.createSectionTitle(requireContext(), "好友编组"));
        section.addView(UiUtils.createCaption(requireContext(), "查看好友在线状态、战绩和当前财富。"));
        friendsList = new LinearLayout(requireContext());
        friendsList.setOrientation(LinearLayout.VERTICAL);
        UiUtils.setTopMargin(friendsList, requireContext(), 10);
        section.addView(friendsList);
        return section;
    }

    private LinearLayout buildRequestsSection() {
        LinearLayout section = UiUtils.createPanel(requireContext(), 14);
        section.addView(UiUtils.createSectionTitle(requireContext(), "待处理申请"));
        section.addView(UiUtils.createCaption(requireContext(), "快速同意或拒绝收到的好友申请。"));
        requestsList = new LinearLayout(requireContext());
        requestsList.setOrientation(LinearLayout.VERTICAL);
        UiUtils.setTopMargin(requestsList, requireContext(), 10);
        section.addView(requestsList);
        return section;
    }

    private LinearLayout buildSearchSection() {
        LinearLayout section = UiUtils.createPanel(requireContext(), 14);
        section.addView(UiUtils.createSectionTitle(requireContext(), "搜索新战友"));
        section.addView(UiUtils.createCaption(requireContext(), "支持用户名或昵称检索，结果会单独列出。"));

        searchInput = UiUtils.createTextInput(requireContext(), "输入用户名 / 昵称");
        searchInput.setInputType(InputType.TYPE_CLASS_TEXT);
        section.addView(searchInput);

        Button searchButton = UiUtils.createActionButton(requireContext(), "搜索玩家");
        searchButton.setOnClickListener(v -> searchUsers(searchInput.getText().toString().trim()));
        section.addView(searchButton);

        searchResultsList = new LinearLayout(requireContext());
        searchResultsList.setOrientation(LinearLayout.VERTICAL);
        UiUtils.setTopMargin(searchResultsList, requireContext(), 10);
        section.addView(searchResultsList);
        return section;
    }

    private void selectTab(int tab) {
        UiUtils.applyTabStyle(friendsTab, tab == TAB_FRIENDS);
        UiUtils.applyTabStyle(requestsTab, tab == TAB_REQUESTS);
        UiUtils.applyTabStyle(searchTab, tab == TAB_SEARCH);
        friendsSection.setVisibility(tab == TAB_FRIENDS ? View.VISIBLE : View.GONE);
        requestsSection.setVisibility(tab == TAB_REQUESTS ? View.VISIBLE : View.GONE);
        searchSection.setVisibility(tab == TAB_SEARCH ? View.VISIBLE : View.GONE);
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
                activity.runOnUiThread(() -> {
                    renderFriends(friends);
                    renderRequests(requests);
                });
            } catch (Exception e) {
                activity.toast(msg(e));
            }
        });
    }

    private void renderFriends(List<UserProfile> friends) {
        friendsList.removeAllViews();
        if (friends.isEmpty()) {
            friendsList.addView(UiUtils.createCaption(requireContext(), "暂无好友，去搜索区添加新的战友。"));
            return;
        }
        for (UserProfile friend : friends) {
            friendsList.addView(createFriendCard(friend));
        }
    }

    private View createFriendCard(UserProfile friend) {
        LinearLayout card = UiUtils.createPanel(requireContext(), 12);

        LinearLayout topRow = UiUtils.createRow(requireContext());
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = UiUtils.createBody(requireContext(), friend.getNickname());
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        name.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        name.setLayoutParams(nameParams);
        topRow.addView(name);
        topRow.addView(UiUtils.createChip(
                requireContext(),
                friend.isOnline() ? "在线" : "离线",
                friend.isOnline() ? UiUtils.colorPositive() : UiUtils.colorNegative()
        ));
        card.addView(topRow);

        TextView account = UiUtils.createCaption(requireContext(), "账号: " + friend.getUsername() + "  皮肤#" + friend.getEquippedSkinId());
        UiUtils.setTopMargin(account, requireContext(), 4);
        card.addView(account);

        LinearLayout stats = UiUtils.createRow(requireContext());
        UiUtils.setTopMargin(stats, requireContext(), 10);
        stats.addView(UiUtils.createChip(requireContext(), "最高分 " + friend.getHighScore(), UiUtils.colorGold()));
        TextView coins = UiUtils.createChip(requireContext(), "金币 " + friend.getCoins(), UiUtils.colorAccent());
        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chipParams.leftMargin = UiUtils.dp(requireContext(), 8);
        coins.setLayoutParams(chipParams);
        stats.addView(coins);
        card.addView(stats);
        return card;
    }

    private void renderRequests(List<FriendRequestItem> requests) {
        requestsList.removeAllViews();
        if (requests.isEmpty()) {
            requestsList.addView(UiUtils.createCaption(requireContext(), "当前没有待处理的好友申请。"));
            return;
        }
        for (FriendRequestItem request : requests) {
            requestsList.addView(createRequestCard(request));
        }
    }

    private View createRequestCard(FriendRequestItem request) {
        LinearLayout card = UiUtils.createPanel(requireContext(), 12);
        card.addView(UiUtils.createBody(requireContext(), request.getFromNickname()));
        TextView subtitle = UiUtils.createCaption(requireContext(), "来自账号 " + request.getFromUsername() + " / 请求#" + request.getRequestId());
        UiUtils.setTopMargin(subtitle, requireContext(), 4);
        card.addView(subtitle);

        LinearLayout actions = UiUtils.createRow(requireContext());
        UiUtils.setTopMargin(actions, requireContext(), 12);
        Button accept = UiUtils.createSmallButton(requireContext(), "同意");
        accept.setOnClickListener(v -> respond(request.getRequestId(), true));
        Button reject = UiUtils.createSmallButton(requireContext(), "拒绝");
        reject.setOnClickListener(v -> respond(request.getRequestId(), false));
        LinearLayout.LayoutParams acceptParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        acceptParams.rightMargin = UiUtils.dp(requireContext(), 8);
        accept.setLayoutParams(acceptParams);
        reject.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        actions.addView(accept);
        actions.addView(reject);
        card.addView(actions);
        return card;
    }

    private void searchUsers(String keyword) {
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
                List<UserProfile> users = activity.getApiClient().searchUsers(keyword).stream()
                        .filter(user -> user.getUserId() != myId)
                        .toList();
                activity.runOnUiThread(() -> renderSearchResults(users));
            } catch (Exception e) {
                activity.toast(msg(e));
            }
        });
    }

    private void renderSearchResults(List<UserProfile> users) {
        searchResultsList.removeAllViews();
        if (users.isEmpty()) {
            searchResultsList.addView(UiUtils.createCaption(requireContext(), "没有匹配到可添加的玩家。"));
            return;
        }
        for (UserProfile user : users) {
            searchResultsList.addView(createSearchCard(user));
        }
    }

    private View createSearchCard(UserProfile user) {
        LinearLayout card = UiUtils.createPanel(requireContext(), 12);
        LinearLayout topRow = UiUtils.createRow(requireContext());
        TextView name = UiUtils.createBody(requireContext(), user.getNickname());
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        name.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        topRow.addView(name);
        topRow.addView(UiUtils.createChip(
                requireContext(),
                user.isOnline() ? "在线" : "离线",
                user.isOnline() ? UiUtils.colorPositive() : UiUtils.colorNegative()
        ));
        card.addView(topRow);

        TextView account = UiUtils.createCaption(requireContext(), "账号: " + user.getUsername());
        UiUtils.setTopMargin(account, requireContext(), 4);
        card.addView(account);

        Button addButton = UiUtils.createActionButton(requireContext(), "发送好友申请");
        addButton.setOnClickListener(v -> sendFriendRequest(user));
        card.addView(addButton);
        return card;
    }

    private void sendFriendRequest(UserProfile user) {
        MainActivity activity = (MainActivity) requireActivity();
        if (activity.getCurrentUser() == null) {
            activity.toast("请先登录");
            return;
        }
        long myId = activity.getCurrentUser().getUserId();
        NetworkExecutor.run(() -> {
            try {
                activity.getApiClient().sendFriendRequest(myId, user.getUserId());
                activity.toast("已向 " + user.getNickname() + " 发送申请");
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
