package dev.dialogguard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Tiny JSON config stored at config/dialogguard.json. Secure defaults: everything is blocked. */
public final class GuardConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static GuardConfig instance = new GuardConfig();

    /** Block every server dialog that contains at least one input control. */
    public boolean blockInputDialogs = true;
    /** Block dialogs whose text looks like a credential / verification-code request. */
    public boolean blockKeywordDialogs = true;
    public boolean showBanner = true;
    public boolean showToast = true;
    public boolean echoToChat = true;
    public int bannerSeconds = 15;

    /** A single hit on any of these is enough (lower-case, prefix-matched on word start). */
    public List<String> strongKeywords = new ArrayList<>(List.of(
            "microsoft", "devicelogin", "oauth", "xbox live", "mojang account",
            "verification code", "verify code", "2fa", "two-factor", "two factor",
            "one-time", "authenticator", "password"));

    /** Two or more distinct hits on these are required. */
    public List<String> weakKeywords = new ArrayList<>(List.of(
            "code", "verify", "verification", "auth", "login", "log in", "sign in", "e-mail", "email"));

    public static GuardConfig get() {
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("dialogguard.json");
        try {
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    GuardConfig loaded = GSON.fromJson(reader, GuardConfig.class);
                    if (loaded != null) {
                        instance = loaded.sanitized();
                    }
                }
            }
            // Always write back so new options appear in old files.
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException | JsonParseException e) {
            DialogGuardClient.LOGGER.error("Could not read/write config, using secure defaults", e);
            instance = new GuardConfig();
        }
    }

    private GuardConfig sanitized() {
        GuardConfig defaults = new GuardConfig();
        if (strongKeywords == null) strongKeywords = defaults.strongKeywords;
        if (weakKeywords == null) weakKeywords = defaults.weakKeywords;
        bannerSeconds = Math.max(3, Math.min(120, bannerSeconds));
        return this;
    }
}
