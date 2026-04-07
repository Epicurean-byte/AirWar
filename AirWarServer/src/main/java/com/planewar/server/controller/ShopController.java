package com.planewar.server.controller;

import com.planewar.server.datastore.InMemoryDataStore;
import com.planewar.server.model.dto.ApiResponse;
import com.planewar.server.model.entity.SkinConfig;
import com.planewar.server.model.entity.User;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 商城 HTTP 接口：查询、购买、装备皮肤与纪念品。
 */
@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final InMemoryDataStore store;

    public ShopController(InMemoryDataStore store) {
        this.store = store;
    }

    /**
     * 获取商城数据：返回用户余额、所有商品列表（含是否已拥有标记）、当前装备皮肤。
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> shopInfo(@RequestParam long userId) {
        Optional<User> opt = store.findById(userId);
        if (opt.isEmpty()) return ApiResponse.fail("用户不存在");
        User user = opt.get();

        List<Map<String, Object>> skins = store.skinCatalog.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("skinId", s.getSkinId());
            m.put("name", s.getName());
            m.put("description", s.getDescription());
            m.put("price", s.getPrice());
            m.put("category", s.getCategory());
            m.put("equippable", s.isEquippable());
            m.put("assetName", s.getAssetName());
            m.put("owned", user.getOwnedSkins().contains(s.getSkinId()));
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> resp = new HashMap<>();
        resp.put("coins", user.getCoins());
        resp.put("equippedSkinId", user.getEquippedSkinId());
        resp.put("skins", skins);
        return ApiResponse.ok(resp);
    }

    /**
     * 购买商品。
     */
    @PostMapping("/buy")
    public ApiResponse<Map<String, Object>> buy(@RequestBody Map<String, Object> body) {
        long userId = ((Number) body.get("userId")).longValue();
        int skinId = ((Number) body.get("skinId")).intValue();

        Optional<User> opt = store.findById(userId);
        if (opt.isEmpty()) return ApiResponse.fail("用户不存在");
        User user = opt.get();

        Optional<SkinConfig> skinOpt = store.findSkinById(skinId);
        if (skinOpt.isEmpty()) return ApiResponse.fail("商品不存在");
        SkinConfig skin = skinOpt.get();

        if (user.getOwnedSkins().contains(skinId)) return ApiResponse.fail("已拥有该物品");
        if (user.getCoins() < skin.getPrice()) return ApiResponse.fail("金币不足");

        synchronized (user) {
            if (user.getCoins() < skin.getPrice()) return ApiResponse.fail("金币不足");
            user.setCoins(user.getCoins() - skin.getPrice());
            user.getOwnedSkins().add(skinId);
        }

        store.flushUsers();
        Map<String, Object> resp = new HashMap<>();
        resp.put("coins", user.getCoins());
        resp.put("ownedItems", user.getOwnedSkins());
        return ApiResponse.ok(resp);
    }

    /**
     * 装备飞机皮肤。
     */
    @PostMapping("/equip")
    public ApiResponse<Void> equip(@RequestBody Map<String, Object> body) {
        long userId = ((Number) body.get("userId")).longValue();
        int skinId = ((Number) body.get("skinId")).intValue();

        Optional<User> opt = store.findById(userId);
        if (opt.isEmpty()) return ApiResponse.fail("用户不存在");
        User user = opt.get();

        Optional<SkinConfig> skinOpt = store.findSkinById(skinId);
        if (skinOpt.isEmpty()) return ApiResponse.fail("商品不存在");
        SkinConfig skin = skinOpt.get();
        if (!skin.isEquippable()) return ApiResponse.fail("该物品不可装备");
        if (!user.getOwnedSkins().contains(skinId)) return ApiResponse.fail("未拥有该皮肤");
        user.setEquippedSkinId(skinId);
        store.flushUsers();
        return ApiResponse.ok();
    }
}
