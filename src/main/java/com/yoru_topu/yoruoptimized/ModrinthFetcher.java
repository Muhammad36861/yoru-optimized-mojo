package com.yoru_topu.yoruoptimized;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Tiny helper that queries Modrinth for the latest Fabric 1.20.1 version of a mod
 * and downloads the primary .jar file to the mods folder.
 * No external libs; naive JSON parsing via regex to keep it self-contained.
 */
public class ModrinthFetcher {

    // Basic JSON field extraction: finds the first "url":"...jar" in the version response
    private static final Pattern URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"(https:[^\"]+\\.jar)\"");

    public static boolean ensureInstalled(String modsDir, String modrinthSlug, String mcVersion, String loader) {
        try {
            if (isPresent(modsDir, modrinthSlug)) return false; // already installed

            String api = "https://api.modrinth.com/v2/project/" + URLEncoder.encode(modrinthSlug, "UTF-8")
                    + "/version?game_versions=[\"" + mcVersion + "\"]&loaders=[\"" + loader + "\"]";

            String json = httpGet(api, 20_000);
            if (json == null || json.isEmpty()) return false;

            // pick first version in array
            Matcher m = URL_PATTERN.matcher(json);
            if (!m.find()) return false;

            String jarUrl = m.group(1);
            String fileName = jarUrl.substring(jarUrl.lastIndexOf('/') + 1);

            Path target = Paths.get(modsDir, fileName);
            download(jarUrl, target);
            return true;
        } catch (Exception e) {
            System.err.println("[Yoru Optimized] Failed to install " + modrinthSlug + ": " + e.getMessage());
            return false;
        }
    }

    private static boolean isPresent(String modsDir, String slug) throws IOException {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(modsDir), "*.jar")) {
            for (Path p : ds) {
                String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.contains(slug.toLowerCase(Locale.ROOT))) return true;
            }
        } catch (NoSuchFileException e) {
            // mods dir may not exist yet
        }
        return false;
    }

    private static String httpGet(String url, int timeoutMs) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", "YoruOptimized/1.0 (+fabric)");
        c.setConnectTimeout(timeoutMs);
        c.setReadTimeout(timeoutMs);
        try (InputStream in = c.getInputStream()) {
            return new String(in.readAllBytes());
        }
    }

    private static void download(String url, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", "YoruOptimized/1.0 (+fabric)");
        try (InputStream in = c.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("[Yoru Optimized] Downloaded: " + target.getFileName());
    }

    public static String detectModsFolder() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String modsPath;
        if (os.contains("win")) {
            String appdata = System.getenv("APPDATA");
            if (appdata != null) {
                modsPath = appdata + "\\.minecraft\\mods";
            } else {
                modsPath = System.getProperty("user.home") + "\\AppData\\Roaming\\.minecraft\\mods";
            }
        } else if (os.contains("mac")) {
            modsPath = System.getProperty("user.home") + "/Library/Application Support/minecraft/mods";
        } else {
            modsPath = System.getProperty("user.home") + "/.minecraft/mods";
        }
        return modsPath;
    }
}
