package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.AdapterKeyboardIconBinding;
import com.fongmi.android.tv.databinding.AdapterKeyboardTextBinding;
import com.fongmi.android.tv.setting.Setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KeyboardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int MODE_26 = 0;
    public static final int MODE_9 = 1;
    public static final int SPAN = 21;

    private static final String SWITCH_TO_9 = "九键";
    private static final String SWITCH_TO_26 = "全键";
    private static final List<String> GROUPS = Arrays.asList("ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ");
    private static final List<String> LETTERS = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    private static final List<String> DIGITS = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "0");

    private final OnClickListener listener;
    private final List<Object> mItems = new ArrayList<>();
    private int mode;

    public KeyboardAdapter(OnClickListener listener) {
        this.listener = listener;
        this.mode = Setting.getKeyboardMode() == MODE_9 ? MODE_9 : MODE_26;
        rebuild();
    }

    public interface OnClickListener {

        void onTextClick(String text);

        void onIconClick(int resId);

        boolean onLongClick(int resId);
    }

    public boolean isSwitch(String text) {
        return SWITCH_TO_9.equals(text) || SWITCH_TO_26.equals(text);
    }

    public int spanSize(int position) {
        Object item = mItems.get(position);
        if (item instanceof Integer || isSwitch(String.valueOf(item))) return 3;
        return mode == MODE_9 ? 7 : 3;
    }

    public void toggle() {
        mode = mode == MODE_9 ? MODE_26 : MODE_9;
        Setting.putKeyboardMode(mode);
        int count = getItemCount();
        rebuild();
        notifyItemRangeRemoved(0, count);
        notifyItemRangeInserted(0, getItemCount());
    }

    private void rebuild() {
        mItems.clear();
        mItems.add(R.drawable.ic_keyboard_remote);
        mItems.add(R.drawable.ic_keyboard_left);
        mItems.add(R.drawable.ic_keyboard_right);
        mItems.add(R.drawable.ic_keyboard_back);
        mItems.add(R.drawable.ic_keyboard_search);
        mItems.add(mode == MODE_26 ? SWITCH_TO_9 : SWITCH_TO_26);
        mItems.add(R.drawable.ic_setting_home);
        if (mode == MODE_9) {
            mItems.addAll(GROUPS);
            mItems.addAll(DIGITS);
        } else {
            mItems.addAll(LETTERS);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position) instanceof String ? 0 : 1;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) return new TextHolder(AdapterKeyboardTextBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        else return new IconHolder(AdapterKeyboardIconBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case 0:
                TextHolder text = (TextHolder) holder;
                String value = mItems.get(position).toString();
                text.binding.text.setText(value);
                text.binding.text.setTextSize(value.length() > 1 ? 14 : 16);
                break;
            case 1:
                IconHolder icon = (IconHolder) holder;
                icon.binding.icon.setImageResource((int) mItems.get(position));
                break;
        }
    }

    class TextHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AdapterKeyboardTextBinding binding;

        TextHolder(@NonNull AdapterKeyboardTextBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onTextClick(mItems.get(getLayoutPosition()).toString());
        }
    }

    class IconHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private final AdapterKeyboardIconBinding binding;

        IconHolder(@NonNull AdapterKeyboardIconBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onIconClick((int) mItems.get(getLayoutPosition()));
        }

        @Override
        public boolean onLongClick(View view) {
            return listener.onLongClick((int) mItems.get(getLayoutPosition()));
        }
    }
}
