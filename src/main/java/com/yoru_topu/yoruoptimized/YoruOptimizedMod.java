package com.yoru_topu.yoruoptimized;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Yoru Optimized:
 *  - Sets FPS cap to 150 on startup
 *  - Auto-installs performance mods via Modrinth (Fabric, 1.20.1)
 * Mods: sodium, lithium, starlight, indium, entityculling, memoryleakfix
 */
public class YoruOptimizedMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 1) Force FPS cap = 150 every launch
        try {
            MinecraftClient.getInstance().options.getMaxFps().setValue(150);
            System.out.println("[Yoru Optimized by yoru_topu] FPS cap set to 150.");
        } catch (Throwable t) {
            System.err.println("[Yoru Optimized] Failed to set FPS cap: " + t.getMessage());
        }

        // 2) Auto-install performance mods in a background thread to avoid blocking load
        new Thread(() -> {
            try {
                String modsDir = ModrinthFetcher.detectModsFolder();
                String mcVer = "1.20.1";
                String loader = "fabric";

                String[] wanted = new String[] {
                        "sodium",          // renderer optimization (huge FPS)
                        "lithium",         // logic optimization
                        "starlight",       // faster light engine
                        "indium",          // enables Fabric rendering API on Sodium
                        "entityculling",   // don't render unseen entities
                        "memoryleakfix"    // general memory leak fixes
                };

                List<String> installedNow = new ArrayList<>();
                for (String slug : wanted) {
                    boolean installed = ModrinthFetcher.ensureInstalled(modsDir, slug, mcVer, loader);
                    if (installed) installedNow.add(slug);
                }

                if (!installedNow.isEmpty()) {
                    System.out.println("[Yoru Optimized] Installed: " + installedNow);
                    // We can't safely open a GUI here; print to log and in-game chat (if client ready)
                    try {
                        var mc = MinecraftClient.getInstance();
                        mc.execute(() -> {
                            if (mc.player != null) {
                                mc.player.sendMessage(net.minecraft.text.Text.literal(
                                    "§aYoru Optimized installed: " + String.join(", ", installedNow) + ". §ePlease restart Minecraft."), false);
                            } else {
                                System.out.println("[Yoru Optimized] Please restart Minecraft to enable new mods.");
                            }
                        });
                    } catch (Throwable ignored) {}
                } else {
                    System.out.println("[Yoru Optimized] All optimization mods already present.");
                }
            } catch (Throwable t) {
                System.err.println("[Yoru Optimized] Auto-install failed: " + t.getMessage());
            }
        }, "YoruOptimized-Installer").start();
    }
}
