package edu.hitsz.aircraftwar.android.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.hitsz.aircraftwar.android.MainActivity;
import edu.hitsz.aircraftwar.android.network.LocalInventoryManager;
import edu.hitsz.aircraftwar.android.network.NetworkExecutor;
import edu.hitsz.aircraftwar.android.network.model.LocalInventorySnapshot;
import edu.hitsz.aircraftwar.android.network.model.ShopCatalog;
import edu.hitsz.aircraftwar.android.network.model.ShopInfo;
import edu.hitsz.aircraftwar.android.network.model.ShopSkin;

public class WarehouseFragment extends Fragment {
    private TextView walletView;
    private TextView equippedView;
    private GridLayout skinGrid;
    private GridLayout memorabiliaGrid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);

        LinearLayout root = UiUtils.createScreenColumn(requireContext());
        scrollView.addView(root);

        root.addView(UiUtils.createTitle(requireContext(), "仓库"));
        root.addView(UiUtils.createCaption(requireContext(), "已拥有的飞机皮肤和纪念藏品都集中在这里管理。"));

        LinearLayout summaryPanel = UiUtils.createPanel(requireContext());
        summaryPanel.addView(UiUtils.createSectionTitle(requireContext(), "库存总览"));
        walletView = UiUtils.createBody(requireContext(), "金币储备: --");
        equippedView = UiUtils.createCaption(requireContext(), "当前装扮: 默认战机");
        summaryPanel.addView(walletView);
        summaryPanel.addView(equippedView);

        LinearLayout toolbar = UiUtils.createRow(requireContext());
        UiUtils.setTopMargin(toolbar, requireContext(), 12);
        Button refreshButton = UiUtils.createSmallButton(requireContext(), "刷新仓库");
        refreshButton.setOnClickListener(v -> loadInventory());
        Button backButton = UiUtils.createSmallButton(requireContext(), "返回主菜单");
        backButton.setOnClickListener(v -> ((MainActivity) requireActivity()).showMainMenu());
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        leftParams.rightMargin = UiUtils.dp(requireContext(), 8);
        refreshButton.setLayoutParams(leftParams);
        backButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        toolbar.addView(refreshButton);
        toolbar.addView(backButton);
        summaryPanel.addView(toolbar);
        root.addView(summaryPanel);

        LinearLayout skinPanel = UiUtils.createPanel(requireContext());
        skinPanel.addView(UiUtils.createSectionTitle(requireContext(), "飞机皮肤仓"));
        skinPanel.addView(UiUtils.createCaption(requireContext(), "01-08 购买后会进入这里，可随时切换为当前装扮。"));
        skinGrid = new GridLayout(requireContext());
        skinGrid.setColumnCount(2);
        UiUtils.setTopMargin(skinGrid, requireContext(), 12);
        skinPanel.addView(skinGrid);
        root.addView(skinPanel);

        LinearLayout souvenirPanel = UiUtils.createPanel(requireContext());
        souvenirPanel.addView(UiUtils.createSectionTitle(requireContext(), "纪念品陈列柜"));
        souvenirPanel.addView(UiUtils.createCaption(requireContext(), "09-16 仅供收藏与欣赏，不参与战机装扮。"));
        memorabiliaGrid = new GridLayout(requireContext());
        memorabiliaGrid.setColumnCount(2);
        UiUtils.setTopMargin(memorabiliaGrid, requireContext(), 12);
        souvenirPanel.addView(memorabiliaGrid);
        root.addView(souvenirPanel);

        loadInventory();
        return scrollView;
    }

    private void loadInventory() {
        MainActivity activity = (MainActivity) requireActivity();
        if (activity.getCurrentUser() == null) {
            activity.toast("请先登录");
            return;
        }
        long userId = activity.getCurrentUser().getUserId();
        NetworkExecutor.run(() -> {
            try {
                ShopInfo remote = activity.getApiClient().getShopInfo(userId);
                ShopInfo merged = mergeWithLocal(activity, userId, remote);
                activity.runOnUiThread(() -> render(merged));
            } catch (Exception e) {
                activity.toast(msg(e));
            }
        });
    }

    private ShopInfo mergeWithLocal(MainActivity activity, long userId, ShopInfo remote) {
        LocalInventoryManager inventoryManager = activity.getLocalInventoryManager();
        Set<Integer> remoteOwned = remote.getSkins().stream()
                .filter(ShopSkin::isOwned)
                .map(ShopSkin::getSkinId)
                .collect(Collectors.toSet());
        inventoryManager.ensureInitialized(userId, remote.getCoins(), remote.getEquippedSkinId(), remoteOwned);
        LocalInventorySnapshot snapshot = inventoryManager.snapshot(userId);
        return ShopCatalog.merge(remote, snapshot);
    }

    private void render(ShopInfo info) {
        ((MainActivity) requireActivity()).updateCoinsAndEquippedSkin(info.getCoins(), info.getEquippedSkinId());
        walletView.setText("金币储备: " + info.getCoins());
        ShopSkin equipped = info.getSkins().stream()
                .filter(item -> item.getSkinId() == info.getEquippedSkinId())
                .findFirst()
                .orElse(null);
        equippedView.setText("当前装扮: " + (equipped == null ? "默认战机" : equipped.getName()));

        skinGrid.removeAllViews();
        memorabiliaGrid.removeAllViews();

        List<ShopSkin> ownedPlaneSkins = info.getSkins().stream()
                .filter(ShopSkin::isOwned)
                .filter(ShopSkin::isPlaneSkin)
                .sorted(Comparator.comparingInt(ShopSkin::getSkinId))
                .collect(Collectors.toList());
        List<ShopSkin> ownedMemorabilia = info.getSkins().stream()
                .filter(ShopSkin::isOwned)
                .filter(ShopSkin::isMemorabilia)
                .sorted(Comparator.comparingInt(ShopSkin::getSkinId))
                .collect(Collectors.toList());

        if (ownedPlaneSkins.isEmpty()) {
            skinGrid.addView(UiUtils.createCaption(requireContext(), "目前还没有可用皮肤。先去商城采购。"));
        } else {
            for (ShopSkin item : ownedPlaneSkins) {
                skinGrid.addView(createWarehouseCard(info, item));
            }
        }

        if (ownedMemorabilia.isEmpty()) {
            memorabiliaGrid.addView(UiUtils.createCaption(requireContext(), "纪念品陈列柜暂时为空。"));
        } else {
            for (ShopSkin item : ownedMemorabilia) {
                memorabiliaGrid.addView(createWarehouseCard(info, item));
            }
        }
    }

    private View createWarehouseCard(ShopInfo info, ShopSkin item) {
        LinearLayout card = UiUtils.createPanel(requireContext(), 14);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(0, UiUtils.dp(requireContext(), 10), UiUtils.dp(requireContext(), 10), 0);
        card.setLayoutParams(params);

        ImageView preview = new ImageView(requireContext());
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setAdjustViewBounds(true);
        preview.setImageResource(ShopItemVisuals.resolvePreviewResId(item.getAssetName()));
        preview.setBackground(UiUtils.createPanel(requireContext(), 8).getBackground());
        preview.setPadding(UiUtils.dp(requireContext(), 10), UiUtils.dp(requireContext(), 10),
                UiUtils.dp(requireContext(), 10), UiUtils.dp(requireContext(), 10));
        card.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UiUtils.dp(requireContext(), 130)
        ));

        TextView nameView = UiUtils.createBody(requireContext(), item.getName());
        nameView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        UiUtils.setTopMargin(nameView, requireContext(), 12);
        card.addView(nameView);

        TextView tag = UiUtils.createChip(
                requireContext(),
                item.isPlaneSkin() ? "飞机皮肤" : "纪念品",
                item.isPlaneSkin() ? UiUtils.colorAccent() : UiUtils.colorGold()
        );
        UiUtils.setTopMargin(tag, requireContext(), 8);
        card.addView(tag);

        TextView descView = UiUtils.createCaption(requireContext(), item.getDescription());
        UiUtils.setTopMargin(descView, requireContext(), 10);
        card.addView(descView);

        if (item.isPlaneSkin()) {
            Button equipButton = UiUtils.createSmallButton(
                    requireContext(),
                    item.getSkinId() == info.getEquippedSkinId() ? "使用中" : "装扮"
            );
            equipButton.setEnabled(item.getSkinId() != info.getEquippedSkinId());
            equipButton.setAlpha(equipButton.isEnabled() ? 1f : 0.55f);
            equipButton.setOnClickListener(v -> equipSkin(item.getSkinId()));
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            buttonParams.topMargin = UiUtils.dp(requireContext(), 12);
            equipButton.setLayoutParams(buttonParams);
            card.addView(equipButton);
        } else {
            TextView memoView = UiUtils.createCaption(requireContext(), "已收录，可在仓库中欣赏。");
            memoView.setGravity(Gravity.CENTER_HORIZONTAL);
            UiUtils.setTopMargin(memoView, requireContext(), 12);
            card.addView(memoView);
        }

        return card;
    }

    private void equipSkin(int skinId) {
        MainActivity activity = (MainActivity) requireActivity();
        if (activity.getCurrentUser() == null) {
            activity.toast("请先登录");
            return;
        }
        long userId = activity.getCurrentUser().getUserId();
        NetworkExecutor.run(() -> {
            try {
                try {
                    activity.getApiClient().equipSkin(userId, skinId);
                } catch (Exception e) {
                    String error = msg(e);
                    if (!error.contains("商品不存在") && !error.contains("不可装备") && !error.contains("未拥有")) {
                        throw e;
                    }
                }
                LocalInventorySnapshot updated = activity.getLocalInventoryManager().equip(userId, skinId);
                activity.updateCoinsAndEquippedSkin(updated.getCoins(), skinId);
                activity.toast("装扮已切换");
                activity.runOnUiThread(this::loadInventory);
            } catch (Exception e) {
                activity.toast(msg(e));
            }
        });
    }

    private static String msg(Exception e) {
        return e.getMessage() == null ? "请求失败" : e.getMessage();
    }
}
