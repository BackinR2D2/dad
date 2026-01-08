package ro.ase.dad.c03;

import ro.ase.dad.rmi.ZoomService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiZoomClient {

    public byte[] call(String host, int port, byte[] bmp, int percent, boolean zoomIn) throws Exception {
        Registry reg = LocateRegistry.getRegistry(host, port);
        ZoomService svc = (ZoomService) reg.lookup("ZoomService");
        return svc.zoomBmp(bmp, percent, zoomIn);
    }
}