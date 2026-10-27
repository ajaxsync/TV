package com.fongmi.android.tv.ui.presenter;

import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowHeaderPresenter;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;

public class QuietHeaderPresenter extends RowHeaderPresenter {

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        super.onBindViewHolder(viewHolder, item);
        View root = viewHolder.view;
        int start = root.getResources().getDimensionPixelSize(R.dimen.atv_shelf_padding_start);
        int end = root.getResources().getDimensionPixelSize(R.dimen.atv_page_padding);
        root.setPadding(start, ResUtil.dp2px(12), end, ResUtil.dp2px(2));
        TextView title = title(root);
        if (title == null) return;
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTextColor(ContextCompat.getColor(title.getContext(), R.color.atv_text_secondary));
    }

    @Override
    protected void onSelectLevelChanged(ViewHolder holder) {
        holder.view.setAlpha(0.72f + 0.28f * holder.getSelectLevel());
    }

    private static TextView title(View root) {
        if (root instanceof TextView text) return text;
        View found = root.findViewById(androidx.leanback.R.id.row_header);
        return found instanceof TextView text ? text : null;
    }
}
