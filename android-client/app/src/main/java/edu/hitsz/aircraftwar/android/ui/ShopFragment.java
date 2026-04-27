package edu.hitsz.aircraftwar.android.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
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

import edu.hitsz.aircraftwar.android.MainActivity;
import edu.hitsz.aircraftwar.android.network.NetworkExecutor;
import edu.hitsz.aircraftwar.android.network.model.ShopInfo;
import edu.hitsz.aircraftwar.android.network.model.ShopSkin;

public class ShopFragment extends Fragment {
    private TextView walletView;
    private TextView equippedView;
    private GridLayout skinGrid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);

        LinearLayout root = UiUtils.createScreenColumn(requireContext());
        scrollView.addView(root);

        root.addView(UiUtils.createTitle(requireContext(), "机库商城"));
        root.addView(UiUtils.createCaption(requireContext(), "接入 shop_items 全部商品。01-08 为飞机皮肤，09-16 为纪念藏品。"));

        LinearLayout headerPanel = UiUtils.createPanel(requireContext());
        headerPanel.addView(UiUtils.createSectionTitle(requireContext(), "后勤面板"));
        walletView = UiUtils.createBody(requireContext(), "金币储备: --");
        equippedView = UiUtils.createCaption(requireContext(), "当前装扮: --");
        headerPanel.addView(walletView);
        headerPanel.addView(equippedView);

        LinearLayout toolbar = UiUtils.createRow(requireContext());
        UiUtils.setTopMargin(toolbar, requireContext(), 12);
        Button refresh = UiUtils.createSmallButton(requireContext(), "刷新库存");
        refresh.setOnClickListener(v -> loadShop());
        Button back = UiUtils.createSmallButton(requireContext(), "返回主菜单");
        back.setOnClickListener(v -> ((MainActivity) requireActivity()).showMainMenu());
        Button warehouse = UiUtils.createSmallButton(requireContext(), "打开仓库");
        warehouse.setOnClickListener(v -> ((MainActivity) requireActivity()).showWarehouse());
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        leftParams.rightMargin = UiUtils.dp(requireContext(), 8);
        refresh.setLayoutParams(leftParams);
        LinearLayout.LayoutParams midParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        midParams.rightMargin = UiUtils.dp(requireContext(), 8);
        warehouse.setLayoutParams(midParams);
        back.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        toolbar.addView(refresh);
        toolbar.addView(warehouse);
        toolbar.addView(back);
        headerPanel.addView(toolbar);
        root.addView(headerPanel);

        LinearLayout gridPanel = UiUtils.createPanel(requireContext());
        gridPanel.addView(UiUtils.createSectionTitle(requireContext(), "上架商品"));
        gridPanel.addView(UiUtils.createCaption(requireContext(), "商城负责购买，仓库负责陈列和装扮。皮肤与纪念品共用统一卡片系统。"));

        skinGrid = new GridLayout(requireContext());
        skinGrid.setColumnCount(2);
        UiUtils.setTopMargin(skinGrid, requireContext(), 12);
        gridPanel.addView(skinGrid);
        root.addView(gridPanel);

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
        ((MainActivity) requireActivity()).applyServerShopState(info.getCoins(), info.getEquippedSkinId());
        walletView.setText("金币储备: " + info.getCoins());
        ShopSkin equipped = info.getSkins().stream()
                .filter(item -> item.getSkinId() == info.getEquippedSkinId())
                .findFirst()
                .orElse(null);
        equippedView.setText("当前装扮: " + (equipped == null ? "默认战机" : equipped.getName()));
        skinGrid.removeAllViews();

        if (info.getSkins().isEmpty()) {
            skinGrid.addView(UiUtils.createCaption(requireContext(), "当前没有可展示的皮肤。"));
            return;
        }

        info.getSkins().stream()
                .filter(item -> item.getSkinId() != 0)
                .sorted(Comparator.comparingInt(ShopSkin::getSkinId))
                .forEach(skin -> skinGrid.addView(createSkinCard(info, skin)));
    }

    private View createSkinCard(ShopInfo info, ShopSkin skin) {
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
        preview.setImageResource(ShopItemVisuals.resolvePreviewResId(skin.getAssetName()));
        preview.setBackground(UiUtils.createPanel(requireContext(), 8).getBackground());
        preview.setPadding(UiUtils.dp(requireContext(), 10), UiUtils.dp(requireContext(), 10),
                UiUtils.dp(requireContext(), 10), UiUtils.dp(requireContext(), 10));
        card.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UiUtils.dp(requireContext(), 130)
        ));

        LinearLayout topRow = UiUtils.createRow(requireContext());
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        UiUtils.setTopMargin(topRow, requireContext(), 12);
        String itemLabel = skin.isPlaneSkin() ? "SKIN-" + skin.getSkinId() : "MUSEUM-" + skin.getSkinId();
        TextView code = UiUtils.createChip(requireContext(), itemLabel, skin.isPlaneSkin() ? UiUtils.colorAccent() : UiUtils.colorGold());
        topRow.addView(code);
        if (skin.getSkinId() == info.getEquippedSkinId()) {
            TextView equipChip = UiUtils.createChip(requireContext(), "已装备", UiUtils.colorPositive());
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            chipParams.leftMargin = UiUtils.dp(requireContext(), 8);
            equipChip.setLayoutParams(chipParams);
            topRow.addView(equipChip);
        } else if (skin.isOwned()) {
            TextView ownChip = UiUtils.createChip(requireContext(), "已持有", UiUtils.colorGold());
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            chipParams.leftMargin = UiUtils.dp(requireContext(), 8);
            ownChip.setLayoutParams(chipParams);
            topRow.addView(ownChip);
        }
        card.addView(topRow);

        TextView nameView = UiUtils.createBody(requireContext(), skin.getName());
        nameView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        UiUtils.setTopMargin(nameView, requireContext(), 12);
        card.addView(nameView);

        TextView assetName = UiUtils.createCaption(requireContext(), skin.isPlaneSkin() ? "飞机皮肤" : "纪念品");
        UiUtils.setTopMargin(assetName, requireContext(), 4);
        card.addView(assetName);

        TextView desc = UiUtils.createCaption(requireContext(), skin.getDescription());
        UiUtils.setTopMargin(desc, requireContext(), 10);
        card.addView(desc);

        TextView price = UiUtils.createBody(requireContext(), skin.getPrice() == 0
                ? "基础配发"
                : "价格 " + skin.getPrice() + " 金币");
        price.setTextColor(UiUtils.colorGold());
        UiUtils.setTopMargin(price, requireContext(), 12);
        card.addView(price);

        LinearLayout actions = UiUtils.createRow(requireContext());
        UiUtils.setTopMargin(actions, requireContext(), 12);

        Button buyButton = UiUtils.createSmallButton(requireContext(), skin.isOwned() ? "已拥有" : "购买");
        buyButton.setEnabled(!skin.isOwned());
        buyButton.setAlpha(skin.isOwned() ? 0.55f : 1.0f);
        buyButton.setOnClickListener(v -> buySkin(skin.getSkinId()));

        String actionText = skin.isPlaneSkin()
                ? (skin.getSkinId() == info.getEquippedSkinId() ? "使用中" : "装扮")
                : "入库欣赏";
        Button equipButton = UiUtils.createSmallButton(requireContext(), actionText);
        equipButton.setEnabled(skin.isPlaneSkin() && skin.isOwned() && skin.getSkinId() != info.getEquippedSkinId());
        equipButton.setAlpha(equipButton.isEnabled() ? 1.0f : 0.55f);
        equipButton.setOnClickListener(v -> equipSkin(skin.getSkinId()));

        LinearLayout.LayoutParams buyParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        buyParams.rightMargin = UiUtils.dp(requireContext(), 8);
        buyButton.setLayoutParams(buyParams);
        equipButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        actions.addView(buyButton);
        actions.addView(equipButton);
        card.addView(actions);

        return card;
    }

    private void buySkin(int skinId) {
        MainActivity activity = (MainActivity) requireActivity();
        if (activity.getCurrentUser() == null) {
            activity.toast("请先登录");
            return;
        }
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
        if (activity.getCurrentUser() == null) {
            activity.toast("请先登录");
            return;
        }
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
