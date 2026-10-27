package com.fongmi.android.tv.ui.custom;

import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.RowPresenter;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.ui.presenter.QuietHeaderPresenter;
import com.fongmi.android.tv.utils.ResUtil;

public class ShelfRowPresenter extends CustomRowPresenter {

    public ShelfRowPresenter(int spacing, int focusZoomFactor) {
        this(spacing, focusZoomFactor, HorizontalGridView.FOCUS_SCROLL_ITEM);
    }

    public ShelfRowPresenter(int spacing, int focusZoomFactor, int strategy) {
        super(spacing, focusZoomFactor, strategy);
        setHeaderPresenter(new QuietHeaderPresenter());
    }

    @Override
    protected void initializeRowViewHolder(RowPresenter.ViewHolder holder) {
        super.initializeRowViewHolder(holder);
        ViewHolder vh = (ViewHolder) holder;
        HorizontalGridView grid = vh.getGridView();
        int start = grid.getResources().getDimensionPixelSize(R.dimen.atv_shelf_padding_start);
        int end = grid.getResources().getDimensionPixelSize(R.dimen.atv_page_padding);
        grid.setPadding(start, ResUtil.dp2px(4), end, ResUtil.dp2px(18));
        grid.setClipToPadding(false);
    }
}
