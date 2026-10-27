package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.databinding.AdapterAtvMoreBinding;

public class MorePresenter extends Presenter {

    public interface OnClickListener {
        void onMore(CategoryMore item);
    }

    private final OnClickListener listener;
    private int[] size = Product.getSpec(Style.rect());

    public MorePresenter(OnClickListener listener) {
        this.listener = listener;
    }

    public void size(Style style) {
        this.size = Product.getSpec(style);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
        AdapterAtvMoreBinding binding = AdapterAtvMoreBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        binding.getRoot().getLayoutParams().width = size[0];
        binding.getRoot().getLayoutParams().height = size[1];
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Presenter.ViewHolder viewHolder, Object object) {
        CategoryMore item = (CategoryMore) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.name.setText(holder.view.getContext().getString(com.fongmi.android.tv.R.string.atv_more));
        holder.view.setOnClickListener(v -> listener.onMore(item));
    }

    @Override
    public void onUnbindViewHolder(@NonNull Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterAtvMoreBinding binding;

        public ViewHolder(AdapterAtvMoreBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
