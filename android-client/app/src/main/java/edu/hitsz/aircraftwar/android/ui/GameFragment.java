package edu.hitsz.aircraftwar.android.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import edu.hitsz.aircraftwar.android.MainActivity;
import edu.hitsz.aircraftwar.android.game.AircraftWarSurfaceView;
import edu.hitsz.game.core.mode.Difficulty;

public class GameFragment extends Fragment {
    private static final String ARG_DIFFICULTY = "difficulty";
    private static final String ARG_PLAYER = "player";

    private AircraftWarSurfaceView surfaceView;

    public static GameFragment newInstance(Difficulty difficulty, String playerName) {
        Bundle args = new Bundle();
        args.putString(ARG_DIFFICULTY, difficulty.name());
        args.putString(ARG_PLAYER, playerName);
        GameFragment fragment = new GameFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();
        Difficulty difficulty = Difficulty.valueOf(args.getString(ARG_DIFFICULTY, Difficulty.NORMAL.name()));
        String playerName = args.getString(ARG_PLAYER, "Player");

        FrameLayout root = new FrameLayout(requireContext());
        surfaceView = new AircraftWarSurfaceView(requireContext(), difficulty);
        root.addView(surfaceView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        Button backButton = new Button(requireContext());
        backButton.setText("返回");
        backButton.setOnClickListener(view -> ((MainActivity) requireActivity()).showMainMenu());
        FrameLayout.LayoutParams backParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        backParams.gravity = Gravity.TOP | Gravity.START;
        int margin = UiUtils.dp(requireContext(), 16);
        backParams.setMargins(margin, margin, margin, margin);
        root.addView(backButton, backParams);

        TextView badge = UiUtils.createBody(requireContext(), playerName + " / " + difficulty.name());
        badge.setBackgroundColor(0x66333333);
        badge.setTextColor(0xFFFFFFFF);
        badge.setPadding(margin, UiUtils.dp(requireContext(), 8), margin, UiUtils.dp(requireContext(), 8));
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        badgeParams.gravity = Gravity.TOP | Gravity.END;
        badgeParams.setMargins(margin, margin, margin, margin);
        root.addView(badge, badgeParams);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (surfaceView != null) {
            surfaceView.resumeGame();
        }
    }

    @Override
    public void onPause() {
        if (surfaceView != null) {
            surfaceView.pauseGame();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (surfaceView != null) {
            surfaceView.release();
            surfaceView = null;
        }
        super.onDestroyView();
    }
}
