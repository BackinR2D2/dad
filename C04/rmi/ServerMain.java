import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerMain {

    private static int envInt(String key, int def) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) return def;
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return def; }
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    public static void main(String[] args) throws Exception {
        int registryPort = envInt("RMI_PORT", 1099);
        String name = env("RMI_NAME", "ZoomService");
        String hostname = env("RMI_HOSTNAME", "c04");

        System.setProperty("java.rmi.server.hostname", hostname);

        Registry reg = LocateRegistry.createRegistry(registryPort);
        reg.rebind(name, new ZoomServiceImpl());

        System.out.println("RMI ready: " + hostname + ":" + registryPort + " name=" + name +
                " objPort=" + System.getenv().getOrDefault("RMI_OBJ_PORT", "1109"));

        Thread.currentThread().join();
    }
}