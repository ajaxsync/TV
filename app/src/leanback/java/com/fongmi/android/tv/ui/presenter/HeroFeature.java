package com.fongmi.android.tv.ui.presenter;

import com.fongmi.android.tv.bean.Vod;

import java.util.List;

public final class HeroFeature {

    public final List<Vod> items;

    public HeroFeature(List<Vod> items) {
        this.items = List.copyOf(items);
    }
}
