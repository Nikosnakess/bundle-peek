package com.bundlepeek;

import net.fabricmc.api.ClientModInitializer;

public final class BundlePeekClient implements ClientModInitializer {
    public static final String MOD_ID = "bundle_peek";

    @Override
    public void onInitializeClient() {
        BundlePanel.register();
    }
}
