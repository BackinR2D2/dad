import ro.ase.dad.rmi.ZoomService;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ZoomServiceImpl extends UnicastRemoteObject implements ZoomService {

    protected ZoomServiceImpl() throws RemoteException {
        super(Integer.parseInt(System.getenv().getOrDefault("RMI_OBJ_PORT", "1109")));
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