package eformx.app;

/**
 * Enterprise Secure Configuration & String Obfuscation Vault
 * Protects backend API endpoints, URLs, and storage keys against APK reverse-engineering,
 * static DEX string inspection, and decompilers.
 */
public final class SecureConfig {

    private SecureConfig() {
        // Prevent direct instantiation
    }

    private static final byte XOR_KEY = 0x5E;

    // Encrypted byte arrays generated via dynamic multi-mask XOR cipher
    private static final byte[] ENC_INSTALL_API = new byte[]{
        (byte) 0x36, (byte) 0x2B, (byte) 0x28, (byte) 0x2D, (byte) 0x29, (byte) 0x61, (byte) 0x77, (byte) 0x76, (byte) 0x37, (byte) 0x27, (byte) 0x3D, (byte) 0x7B, (byte) 0x37, (byte) 0x35, (byte) 0x3F, (byte) 0x23, (byte) 0x33, (byte) 0x27, (byte) 0x72, (byte) 0x34, (byte) 0x34, (byte) 0x74, (byte) 0x67, (byte) 0x38, (byte) 0x26, (byte) 0x3E, (byte) 0x69, (byte) 0x3C, (byte) 0x3C, (byte) 0x20, (byte) 0x24, (byte) 0x30, (byte) 0x32, (byte) 0x33, (byte) 0x73, (byte) 0x3C, (byte) 0x2A, (byte) 0x2B
    }; // "https://api.eformx.in/?api=install/app"

    private static final byte[] ENC_FCM_STORE_API = new byte[]{
        (byte) 0x36, (byte) 0x2B, (byte) 0x28, (byte) 0x2D, (byte) 0x29, (byte) 0x61, (byte) 0x77, (byte) 0x76, (byte) 0x37, (byte) 0x27, (byte) 0x3D, (byte) 0x7B, (byte) 0x37, (byte) 0x35, (byte) 0x3F, (byte) 0x23, (byte) 0x33, (byte) 0x27, (byte) 0x72, (byte) 0x34, (byte) 0x34, (byte) 0x74, (byte) 0x67, (byte) 0x38, (byte) 0x26, (byte) 0x3E, (byte) 0x69, (byte) 0x13, (byte) 0x11, (byte) 0x1E, (byte) 0x7F, (byte) 0x22, (byte) 0x2A, (byte) 0x30, (byte) 0x2E, (byte) 0x38
    }; // "https://api.eformx.in/?api=FCM/store"

    private static final byte[] ENC_DEFAULT_URL = new byte[]{
        (byte) 0x36, (byte) 0x2B, (byte) 0x28, (byte) 0x2D, (byte) 0x29, (byte) 0x61, (byte) 0x77, (byte) 0x76, (byte) 0x33, (byte) 0x31, (byte) 0x3B, (byte) 0x27, (byte) 0x3F, (byte) 0x2B, (byte) 0x7E, (byte) 0x32, (byte) 0x31, (byte) 0x32, (byte) 0x73
    }; // "https://eformx.com/"

    private static final byte[] ENC_PREFS_NAME = new byte[]{
        (byte) 0x3B, (byte) 0x39, (byte) 0x33, (byte) 0x2F, (byte) 0x37, (byte) 0x23, (byte) 0x07, (byte) 0x29, (byte) 0x24, (byte) 0x32, (byte) 0x32, (byte) 0x26
    }; // "eformx_prefs"

    private static final byte[] ENC_KEY_REDIRECT = new byte[]{
        (byte) 0x2C, (byte) 0x3A, (byte) 0x38, (byte) 0x34, (byte) 0x28, (byte) 0x3E, (byte) 0x3B, (byte) 0x2D, (byte) 0x09, (byte) 0x22, (byte) 0x26, (byte) 0x39
    }; // "redirect_url"

    private static final byte[] ENC_KEY_FCM_TOKEN = new byte[]{
        (byte) 0x38, (byte) 0x3C, (byte) 0x31, (byte) 0x02, (byte) 0x2E, (byte) 0x34, (byte) 0x33, (byte) 0x3C, (byte) 0x38
    }; // "fcm_token"

    private static final byte[] ENC_KEY_DEVICE_ID = new byte[]{
        (byte) 0x3A, (byte) 0x3A, (byte) 0x2A, (byte) 0x34, (byte) 0x39, (byte) 0x3E, (byte) 0x07, (byte) 0x30, (byte) 0x32
    }; // "device_id"

    private static String decode(byte[] data) {
        if (data == null) return "";
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ (XOR_KEY ^ (i & 0x0F)));
        }
        return new String(result, java.nio.charset.StandardCharsets.UTF_8);
    }

    public static String getInstallApiUrl() {
        return decode(ENC_INSTALL_API);
    }

    public static String getFcmStoreApiUrl() {
        return decode(ENC_FCM_STORE_API);
    }

    public static String getDefaultWebUrl() {
        return decode(ENC_DEFAULT_URL);
    }

    public static String getPrefsName() {
        return decode(ENC_PREFS_NAME);
    }

    public static String getKeyRedirectUrl() {
        return decode(ENC_KEY_REDIRECT);
    }

    public static String getKeyFcmToken() {
        return decode(ENC_KEY_FCM_TOKEN);
    }

    public static String getKeyDeviceId() {
        return decode(ENC_KEY_DEVICE_ID);
    }
}
