package com.fongmi.android.tv.ui.atv;

import com.fongmi.android.tv.setting.Setting;

/**
 * Interaction rules for the shelf home. Visual tokens live in {@code atv_tokens}.
 * <p>
 * Left sidebar, always in this order: Search, Home, History, Live, Settings.
 * Up from the top of Home focuses the sidebar. Down from the hero enters the shelves.
 * Back leaves the sidebar for the page, then the previous page, then home.
 * Default focus is the main action: Add config, Hero play, or Continue watching.
 * Site switching, lines, DoH, and modules stay off the first path.
 */
public final class AtvInteraction {

    private AtvInteraction() {
    }

    /** Poster OK plays immediately unless Advanced asks for the detail page first. */
    public static boolean directPlay() {
        return !Setting.isDetailFirst();
    }
}
