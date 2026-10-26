package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.VerticalGridView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.ActivityHistoryBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.adapter.BaseDiffCallback;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.presenter.HistoryPresenter;
import com.fongmi.android.tv.utils.KeyUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

/**
 * Watch history. OK resumes. Delete is long-press, then OK, and does not sit on the resume path.
 */
public class HistoryActivity extends BaseActivity implements HistoryPresenter.OnClickListener {

    private ActivityHistoryBinding mBinding;
    private ArrayObjectAdapter mAdapter;
    private HistoryPresenter mPresenter;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, HistoryActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHistoryBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mPresenter = new HistoryPresenter(this);
        mAdapter = new ArrayObjectAdapter(mPresenter);
        VerticalGridView grid = mBinding.recycler;
        grid.setNumColumns(Product.getColumn());
        grid.setAdapter(new ItemBridgeAdapter(mAdapter));
        mBinding.progressLayout.setEmptyText(getString(R.string.atv_history_empty));
        getHistory();
    }

    private void getHistory() {
        List<History> items = History.get();
        mAdapter.setItems(items, new BaseDiffCallback<>());
        if (items.isEmpty()) mBinding.progressLayout.showEmpty();
        else {
            mBinding.progressLayout.showContent();
            mBinding.recycler.requestFocus();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() == RefreshEvent.Type.HISTORY) getHistory();
    }

    @Override
    public void onItemClick(History item) {
        VideoActivity.resume(this, item);
    }

    @Override
    public void onItemDelete(History item) {
        mAdapter.remove(item.delete());
        if (mAdapter.size() == 0) {
            mPresenter.setDelete(false);
            mBinding.progressLayout.showEmpty();
        }
    }

    @Override
    public boolean onLongClick() {
        mPresenter.setDelete(!mPresenter.isDelete());
        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event) && mAdapter.size() > 0 && !mPresenter.isDelete()) {
            onLongClick();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onBackInvoked() {
        if (mPresenter.isDelete()) {
            mPresenter.setDelete(false);
            mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
        } else super.onBackInvoked();
    }
}
