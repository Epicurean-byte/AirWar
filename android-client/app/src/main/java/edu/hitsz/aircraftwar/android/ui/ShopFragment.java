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

import edu.hitsz.aircraftwar.android.MainActivity;
import edu.hitsz.aircraftwar.android.network.NetworkExecutor;
import edu.hitsz.aircraftwar.android.network.model.ShopInfo;
import edu.hitsz.aircraftwar.android.network.model.ShopSkin;

public class ShopFragment extends Fragment {
    private LinearLayout skinList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout root = UiUtils.createScreenColumn(requireContext());
        scrollView.addView(root);

        root.addView(UiUtils.createTitle(requireContext(), "在线商城"));
        var refresh = UiUtils.createActionButton(requireContext(), "刷新商城");
        refresh.setOnClickListener(v -> loadShop());
        root.addView(refresh);

        skinList = new LinearLayout(requireContext());
        skinList.setOrientation(LinearLayout.VERTICAL);
        root.addView(skinList);

        var back = UiUtils.createActionButton(requireContext(), "返回主菜单");
        back.setOnClickListener(v -> ((MainActivity) requireActivity()).showMainMenu());
        root.addView(back);

        loadShop();
        return scrollView;
    }

    private void loadShop() {
        MainActivity activity = (MainActivity) requireActivity();
        if (activity.getCurrentUser() == null) {
            activity.toast("请先登录");
            return;
        }
        long userId = activity.getCurrentUser().getUserId();
        NetworkExecutor.run(() -> {
            try {
                ShopInfo info = activity.getApiClient().getShopInfo(userId);
                activity.runOnUiThread(() -> render(info));
            } catch (Exception e) {
                activity.toast(msg(e));
            }
        });
    }

    private void render(ShopInfo info) {
        skinList.removeAllViews();
        skinList.addView(UiUtils.createBody(requireContext(), "当前金币: " + info.getCoins()));
        skinList.addView(UiUtils.createBody(requireContext(), "当前装备皮肤: #" + info.getEquippedSkinId()));

        for (ShopSkin skin : info.getSkins()) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.addView(UiUtils.createBody(requireContext(),
                    "#" + skin.getSkinId() + " " + skin.getName() + " 价格=" + skin.getPrice()
                            + " owned=" + skin.isOwned() + " asset=" + skin.getAssetName()));
            row.addView(UiUtils.createBody(requireContext(), skin.getDescription()));

            var buy = UiUtils.createActionButton(requireContext(), "购买#" + skin.getSkinId());
            buy.setOnClickListener(v -> buySkin(skin.getSkinId()));
            row.addView(buy);

            var equip = UiUtils.createActionButton(requireContext(), "装备#" + skin.getSkinId());
            equip.setOnClickListener(v -> equipSkin(skin.getSkinId()));
            row.addView(equip);

            skinList.addView(row);
        }
    }

    private void buySkin(int skinId) {
        MainActivity activity = (MainActivity) requireActivity();
        long userId = activity.getCurrentUser().getUserId();
        NetworkExecutor.run(() -> {
            try {
                activity.getApiClient().buySkin(userId, skinId);
                activity.toast("购买成功");
                activity.runOnUiThread(this::loadShop);
            } catch (Exception e) {
                activity.toast(msg(e));
            }
        });
    }

    private void equipSkin(int skinId) {
        MainActivity activity = (MainActivity) requireActivity();
        long userId = activity.getCurrentUser().getUserId();
        NetworkExecutor.run(() -> {
            try {
                activity.getApiClient().equipSkin(userId, skinId);
                activity.toast("装备成功");
                activity.runOnUiThread(this::loadShop);
            } catch (Exception e) {
                activity.toast(msg(e));
            }
        });
    }

    private static String msg(Exception e) {
        return e.getMessage() == null ? "请求失败" : e.getMessage();
    }
}
