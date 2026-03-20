package edu.hitsz.aircraftwar.android.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import edu.hitsz.aircraftwar.android.MainActivity;

public class FeatureFragment extends Fragment {
    private static final String ARG_TITLE = "title";
    private static final String ARG_BODY = "body";

    public static FeatureFragment newInstance(String title, String body) {
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_BODY, body);
        FeatureFragment fragment = new FeatureFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();

        LinearLayout root = UiUtils.createScreenColumn(requireContext());
        root.setGravity(android.view.Gravity.CENTER_VERTICAL);

        root.addView(UiUtils.createTitle(requireContext(), args.getString(ARG_TITLE, "功能页")));
        root.addView(UiUtils.createBody(requireContext(), args.getString(ARG_BODY, "")));

        var backButton = UiUtils.createActionButton(requireContext(), "返回主菜单");
        backButton.setOnClickListener(view -> ((MainActivity) requireActivity()).showMainMenu());
        root.addView(backButton);
        return root;
    }
}
