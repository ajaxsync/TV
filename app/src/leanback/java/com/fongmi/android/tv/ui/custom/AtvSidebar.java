package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ViewAtvSidebarBinding;
import com.fongmi.android.tv.ui.activity.HistoryActivity;
import com.fongmi.android.tv.ui.activity.HomeActivity;
import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.ui.activity.SearchActivity;
import com.fongmi.android.tv.ui.activity.SettingActivity;

public class AtvSidebar extends LinearLayout {

    public enum Section { SEARCH, HOME, HISTORY, LIVE, SETTING }

    private ViewAtvSidebarBinding binding;
    private Section section = Section.HOME;
    private int contentFocusId = View.NO_ID;

    public AtvSidebar(Context context) {
        this(context, null);
    }

    public AtvSidebar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AtvSidebar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        binding = ViewAtvSidebarBinding.inflate(LayoutInflater.from(context), this);
        wire(binding.navSearch, Section.SEARCH);
        wire(binding.navHome, Section.HOME);
        wire(binding.navHistory, Section.HISTORY);
        wire(binding.navLive, Section.LIVE);
        wire(binding.navSetting, Section.SETTING);
        setSection(Section.HOME);
    }

    public void setSection(Section section) {
        this.section = section;
        binding.navSearch.setSelected(section == Section.SEARCH);
        binding.navHome.setSelected(section == Section.HOME);
        binding.navHistory.setSelected(section == Section.HISTORY);
        binding.navLive.setSelected(section == Section.LIVE);
        binding.navSetting.setSelected(section == Section.SETTING);
    }

    public void setContentFocusId(int contentFocusId) {
        this.contentFocusId = contentFocusId;
        binding.navSearch.setNextFocusRightId(contentFocusId);
        binding.navHome.setNextFocusRightId(contentFocusId);
        binding.navHistory.setNextFocusRightId(contentFocusId);
        binding.navLive.setNextFocusRightId(contentFocusId);
        binding.navSetting.setNextFocusRightId(contentFocusId);
    }

    public void focusSection() {
        item(section).requestFocus();
    }

    public void focusContent() {
        View root = getRootView();
        View play = root.findViewById(R.id.heroPlay);
        if (play != null && play.isShown()) {
            play.requestFocus();
            return;
        }
        if (contentFocusId == View.NO_ID) return;
        View target = root.findViewById(contentFocusId);
        if (target != null) target.requestFocus();
    }

    private View item(Section section) {
        return switch (section) {
            case SEARCH -> binding.navSearch;
            case HOME -> binding.navHome;
            case HISTORY -> binding.navHistory;
            case LIVE -> binding.navLive;
            case SETTING -> binding.navSetting;
        };
    }

    private void wire(View view, Section target) {
        view.setOnClickListener(v -> open(target));
        view.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                focusContent();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && v.getId() == R.id.navSetting) {
                focusContent();
                return true;
            }
            return false;
        });
    }

    private void open(Section target) {
        if (target == section) {
            focusContent();
            return;
        }
        if (!(getContext() instanceof Activity activity)) return;
        switch (target) {
            case HOME -> {
                if (activity instanceof HomeActivity) focusContent();
                else {
                    activity.startActivity(new Intent(activity, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    activity.finish();
                }
            }
            case SEARCH -> SearchActivity.start(activity);
            case HISTORY -> HistoryActivity.start(activity);
            case LIVE -> LiveActivity.start(activity);
            case SETTING -> SettingActivity.start(activity);
        }
    }
}
