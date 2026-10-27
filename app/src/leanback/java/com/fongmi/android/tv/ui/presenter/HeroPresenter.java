package com.fongmi.android.tv.ui.presenter;

import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ViewAtvHeroBinding;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.List;

public class HeroPresenter extends Presenter {

    private static final long ADVANCE_MS = 7000L;

    private final VodPresenter.OnClickListener listener;

    public HeroPresenter(VodPresenter.OnClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
        return new Holder(ViewAtvHeroBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Presenter.ViewHolder viewHolder, Object object) {
        ((Holder) viewHolder).bind((HeroFeature) object);
    }

    @Override
    public void onUnbindViewHolder(@NonNull Presenter.ViewHolder viewHolder) {
        ((Holder) viewHolder).unbind();
    }

    private final class Holder extends Presenter.ViewHolder {

        private final ViewAtvHeroBinding binding;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private List<Vod> items = List.of();
        private int index;
        private final Runnable advance = () -> {
            if (!binding.heroPlay.hasFocus() && items.size() > 1) show((index + 1) % items.size());
            schedule();
        };

        Holder(ViewAtvHeroBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(HeroFeature feature) {
            items = feature.items;
            index = 0;
            show(0);
            binding.heroPlay.setOnClickListener(v -> {
                if (!items.isEmpty()) listener.onItemClick(items.get(index));
            });
            binding.heroPlay.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN || items.size() < 2) return false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    show((index + 1) % items.size());
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && index > 0) {
                    show(index - 1);
                    return true;
                }
                return false;
            });
            schedule();
        }

        void unbind() {
            handler.removeCallbacks(advance);
            Glide.with(binding.image).clear(binding.image);
            items = List.of();
        }

        private void show(int next) {
            if (items.isEmpty()) return;
            index = next;
            Vod item = items.get(index);
            binding.title.setText(item.getName());
            String meta = meta(item);
            binding.meta.setText(meta);
            binding.meta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);
            String desc = blurb(item.getContent());
            binding.desc.setText(desc);
            binding.desc.setVisibility(desc.isEmpty() ? View.GONE : View.VISIBLE);
            ImgUtil.load(item.getName(), item.getPic(), binding.image);
            dots();
        }

        private void dots() {
            binding.dots.removeAllViews();
            binding.dots.setVisibility(items.size() > 1 ? View.VISIBLE : View.GONE);
            float density = view.getResources().getDisplayMetrics().density;
            int size = Math.round(8 * density);
            int gap = Math.round(6 * density);
            for (int i = 0; i < items.size(); i++) {
                View dot = new View(view.getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                if (i > 0) params.setMarginStart(gap);
                dot.setLayoutParams(params);
                dot.setBackgroundResource(R.drawable.atv_hero_dot);
                dot.setSelected(i == index);
                binding.dots.addView(dot);
            }
        }

        private void schedule() {
            handler.removeCallbacks(advance);
            if (items.size() > 1) handler.postDelayed(advance, ADVANCE_MS);
        }
    }

    private static String meta(Vod item) {
        List<String> parts = new ArrayList<>();
        if (!item.getYear().isEmpty()) parts.add(item.getYear());
        if (!item.getArea().isEmpty()) parts.add(item.getArea());
        if (!item.getRemarks().isEmpty()) parts.add(item.getRemarks());
        if (!item.getTypeName().isEmpty()) parts.add(item.getTypeName());
        return String.join("  ·  ", parts);
    }

    private static String blurb(String content) {
        if (content == null) return "";
        String text = content.replaceAll("\\s+", " ").trim();
        if (text.length() <= 110) return text;
        return text.substring(0, 110) + "…";
    }
}
