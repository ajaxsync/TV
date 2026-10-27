package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.databinding.AdapterEpisodeSideBinding;
import com.fongmi.android.tv.databinding.DialogEpisodeSideBinding;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.List;

public class EpisodeSideDialog extends BaseSideSheetDialog {

    private DialogEpisodeSideBinding binding;

    public static void show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.isStateSaved()) return;
        for (Fragment fragment : manager.getFragments()) {
            if (fragment instanceof EpisodeSideDialog || fragment instanceof SpeedSettingDialog.SideSheet || fragment instanceof SpeedSettingDialog.BottomSheet) return;
        }
        new EpisodeSideDialog().show(manager, null);
    }

    @Override
    protected int getWidth() {
        return Math.min(ResUtil.dp2px(440), ResUtil.getScreenWidth() / 3 + ResUtil.dp2px(20));
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogEpisodeSideBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        if (!(getActivity() instanceof VideoActivity activity)) return;
        List<Episode> items = activity.episodes();
        int selected = 0;
        for (int i = 0; i < items.size(); i++) if (items.get(i).isSelected()) selected = i;
        binding.recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recycler.setAdapter(new Adapter(items, item -> {
            activity.onItemClick(item);
            dismiss();
        }));
        int index = selected;
        binding.recycler.scrollToPosition(index);
        binding.recycler.post(() -> {
            RecyclerView.ViewHolder holder = binding.recycler.findViewHolderForAdapterPosition(index);
            if (holder != null) holder.itemView.requestFocus();
        });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    private static final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

        private final List<Episode> items;
        private final OnPick listener;

        private Adapter(List<Episode> items, OnPick listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(AdapterEpisodeSideBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Episode item = items.get(position);
            holder.binding.text.setText(item.getDesc().concat(item.getName()));
            holder.binding.text.setSelected(item.isSelected());
            holder.binding.getRoot().setOnClickListener(v -> listener.onPick(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private static final class Holder extends RecyclerView.ViewHolder {

            private final AdapterEpisodeSideBinding binding;

            private Holder(@NonNull AdapterEpisodeSideBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private interface OnPick {
        void onPick(Episode item);
    }
}
