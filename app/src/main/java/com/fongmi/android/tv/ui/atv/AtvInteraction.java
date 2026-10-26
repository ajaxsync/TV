package com.fongmi.android.tv.ui.atv;

import com.fongmi.android.tv.setting.Setting;

/**
 * Interaction rules for the shelf home. Visual tokens live in {@code atv_tokens}.
 * <p>
 * Top bar, always in this order: Search, History, Live, Settings.
 * Back closes a layer, then the previous page, then home.
 * Default focus is the main action: Add config, Continue watching, or Play.
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
