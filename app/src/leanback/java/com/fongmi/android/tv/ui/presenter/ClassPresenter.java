package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Class;

public class ClassPresenter extends Presenter {

    private final OnClickListener listener;

    public ClassPresenter(OnClickListener listener) {
        this.listener = listener;
    }

    public interface OnClickListener {
        void onItemClick(Class item);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
        TextView view = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_type, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Presenter.ViewHolder viewHolder, Object object) {
        Class item = (Class) object;
        TextView view = (TextView) viewHolder.view;
        view.setText(item.getTypeName());
        view.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public void onUnbindViewHolder(@NonNull Presenter.ViewHolder viewHolder) {
    }
}
