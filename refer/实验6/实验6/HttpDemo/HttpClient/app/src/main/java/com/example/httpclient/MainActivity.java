package com.example.httpclient;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private TextView tvUserInfo;
    // OkHttp客户端
    private OkHttpClient okHttpClient = new OkHttpClient();
    // 使用10.0.2.2是Android模拟器专用的特殊IP，代表电脑本身的地址，不要用localhost
    private static final String SERVER_IP = "10.0.2.2";
    private static final String SERVER_PORT = "8080";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvUserInfo = findViewById(R.id.tv_user_info);
        // 点击屏幕触发请求（也可直接在onCreate中调用）
        findViewById(R.id.layout_root).setOnClickListener(v -> requestUserInfo("1001"));
    }

    //调用服务器接口获取用户信息
    private void requestUserInfo(String userId) {
        // 子线程执行网络请求（Android禁止主线程发请求）
        new Thread(() -> {
            try {
                // 拼接请求地址（带用户ID参数）
                String url = String.format("http://%s:%s/api/user/info?userId=%s",
                        SERVER_IP, SERVER_PORT, userId);
                // 构建请求
                Request request = new Request.Builder()
                        .url(url)
                        .get() // GET请求
                        .build();
                // 执行请求
                okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            String jsonStr = response.body().string();
                            Gson gson = new Gson();
                            User user = gson.fromJson(jsonStr, User.class);
                            // 切回主线程更新UI
                            runOnUiThread(() -> tvUserInfo.setText(user.toString()));
                        }
                    }
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> tvUserInfo.setText("网络错误"));
                    }
                });
            } catch (Exception e) {
                // 捕获异常（网络错误、解析错误等）
                runOnUiThread(() -> tvUserInfo.setText("请求失败"));
                e.printStackTrace();
            }
        }).start();
    }
}