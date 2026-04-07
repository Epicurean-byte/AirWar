package edu.hitsz.aircraftwar.android.network;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import edu.hitsz.aircraftwar.android.network.model.FriendRequestItem;
import edu.hitsz.aircraftwar.android.network.model.LeaderboardEntry;
import edu.hitsz.aircraftwar.android.network.model.ShopInfo;
import edu.hitsz.aircraftwar.android.network.model.ShopSkin;
import edu.hitsz.aircraftwar.android.network.model.UserProfile;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;

public final class HttpApiClient {
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();

    public UserProfile login(String username, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("password", password);
        JSONObject data = post("/api/user/login", body);
        return parseUserProfile(data);
    }

    public UserProfile register(String username, String password, String nickname) throws Exception {
        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("password", password);
        body.put("nickname", nickname);
        JSONObject data = post("/api/user/register", body);
        return parseUserProfile(data);
    }

    public void logout(long userId) throws Exception {
        Map<String, String> query = new HashMap<>();
        query.put("userId", String.valueOf(userId));
        post("/api/user/logout", query, new JSONObject());
    }

    public List<UserProfile> searchUsers(String keyword) throws Exception {
        Map<String, String> query = new HashMap<>();
        query.put("keyword", keyword);
        JSONArray arr = get("/api/user/search", query).getJSONArray("items");
        return parseUserList(arr);
    }

    public List<UserProfile> getFriends(long userId) throws Exception {
        Map<String, String> query = new HashMap<>();
        query.put("userId", String.valueOf(userId));
        JSONArray arr = get("/api/user/friends", query).getJSONArray("items");
        return parseUserList(arr);
    }

    public List<FriendRequestItem> getFriendRequests(long userId) throws Exception {
        Map<String, String> query = new HashMap<>();
        query.put("userId", String.valueOf(userId));
        JSONArray arr = get("/api/user/friend/requests", query).getJSONArray("items");
        List<FriendRequestItem> result = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            result.add(new FriendRequestItem(
                    o.optLong("requestId", 0L),
                    o.optLong("fromUserId", 0L),
                    o.optString("fromNickname", ""),
                    o.optString("fromUsername", "")
            ));
        }
        return result;
    }

    public void sendFriendRequest(long fromUserId, long toUserId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("fromUserId", fromUserId);
        body.put("toUserId", toUserId);
        post("/api/user/friend/request", body);
    }

    public void respondFriendRequest(long requestId, boolean accept) throws Exception {
        JSONObject body = new JSONObject();
        body.put("requestId", requestId);
        body.put("accept", accept);
        post("/api/user/friend/respond", body);
    }

    public ShopInfo getShopInfo(long userId) throws Exception {
        Map<String, String> query = new HashMap<>();
        query.put("userId", String.valueOf(userId));
        JSONObject data = get("/api/shop/info", query);
        long coins = data.optLong("coins", 0L);
        int equippedSkinId = data.optInt("equippedSkinId", 0);
        JSONArray skinsJson = data.optJSONArray("skins");
        List<ShopSkin> skins = new ArrayList<>();
        if (skinsJson != null) {
            for (int i = 0; i < skinsJson.length(); i++) {
                JSONObject s = skinsJson.getJSONObject(i);
                skins.add(new ShopSkin(
                        s.optInt("skinId", 0),
                        s.optString("name", ""),
                        s.optString("description", ""),
                        s.optLong("price", 0L),
                        s.optString("category", ""),
                        s.optBoolean("equippable", false),
                        s.optString("assetName", ""),
                        s.optBoolean("owned", false)
                ));
            }
        }
        return new ShopInfo(coins, equippedSkinId, skins);
    }

    public void buySkin(long userId, int skinId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("userId", userId);
        body.put("skinId", skinId);
        post("/api/shop/buy", body);
    }

    public void equipSkin(long userId, int skinId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("userId", userId);
        body.put("skinId", skinId);
        post("/api/shop/equip", body);
    }

    public List<LeaderboardEntry> scoreLeaderboard() throws Exception {
        JSONArray arr = get("/api/leaderboard/score", Collections.emptyMap()).getJSONArray("items");
        return parseLeaderboard(arr);
    }

    public List<LeaderboardEntry> coinLeaderboard() throws Exception {
        JSONArray arr = get("/api/leaderboard/coins", Collections.emptyMap()).getJSONArray("items");
        return parseLeaderboard(arr);
    }

    public void settleSingle(long userId, long score, long coins) throws Exception {
        JSONObject body = new JSONObject();
        body.put("userId", userId);
        body.put("score", score);
        body.put("coins", coins);
        post("/api/game/settle/single", body);
    }

    public void settlePvp(long roomId, long userId, long score, long coins) throws Exception {
        JSONObject body = new JSONObject();
        body.put("roomId", roomId);
        body.put("userId", userId);
        body.put("score", score);
        body.put("coins", coins);
        post("/api/game/settle/pvp", body);
    }

    private JSONObject get(String path, Map<String, String> query) throws Exception {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(ServerConfigManager.getInstance().getHttpBaseUrl() + path).newBuilder();
        for (Map.Entry<String, String> entry : query.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        Request request = new Request.Builder().url(urlBuilder.build()).get().build();
        return executeAndExtractData(request);
    }

    private JSONObject post(String path, JSONObject body) throws Exception {
        return post(path, Collections.emptyMap(), body);
    }

    private JSONObject post(String path, Map<String, String> query, JSONObject body) throws Exception {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(ServerConfigManager.getInstance().getHttpBaseUrl() + path).newBuilder();
        for (Map.Entry<String, String> entry : query.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .post(RequestBody.create(body.toString(), JSON_TYPE))
                .build();
        return executeAndExtractData(request);
    }

    private JSONObject executeAndExtractData(Request request) throws Exception {
        String serverAddress = ServerConfigManager.getInstance().getHttpBaseUrl();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("HTTP " + response.code() + " from server " + serverAddress);
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IllegalStateException("Empty response from server " + serverAddress);
            }
            String body = responseBody.string();
            JSONObject root = new JSONObject(body);
            int code = root.optInt("code", 500);
            String message = root.optString("message", "unknown error");
            if (code != 200) {
                throw new IllegalStateException(message + " (server: " + serverAddress + ")");
            }
            Object data = root.opt("data");
            if (data instanceof JSONObject jsonObject) {
                return jsonObject;
            }
            JSONObject wrapper = new JSONObject();
            if (data instanceof JSONArray jsonArray) {
                wrapper.put("items", jsonArray);
            }
            return wrapper;
        } catch (java.net.UnknownHostException e) {
            throw new Exception("Cannot connect to server " + serverAddress + ". Please check the IP address.", e);
        } catch (java.net.ConnectException e) {
            throw new Exception("Cannot connect to server " + serverAddress + ". Please check the IP address and network connection.", e);
        } catch (java.net.SocketTimeoutException e) {
            throw new Exception("Connection timeout to server " + serverAddress + ". Please check your network connection.", e);
        } catch (java.io.IOException e) {
            throw new Exception("Network error connecting to server " + serverAddress + ": " + e.getMessage(), e);
        }
    }

    private List<UserProfile> parseUserList(JSONArray arr) {
        List<UserProfile> users = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item != null) {
                users.add(parseUserProfile(item));
            }
        }
        return users;
    }

    private List<LeaderboardEntry> parseLeaderboard(JSONArray arr) {
        List<LeaderboardEntry> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item != null) {
                list.add(new LeaderboardEntry(
                        item.optLong("userId", 0L),
                        item.optString("nickname", ""),
                        item.optLong("value", 0L),
                        item.optInt("equippedSkinId", 0)
                ));
            }
        }
        return list;
    }

    @NonNull
    private UserProfile parseUserProfile(JSONObject data) {
        return new UserProfile(
                data.optLong("userId", 0L),
                data.optString("username", ""),
                data.optString("nickname", ""),
                data.optBoolean("online", false),
                data.optLong("highScore", 0L),
                data.optLong("coins", 0L),
                data.optInt("equippedSkinId", 0)
        );
    }
}
