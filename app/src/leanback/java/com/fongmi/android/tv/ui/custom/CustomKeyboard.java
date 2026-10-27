package com.fongmi.android.tv.ui.custom;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivitySearchBinding;
import com.fongmi.android.tv.ui.adapter.KeyboardAdapter;
import com.fongmi.android.tv.utils.ResUtil;

public class CustomKeyboard implements KeyboardAdapter.OnClickListener {

    private final ActivitySearchBinding binding;
    private final Callback callback;
    private KeyboardAdapter adapter;
    private String lastGroup = "";

    public static void init(Callback callback, ActivitySearchBinding binding) {
        new CustomKeyboard(callback, binding).initView();
    }

    public CustomKeyboard(Callback callback, ActivitySearchBinding binding) {
        this.callback = callback;
        this.binding = binding;
    }

    private void initView() {
        binding.keyboard.setItemAnimator(null);
        binding.keyboard.setHasFixedSize(false);
        GridLayoutManager layout = new GridLayoutManager(binding.keyboard.getContext(), KeyboardAdapter.SPAN);
        layout.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter == null ? KeyboardAdapter.SPAN : adapter.spanSize(position);
            }
        });
        binding.keyboard.setLayoutManager(layout);
        binding.keyboard.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                if (!(view.getLayoutParams() instanceof GridLayoutManager.LayoutParams params)) return;
                int spacing = ResUtil.dp2px(8);
                int total = KeyboardAdapter.SPAN;
                int index = params.getSpanIndex();
                int size = params.getSpanSize();
                outRect.left = index * spacing / total;
                outRect.right = spacing - (index + size) * spacing / total;
                if (parent.getChildAdapterPosition(view) >= total / 3) outRect.top = spacing;
            }
        });
        binding.keyboard.setAdapter(adapter = new KeyboardAdapter(this));
    }

    @Override
    public void onTextClick(String text) {
        if (adapter.isSwitch(text)) {
            lastGroup = "";
            adapter.toggle();
            return;
        }
        if (text.length() > 1) {
            onT9(text.toLowerCase());
            return;
        }
        lastGroup = "";
        insert(text);
    }

    private void onT9(String group) {
        StringBuilder sb = new StringBuilder(binding.keyword.getText().toString());
        int cursor = Math.max(binding.keyword.getSelectionStart(), 0);
        if (group.equals(lastGroup) && cursor > 0) {
            int index = group.indexOf(sb.charAt(cursor - 1));
            if (index >= 0) {
                sb.setCharAt(cursor - 1, group.charAt((index + 1) % group.length()));
                binding.keyword.setText(sb.toString());
                binding.keyword.setSelection(cursor);
                return;
            }
        }
        if (binding.keyword.length() > 19) return;
        lastGroup = group;
        sb.insert(cursor, group.charAt(0));
        binding.keyword.setText(sb.toString());
        binding.keyword.setSelection(cursor + 1);
    }

    private void insert(String text) {
        StringBuilder sb = new StringBuilder(binding.keyword.getText().toString());
        int cursor = Math.max(binding.keyword.getSelectionStart(), 0);
        if (binding.keyword.length() > 19) return;
        sb.insert(cursor, text);
        binding.keyword.setText(sb.toString());
        binding.keyword.setSelection(cursor + 1);
    }

    @Override
    public void onIconClick(int resId) {
        StringBuilder sb = new StringBuilder(binding.keyword.getText().toString());
        int cursor = binding.keyword.getSelectionStart();
        if (resId == R.drawable.ic_setting_home) callback.showDialog();
        else if (resId == R.drawable.ic_keyboard_remote) callback.onRemote();
        else if (resId == R.drawable.ic_keyboard_search) callback.onSearch();
        else if (resId == R.drawable.ic_keyboard_left) onMoveLeft(cursor);
        else if (resId == R.drawable.ic_keyboard_right) onMoveRight(cursor);
        else if (resId == R.drawable.ic_keyboard_back) onBackspace(sb, cursor);
    }

    private void onMoveLeft(int cursor) {
        binding.keyword.setSelection(--cursor < 0 ? 0 : cursor);
    }

    private void onMoveRight(int cursor) {
        binding.keyword.setSelection(++cursor > binding.keyword.length() ? binding.keyword.length() : cursor);
    }

    private void onBackspace(StringBuilder sb, int cursor) {
        lastGroup = "";
        if (cursor <= 0) return;
        sb.deleteCharAt(cursor - 1);
        binding.keyword.setText(sb.toString());
        binding.keyword.setSelection(cursor - 1);
    }

    @Override
    public boolean onLongClick(int resId) {
        if (resId != R.drawable.ic_keyboard_back) return false;
        binding.keyword.setText("");
        return true;
    }

    public interface Callback {

        void showDialog();

        void onRemote();

        void onSearch();
    }
}
