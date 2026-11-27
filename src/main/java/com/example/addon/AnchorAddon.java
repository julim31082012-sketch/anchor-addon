package com.example.addon;

import com.example.addon.modules.AnchorLoop;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class AnchorAddon extends MeteorAddon {
    @Override
    public void onInitialize() {
        Modules.get().add(new AnchorLoop());
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }
}
