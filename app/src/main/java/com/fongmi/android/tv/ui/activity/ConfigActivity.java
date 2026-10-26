package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.ActivityConfigBinding;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.ConfigListener;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.ui.dialog.HistoryDialog;
import com.fongmi.android.tv.ui.dialog.LocalConnectDialog;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.UrlUtil;

/**
 * One-screen config load. Confirm returns to the caller; address history stays secondary.
 */
public class ConfigActivity extends BaseActivity implements ConfigListener {

    private ActivityConfigBinding mBinding;
    private boolean append = true;
    private String original = "";
    private int type;

    public static void vod(Activity activity) {
        start(activity, 0);
    }

    public static void live(Activity activity) {
        start(activity, 1);
    }

    public static void start(Activity activity, int type) {
        Intent intent = new Intent(activity, ConfigActivity.class);
        intent.putExtra("type", type);
        activity.startActivity(intent);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityConfigBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        type = getIntent().getIntExtra("type", 0);
        original = type == 1 ? LiveConfig.getUrl() : VodConfig.getUrl();
        mBinding.title.setText(type == 1 ? R.string.atv_config_live_title : R.string.atv_config_vod_title);
        mBinding.text.setText(original);
        mBinding.text.setSelection(TextUtils.isEmpty(original) ? 0 : original.length());
        mBinding.text.requestFocus();
    }

    @Override
    protected void initEvent() {
        mBinding.positive.setOnClickListener(view -> onPositive());
        mBinding.choose.setOnClickListener(view -> FileChooser.from(launcher).show());
        mBinding.phone.setOnClickListener(view -> LocalConnectDialog.show(this, "settings"));
        mBinding.history.setOnClickListener(view -> {
            HistoryDialog dialog = HistoryDialog.create();
            if (type == 1) dialog.live();
            else dialog.vod();
            dialog.show(this);
        });
        mBinding.text.addTextChangedListener(new CustomTextListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                detect(s.toString());
            }
        });
        mBinding.text.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) onPositive();
            return true;
        });
    }

    private void detect(String text) {
        if (append && "h".equalsIgnoreCase(text)) {
            append = false;
            mBinding.text.append("ttp://");
        } else if (append && "f".equalsIgnoreCase(text)) {
            append = false;
            mBinding.text.append("ile://");
        } else if (append && "a".equalsIgnoreCase(text)) {
            append = false;
            mBinding.text.append("ssets://");
        } else if (text.length() > 1) {
            append = false;
        } else if (text.isEmpty()) {
            append = true;
        }
    }

    private void onPositive() {
        String text = mBinding.text.getText().toString().trim();
        if (text.isEmpty()) Config.delete(original, type);
        setConfig(Config.find(text, type));
        setConfig(Config.find(text, type));
    }

    @Override
    public void setConfig(Config config) {
        if (config.getUrl() != null && config.getUrl().startsWith("file")) PermissionUtil.requestFile(this, granted -> {
            if (granted) load(config);
        });
        else load(config);
    }

    private void load(Config config) {
        Callback callback = new Callback() {
            @Override
            public void start() {
                Notify.progress(getActivity());
            }

            @Override
            public void success() {
                Notify.dismiss();
                finish();
            }

            @Override
            public void error(String msg) {
                Notify.dismiss();
                Notify.show(msg);
            }
        };
        if (config.getType() == 1) LiveConfig.load(config, callback);
        else VodConfig.load(config, callback);
    }

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> FileChooser.getFileUri(result, this::setConfigUri));

    private void setConfigUri(Uri uri) {
        setConfig(Config.find(UrlUtil.toLocalUrl(uri), type));
    }
}
