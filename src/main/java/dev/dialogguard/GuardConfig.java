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
    private static volatile GuardConfig instance = new GuardConfig();

    /** Block every server dialog that contains at least one input control. */
    public boolean blockInputDialogs = true;
    /** Block dialogs whose text looks like a credential / verification-code request. */
    public boolean blockKeywordDialogs = true;
    public boolean showBanner = true;
    public boolean showToast = true;
    public boolean echoToChat = true;
    public int bannerSeconds = 15;
    /** Minimum seconds between toast/chat alerts; 0 disables throttling (banner still updates). */
    public int notificationCooldownSeconds = 4;

    /** A single hit on any of these is enough (lower-case, prefix-matched on word start). */
    public List<String> strongKeywords = new ArrayList<>(List.of(
            // --- Microsoft / Xbox / Mojang / Minecraft ---
            "microsoft", "devicelogin", "oauth", "xbox live", "mojang account", "minecraft account",
            "live.com", "microsoftonline", "azure", "msn", "outlook", "hotmail", "realm token",
            // --- Other Major Platforms & Phishing Targets ---
            "discord", "steam", "epic games", "google account", "gmail", "apple id", "icloud",
            "twitch", "reddit", "telegram", "whatsapp", "riot games", "battlenet", "playstation",
            // --- Credentials, Passwords & Secrets ---
            "password", "sifre", "şifre", "parola", "credential", "credentials", "giris bilgisi",
            "passcode", "masterkey", "master key", "secret", "gizli kelime", "seed phrase",
            // --- Multi-Factor, OTP & Verification Codes ---
            "verification code", "verify code", "2fa", "two-factor", "two factor", "mfa",
            "one-time", "otp", "authenticator", "security code", "dogrulama kodu", "güvenlik kodu",
            "sms code", "pin code", "pincode", "pin kodu", "tanitim kodu",
            // --- Tokens, API Keys & Sessions ---
            "access token", "refresh token", "session id", "session token", "api key", "secret key",
            "bearer", "auth token", "cookie", "session cookie", "jwt",
            // --- Recovery, Backup & Security Questions ---
            "recovery code", "backup code", "kurtarma kodu", "yedek kod", "security question",
            "guvenlik sorusu", "gizli soru", "yedek eposta", "backup email",
            // --- Financial & Rewards Scams (Common in MC Servers) ---
            "credit card", "kredi karti", "kredi kartı", "cvv", "iban", "papara", "tosla",
            "ininal", "bitcoin", "crypto", "kripto", "wallet address", "cüzdan adresi",
            "free robux", "free nitro", "free store", "bedava sinyal", "hediye çeki",
            // --- Multilingual / Multi-language (German, Spanish, French, etc.) ---
            "passwort", "kennwort", "anmelden", "konto", "benutzerkonto", "bestätigungscode",
            "contraseña", "cuenta", "verificación", "codigo", "connexion", "mot de passe",
            // --- Chinese (Simplified) / 中文 ---
            "密码", "口令", "验证码", "安全码", "登录", "账号", "账户", "授权", "令牌", "绑定",
            // --- Japanese / 日本語 ---
            "パスワード", "暗証番号", "認証コード", "確認コード", "ログイン", "アカウント", "2段階認証", "トークン"
    ));

    /** Two or more distinct hits on these are required. */
    public List<String> weakKeywords = new ArrayList<>(List.of(
            // --- Actions & Authentication ---
            "code", "verify", "verification", "auth", "login", "log in", "sign in", "signin",
            "register", "signup", "sign up", "kayit", "kayıt", "giris", "giriş", "connect", "baglan",
            // --- Communication & IDs ---
            "e-mail", "email", "mail", "posta", "username", "kullanici adi", "kullanıcı adı",
            "account", "hesap", "user", "kullanici", "kullanıcı", "profile", "profil", "id",
            // --- Verification & Confirmation ---
            "confirm", "confirmation", "onayla", "onaylama", "validate", "validation", "dogrula",
            "dogrulama", "aktiflestir", "aktifleştir", "activate", "activation", "sync", "senkronize",
            // --- Security, Privacy & Danger Terms ---
            "security", "guvenlik", "güvenlik", "protect", "koruma", "access", "erisim", "erişim",
            "device", "cihaz", "browser", "tarayici", "tarayıcı", "ip address", "ip adresi",
            "banned", "yasakli", "yasaklı", "suspicious", "supheli", "şüpheli", "risk", "danger",
            // --- Server / Command / UI Traps ---
            "click", "tikla", "tıkla", "link", "url", "web", "site", "browser", "tarayici",
            "rewards", "odul", "ödül", "gift", "hediye", "bonus", "claim", "al", "kazan",
            // --- Multilingual Weak Terms ---
            "code", "token", "user", "pass", "mail", "konto", "clave", "accès", "usuario",
            // --- Chinese Weak Terms / 中文通用 ---
            "验证", "确认", "安全", "邮箱", "用户", "绑定", "点击", "链接",
            // --- Japanese Weak Terms / 日本語一般 ---
            "確認", "認証", "セキュリティ", "メール", "ユーザー", "登録", "リンク", "クリック"
    ));

    public static GuardConfig get() {
        return instance;
    }

    /**
     * Loads (or creates) the config file. Returns false when the file could not be read or
     * written; in that case the secure defaults are used and a diagnostic is logged.
     */
    public static boolean load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("dialogguard.json");
        try {
            GuardConfig loaded = new GuardConfig();
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    GuardConfig fromFile = GSON.fromJson(reader, GuardConfig.class);
                    if (fromFile != null) loaded = fromFile;
                }
            }
            instance = loaded.sanitized();

            Files.createDirectories(path.getParent());
            // Always write back so new options appear in old files.
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(instance, writer);
            }
            return true;
        } catch (IOException | JsonParseException e) {
            DialogGuardClient.LOGGER.error("Could not read/write config, using secure defaults", e);
            instance = new GuardConfig();
            return false;
        }
    }

    private GuardConfig sanitized() {
        GuardConfig defaults = new GuardConfig();
        if (strongKeywords == null) strongKeywords = defaults.strongKeywords;
        if (weakKeywords == null) weakKeywords = defaults.weakKeywords;
        bannerSeconds = Math.max(3, Math.min(120, bannerSeconds));
        notificationCooldownSeconds = Math.max(0, Math.min(120, notificationCooldownSeconds));
        return this;
    }
}
