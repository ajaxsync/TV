package com.fongmi.android.tv.ui.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.splashscreen.SplashScreen;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.BaseGridView;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.SiteApi;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.api.node.NodeRuntime;
import com.fongmi.android.tv.bean.Cache;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.db.BackupManager;
import com.fongmi.android.tv.event.CastEvent;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.player.extractor.Source;
import com.fongmi.android.tv.playback.ExternalPlayback;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.service.DLNARendererService;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.ui.adapter.BaseDiffCallback;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.AtvSidebar;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.custom.ShelfRowPresenter;
import com.fongmi.android.tv.ui.presenter.CategoryMore;
import com.fongmi.android.tv.ui.presenter.ClassPresenter;
import com.fongmi.android.tv.ui.presenter.HeroFeature;
import com.fongmi.android.tv.ui.presenter.HeroPresenter;
import com.fongmi.android.tv.ui.presenter.HistoryPresenter;
import com.fongmi.android.tv.ui.presenter.MorePresenter;
import com.fongmi.android.tv.ui.presenter.ProgressPresenter;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Task;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

public class HomeActivity extends BaseActivity implements VodPresenter.OnClickListener, HistoryPresenter.OnClickListener, ClassPresenter.OnClickListener, MorePresenter.OnClickListener {

    private static final String STATE_ACTION_HANDLED = "action_handled";
    private static final long ID_CONTINUE = 1L;
    private static final long ID_RECOMMEND = 2L;
    private static final long ID_CATEGORIES = 3L;
    private static final int HERO_LIMIT = 6;
    private static final int RECOMMEND_LIMIT = 24;
    private static final int CATEGORY_LIMIT = 6;
    private static final int CATEGORY_ITEMS = 12;
    private static volatile int instanceCount;

    public static boolean isRunning() {
        return instanceCount > 0;
    }

    private ActivityHomeBinding mBinding;
    private ArrayObjectAdapter mHistoryAdapter;
    private ArrayObjectAdapter mAdapter;
    private HistoryPresenter mPresenter;
    private ClassPresenter mClassPresenter;
    private VodPresenter mVodPresenter;
    private MorePresenter mMorePresenter;
    private SiteViewModel mViewModel;
    private Intent pendingAction;
    private Future<?> shelfTask;
    private boolean actionReady;
    private boolean placedFocus;
    private boolean userNavigated;
    private int shelfEpoch;
    private Result mResult;
    private Clock mClock;

    private Site getHome() {
        return VodConfig.get().getHome();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        pendingAction = intent;
        consumeAction();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != ExternalPlayback.REQUEST_CODE) return;
        setResult(resultCode, data);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instanceCount++;
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        if (savedInstanceState == null || !savedInstanceState.getBoolean(STATE_ACTION_HANDLED)) pendingAction = getIntent();
        mResult = Result.empty();
        mClock = Clock.create(mBinding.clock);
        mBinding.progressLayout.showProgress();
        Updater.get().start(this, false);
        setRecyclerView();
        setViewModel();
        setAdapter();
        checkPermission();
    }

    @Override
    protected void initEvent() {
        mBinding.sidebar.setSection(AtvSidebar.Section.HOME);
        mBinding.addVod.setOnClickListener(view -> ConfigActivity.vod(this));
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (mPresenter != null && mPresenter.isDelete()) setHistoryDelete(false);
            }
        });
    }

    private void checkAction(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction()) || Intent.ACTION_VIEW.equals(intent.getAction())) {
            ExternalPlayback.open(this, intent, this::loadLive);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String keyword = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(keyword)) SearchActivity.start(this, keyword);
        }
    }

    private void consumeAction() {
        if (!actionReady || pendingAction == null) return;
        Intent intent = pendingAction;
        pendingAction = null;
        checkAction(intent);
    }

    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        mMorePresenter = new MorePresenter(this);
        selector.addPresenter(HeroFeature.class, new HeroPresenter(this));
        selector.addPresenter(String.class, new ProgressPresenter());
        selector.addPresenter(Class.class, mClassPresenter = new ClassPresenter(this));
        selector.addPresenter(ListRow.class, new ShelfRowPresenter(18, FocusHighlight.ZOOM_FACTOR_NONE), VodPresenter.class);
        selector.addPresenter(ListRow.class, new ShelfRowPresenter(18, FocusHighlight.ZOOM_FACTOR_NONE), ClassPresenter.class);
        selector.addPresenter(ListRow.class, new ShelfRowPresenter(18, FocusHighlight.ZOOM_FACTOR_NONE, HorizontalGridView.FOCUS_SCROLL_ALIGNED), HistoryPresenter.class);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(28));
        mBinding.recycler.setWindowAlignment(BaseGridView.WINDOW_ALIGN_BOTH_EDGE);
        mBinding.recycler.setWindowAlignmentOffsetPercent(BaseGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED);
        mBinding.recycler.setItemAlignmentOffsetPercent(BaseGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED);
        mBinding.recycler.setWindowAlignmentOffset(0);
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.getAction().observe(this, result -> WebActivity.handleAction(this, result));
        mViewModel.getResult().observe(this, this::onResult);
    }

    private void onResult(Result result) {
        mAdapter.remove("progress");
        mResult = result == null ? Result.empty() : result;
        setHero(featured(mResult.getList()));
        addRecommend(mResult);
        loadCategoryShelves(mResult);
        Cache.clear().put(mResult);
        focusMain();
    }

    private void setAdapter() {
        mHistoryAdapter = new ArrayObjectAdapter(mPresenter = new HistoryPresenter(this));
    }

    private void checkPermission() {
        PermissionUtil.requestLocalNetwork(this, this::onNetworkPermissionResult);
    }

    private void onNetworkPermissionResult(boolean allGranted) {
        PermissionUtil.requestNotify(this);
        if (allGranted) DLNARendererService.start(this);
        initConfig();
    }

    private void initConfig() {
        VodConfig.get().init().load(getCallback());
        LiveConfig.get().init().load();
        WallConfig.get().init();
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                onConfigReady();
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
                onConfigReady();
            }
        };
    }

    private void onConfigReady() {
        actionReady = true;
        showContent();
        updateEmpty();
        getHistory();
        consumeAction();
    }

    private void showContent() {
        mBinding.progressLayout.showContent();
    }

    private void updateEmpty() {
        boolean empty = TextUtils.isEmpty(VodConfig.getUrl());
        mBinding.emptyConfig.setVisibility(empty ? View.VISIBLE : View.GONE);
        mBinding.progressLayout.setVisibility(empty ? View.GONE : View.VISIBLE);
        mBinding.sidebar.setContentFocusId(empty ? R.id.addVod : R.id.recycler);
        if (empty && !userNavigated) {
            mBinding.addVod.post(() -> mBinding.addVod.requestFocus());
            placedFocus = true;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_ACTION_HANDLED, pendingAction == null);
        super.onSaveInstanceState(outState);
    }

    private void loadLive(String url) {
        if (isFinishing() || isDestroyed()) return;
        LiveConfig.load(Config.find(url, 1), new Callback() {
            @Override
            public void success() {
                if (getIntent().getBooleanExtra(ExternalPlayback.FORWARD_RESULT, false)) startActivityForResult(new Intent(HomeActivity.this, LiveActivity.class).putExtra("empty", LiveConfig.isEmpty()), ExternalPlayback.REQUEST_CODE);
                else LiveActivity.start(getActivity());
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
                if (getIntent().getBooleanExtra(ExternalPlayback.FORWARD_RESULT, false)) finish();
            }
        });
    }

    private void focusMain() {
        if (mBinding.emptyConfig.getVisibility() == View.VISIBLE) {
            if (!userNavigated) mBinding.addVod.post(() -> mBinding.addVod.requestFocus());
            placedFocus = true;
            return;
        }
        if (userNavigated) return;
        mBinding.recycler.post(this::focusDefault);
    }

    private void focusDefault() {
        if (userNavigated || mBinding.emptyConfig.getVisibility() == View.VISIBLE) return;
        View play = mBinding.recycler.findViewById(R.id.heroPlay);
        if (play != null) {
            play.requestFocus();
            placedFocus = true;
            return;
        }
        if (mAdapter.size() > 0) {
            mBinding.recycler.requestFocus();
            placedFocus = true;
        }
    }

    private void getVideo() {
        mResult = Result.empty();
        clearShelves();
        if (mAdapter.indexOf("progress") < 0) mAdapter.add("progress");
        mViewModel.homeContent();
    }

    private void clearShelves() {
        shelfEpoch++;
        if (shelfTask != null) shelfTask.cancel(true);
        for (int i = mAdapter.size() - 1; i >= 0; i--) {
            if (isContinue(mAdapter.get(i))) continue;
            mAdapter.removeItems(i, 1);
        }
    }

    private void setHero(List<Vod> items) {
        int at = indexOfHero();
        if (items.isEmpty()) {
            if (at >= 0) mAdapter.removeItems(at, 1);
            return;
        }
        HeroFeature feature = new HeroFeature(items);
        if (at < 0) mAdapter.add(0, feature);
        else mAdapter.replace(at, feature);
    }

    private List<Vod> featured(List<Vod> list) {
        List<Vod> playable = new ArrayList<>();
        for (Vod vod : list) {
            if (vod.isAction() || vod.isFolder() || vod.getName().isEmpty()) continue;
            playable.add(vod);
        }
        List<Vod> withPic = new ArrayList<>();
        for (Vod vod : playable) if (!vod.getPic().isEmpty()) withPic.add(vod);
        List<Vod> source = withPic.isEmpty() ? playable : withPic;
        return source.subList(0, Math.min(HERO_LIMIT, source.size()));
    }

    private void addRecommend(Result result) {
        int existing = indexOfHeader(ID_RECOMMEND);
        if (existing >= 0) mAdapter.removeItems(existing, 1);
        List<Vod> items = new ArrayList<>();
        for (Vod vod : result.getList()) {
            if (vod.getName().isEmpty() && vod.getPic().isEmpty()) continue;
            items.add(vod);
            if (items.size() == RECOMMEND_LIMIT) break;
        }
        if (items.isEmpty()) return;
        Style style = result.getStyle(getHome().getStyle());
        if (style.isList()) style = Style.rect();
        if (mVodPresenter == null || !mVodPresenter.matches(style)) mVodPresenter = new VodPresenter(this, style);
        mMorePresenter.size(style);
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(mVodPresenter);
        adapter.addAll(0, items);
        mAdapter.add(endOfRecommend(), new ListRow(new HeaderItem(ID_RECOMMEND, getString(R.string.home_recommend)), adapter));
    }

    private void loadCategoryShelves(Result result) {
        int ticket = ++shelfEpoch;
        if (shelfTask != null) shelfTask.cancel(true);
        String key = getHome().getKey();
        List<Class> types = new ArrayList<>();
        for (Class type : result.getTypes()) {
            if (type.isHome() || type.getTypeId().isEmpty() || type.getTypeName().isEmpty()) continue;
            types.add(type);
            if (types.size() == CATEGORY_LIMIT) break;
        }
        if (types.isEmpty() || key.isEmpty()) {
            if (!result.getTypes().isEmpty()) showCategoryChips(result);
            return;
        }
        shelfTask = Task.submit(() -> {
            for (int i = 0; i < types.size(); i++) {
                if (ticket != shelfEpoch || Thread.currentThread().isInterrupted()) return;
                Class type = types.get(i);
                List<Vod> ready = List.of();
                try {
                    Result page = SiteApi.categoryContent(key, type.getTypeId(), "1", type.getFilter(), new HashMap<>());
                    List<Vod> fetched = new ArrayList<>();
                    for (Vod vod : page.getList()) {
                        if (vod.getName().isEmpty()) continue;
                        fetched.add(vod);
                        if (fetched.size() == CATEGORY_ITEMS) break;
                    }
                    ready = fetched;
                } catch (Exception ignored) {
                }
                int order = i;
                int index = Math.max(0, result.getTypes().indexOf(type));
                List<Vod> items = ready;
                boolean last = i == types.size() - 1;
                App.post(() -> {
                    if (ticket != shelfEpoch || isFinishing()) return;
                    if (!items.isEmpty()) addCategoryShelf(type, order, index, items);
                    if (last) showCategoryChips(result);
                });
            }
        });
    }

    private void addCategoryShelf(Class type, int order, int index, List<Vod> items) {
        long id = 100L + order;
        if (indexOfHeader(id) >= 0) return;
        if (mVodPresenter == null) mVodPresenter = new VodPresenter(this, Style.rect());
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(shelfSelector());
        adapter.addAll(0, items);
        adapter.add(new CategoryMore(index, type.getTypeName()));
        mAdapter.add(categoryInsertAt(order), new ListRow(new HeaderItem(id, type.getTypeName()), adapter));
    }

    private PresenterSelector shelfSelector() {
        return new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object item) {
                return item instanceof CategoryMore ? mMorePresenter : mVodPresenter;
            }

            @NonNull
            @Override
            public Presenter[] getPresenters() {
                return new Presenter[]{mVodPresenter, mMorePresenter};
            }
        };
    }

    private void showCategoryChips(Result result) {
        if (result.getTypes().isEmpty() || indexOfHeader(ID_CATEGORIES) >= 0 || hasCategoryShelf()) return;
        ArrayObjectAdapter row = new ArrayObjectAdapter(mClassPresenter);
        row.addAll(0, result.getTypes());
        mAdapter.add(new ListRow(new HeaderItem(ID_CATEGORIES, getString(R.string.atv_categories)), row));
    }

    private void getHistory() {
        getHistory(false);
    }

    private void getHistory(boolean renew) {
        List<History> items = History.get();
        if (renew) {
            mHistoryAdapter = new ArrayObjectAdapter(mPresenter = new HistoryPresenter(this));
            int existing = indexOfHeader(ID_CONTINUE);
            if (existing >= 0) mAdapter.removeItems(existing, 1);
        }
        int header = indexOfHeader(ID_CONTINUE);
        if (items.isEmpty()) {
            if (header >= 0) mAdapter.removeItems(header, 1);
        } else {
            if (header < 0) {
                int at = indexOfHero() >= 0 ? indexOfHero() + 1 : 0;
                mAdapter.add(at, new ListRow(new HeaderItem(ID_CONTINUE, getString(R.string.atv_continue)), mHistoryAdapter));
            }
            mHistoryAdapter.setItems(items, new BaseDiffCallback<>());
        }
        focusMain();
    }

    private void removeContinueRow() {
        int header = indexOfHeader(ID_CONTINUE);
        if (header >= 0) mAdapter.removeItems(header, 1);
    }

    private void setHistoryDelete(boolean delete) {
        mPresenter.setDelete(delete);
        mHistoryAdapter.notifyArrayItemRangeChanged(0, mHistoryAdapter.size());
    }

    private void clearHistory() {
        removeContinueRow();
        History.clear(VodConfig.getCid());
        mPresenter.setDelete(false);
        mHistoryAdapter.clear();
    }

    private boolean isContinue(Object item) {
        return item instanceof ListRow row && row.getHeaderItem() != null && row.getHeaderItem().getId() == ID_CONTINUE;
    }

    private boolean hasCategoryShelf() {
        for (int i = 0; i < mAdapter.size(); i++) {
            Object item = mAdapter.get(i);
            if (item instanceof ListRow row && row.getHeaderItem() != null && row.getHeaderItem().getId() >= 100L) return true;
        }
        return false;
    }

    private int indexOfHero() {
        for (int i = 0; i < mAdapter.size(); i++) if (mAdapter.get(i) instanceof HeroFeature) return i;
        return -1;
    }

    private int indexOfHeader(long id) {
        for (int i = 0; i < mAdapter.size(); i++) {
            Object item = mAdapter.get(i);
            if (item instanceof ListRow row && row.getHeaderItem() != null && row.getHeaderItem().getId() == id) return i;
        }
        return -1;
    }

    private int endOfRecommend() {
        int recommend = indexOfHeader(ID_RECOMMEND);
        if (recommend >= 0) return recommend + 1;
        int at = 0;
        int hero = indexOfHero();
        if (hero >= 0) at = hero + 1;
        int history = indexOfHeader(ID_CONTINUE);
        if (history >= 0) at = Math.max(at, history + 1);
        int progress = mAdapter.indexOf("progress");
        if (progress >= 0) at = Math.max(at, progress + 1);
        return at;
    }

    private int categoryInsertAt(int order) {
        int at = endOfRecommend();
        int recommend = indexOfHeader(ID_RECOMMEND);
        if (recommend >= 0) at = recommend + 1;
        for (int i = 0; i < order; i++) {
            int found = indexOfHeader(100L + i);
            if (found >= 0) at = found + 1;
        }
        return at;
    }

    private boolean atTop() {
        if (mBinding.sidebar.hasFocus()) return false;
        if (mBinding.emptyConfig.getVisibility() == View.VISIBLE) return mBinding.addVod.hasFocus();
        return mBinding.recycler.getSelectedPosition() <= 0;
    }

    private boolean atRowStart() {
        View focus = getCurrentFocus();
        if (focus == null || focus.getId() == R.id.heroPlay || mBinding.sidebar.hasFocus()) return false;
        ViewParent parent = focus.getParent();
        if (!(parent instanceof ViewGroup group)) return false;
        int left = focus.getLeft();
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child != focus && child.isFocusable() && child.getLeft() + 8 < left) return false;
        }
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        switch (event.type()) {
            case VOD:
                placedFocus = false;
                userNavigated = false;
                updateEmpty();
                RefreshEvent.history();
                RefreshEvent.home();
                break;
            case COMMON:
                break;
            case BOOT:
                LiveActivity.start(this);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case HOME:
                getVideo();
                break;
            case HISTORY:
                getHistory();
                break;
            case SIZE:
                getVideo();
                getHistory(true);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        switch (event.type()) {
            case SEARCH:
                SearchActivity.start(this, event.text());
                break;
            case PUSH:
                VideoActivity.push(this, event.text());
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCastEvent(CastEvent event) {
        if (VodConfig.get().getConfig().equals(event.config())) VideoActivity.cast(this, event.history());
        else VodConfig.load(event.config(), getCallback(event));
    }

    private Callback getCallback(CastEvent event) {
        return new Callback() {
            @Override
            public void success() {
                onCastEvent(event);
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        };
    }

    @Override
    public void onMore(CategoryMore item) {
        if (mResult != null) VodActivity.start(this, mResult, item.index);
    }

    @Override
    public void onItemClick(Class item) {
        List<Class> types = mResult == null ? List.of() : mResult.getTypes();
        int index = Math.max(0, types.indexOf(item));
        if (mResult != null) VodActivity.start(this, mResult, index);
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.isAction()) mViewModel.action(getHome().getKey(), item.getAction());
        else if (getHome().isIndex()) CollectActivity.start(this, item.getName());
        else VideoActivity.open(this, getHome().getKey(), item.getId(), item.getName(), item.getPic());
    }

    @Override
    public boolean onLongClick(Vod item) {
        if (item.isAction()) return false;
        CollectActivity.start(this, item.getName());
        return true;
    }

    @Override
    public void onItemClick(History item) {
        VideoActivity.resume(this, item);
    }

    @Override
    public void onItemDelete(History item) {
        mHistoryAdapter.remove(item.delete());
        if (mHistoryAdapter.size() > 0) return;
        removeContinueRow();
        mPresenter.setDelete(false);
    }

    @Override
    public boolean onLongClick() {
        if (mPresenter.isDelete()) clearHistory();
        else setHistoryDelete(true);
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event)) {
            SettingActivity.start(this);
            return true;
        }
        if (KeyUtil.isActionDown(event)) {
            if (KeyUtil.isUpKey(event) && atTop()) {
                userNavigated = true;
                mBinding.sidebar.focusSection();
                return true;
            }
            if (KeyUtil.isLeftKey(event) && atRowStart()) {
                userNavigated = true;
                mBinding.sidebar.focusSection();
                return true;
            }
            userNavigated = true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mClock.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mClock.stop();
    }

    @Override
    protected void onBackInvoked() {
        if (mBinding.sidebar.hasFocus()) {
            mBinding.sidebar.focusContent();
        } else if (mBinding.progressLayout.isProgress()) {
            showContent();
        } else if (mPresenter != null && mPresenter.isDelete()) {
            setHistoryDelete(false);
        } else if (mBinding.recycler.getSelectedPosition() != 0) {
            mBinding.recycler.scrollToPosition(0);
        } else {
            if (PlaybackService.isRunning()) Util.moveToBackground(this);
            else super.onBackInvoked();
        }
    }

    @Override
    protected void onDestroy() {
        shelfEpoch++;
        if (shelfTask != null) shelfTask.cancel(true);
        if (--instanceCount == 0) {
            BackupManager.backup();
            releaseResources();
        }
        super.onDestroy();
    }

    public static void releaseResources() {
        if (isRunning() || PlaybackService.isRunning()) return;
        DLNARendererService.stop(App.get());
        LiveConfig.get().clear();
        VodConfig.get().clear();
        OkHttp.get().clear();
        Source.get().exit();
        Server.get().stop();
        NodeRuntime.get().releaseIfUnused();
    }
}
