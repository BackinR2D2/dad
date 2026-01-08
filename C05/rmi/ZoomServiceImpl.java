import ro.ase.dad.rmi.ZoomService;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ZoomServiceImpl extends UnicastRemoteObject implements ZoomService {

    private static int envInt(String key, int def) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) return def;
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return def; }
    }

    protected ZoomServiceImpl() throws RemoteException {
        super(envInt("RMI_SERVICE_PORT", 2001));
    }

    @Override
    public byte[] zoomBmp(byte[] bmpBytes, int percent, boolean zoomIn) throws RemoteException {
        try {
            return BmpUtils.zoomNearest(bmpBytes, percent, zoomIn);
        } catch (Exception e) {
            throw new RemoteException("Zoom failed: " + e.getMessage(), e);
        }
    }
}