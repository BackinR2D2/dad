package ro.ase.dad.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ZoomService extends Remote {
    byte[] zoomBmp(byte[] bmpBytes, int percent, boolean zoomIn) throws RemoteException;
}
