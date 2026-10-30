package com.arabatube.app;

/**
 * Telefon ekranı (MainActivity) ile araç ekranı (TubeCarActivity) arasındaki basit köprü.
 * İkisi de aynı süreçte çalıştığı için statik alan yeterli.
 */
public final class CarBridge {

    public interface Listener {
        void load(String url);
    }

    private static Listener listener;
    private static String pending;

    private CarBridge() {}

    /** Araç ekranı açılınca kendini kaydeder; bekleyen link varsa hemen yüklenir. */
    public static synchronized void setListener(Listener l) {
        listener = l;
        if (l != null && pending != null) {
            String p = pending;
            pending = null;
            l.load(p);
        }
    }

    /** @return true: araç ekranı açık, hemen yüklendi. false: açılınca yüklenecek. */
    public static synchronized boolean send(String url) {
        if (listener != null) {
            listener.load(url);
            return true;
        }
        pending = url;
        return false;
    }

    public static synchronized boolean isCarConnected() {
        return listener != null;
    }
}
