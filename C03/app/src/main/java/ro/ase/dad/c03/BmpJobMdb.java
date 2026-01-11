package ro.ase.dad.c03;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "bmp.topic"),

        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientId", propertyValue = "c03Consumer"),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "bmpSub"),

        @ActivationConfigProperty(propertyName = "connectionFactoryLookup", propertyValue = "jms/InboundConnectionFactory")
})
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class BmpJobMdb implements MessageListener {

    private final HttpToC06Client c06 = new HttpToC06Client();

    private static final String RMI_TOP_HOST = getenv("RMI_TOP_HOST", "c04");
    private static final int    RMI_TOP_PORT = getint("RMI_TOP_PORT", 1099);

    private static final String RMI_BOTTOM_HOST = getenv("RMI_BOTTOM_HOST", "c05");
    private static final int    RMI_BOTTOM_PORT = getint("RMI_BOTTOM_PORT", 1099);

    private final RmiZoomClient rmi = new RmiZoomClient();

    @EJB
    private JobDonePublisher donePublisher;

    @Override
    public void onMessage(Message message) {
        System.out.println("C03: onMessage HIT, class=" + message.getClass());
        try {
            boolean zoomIn = message.getBooleanProperty("zoomIn");
            int percent = message.getIntProperty("percent");
            String filename = message.getStringProperty("filename");
            String jobId = message.getStringProperty("jobId");

            byte[] bmp = message.getBody(byte[].class);

            System.out.println("C03: Received jobId=" + jobId + " bytes=" + bmp.length +
                    " zoomIn=" + zoomIn + " percent=" + percent);

            System.out.println("C03: header=" + hex(bmp, 16));

            if (bmp.length < 2 || bmp[0] != 'B' || bmp[1] != 'M') {
                throw new IllegalArgumentException("Not a BMP");
            }

            BmpSplitMerge.Split split = BmpSplitMerge.splitHalf(bmp);

            byte[] outTop = rmi.call(RMI_TOP_HOST, RMI_TOP_PORT, split.topBmp, percent, zoomIn);
            byte[] outBottom = rmi.call(RMI_BOTTOM_HOST, RMI_BOTTOM_PORT, split.bottomBmp, percent, zoomIn);

            byte[] merged = BmpSplitMerge.mergeVertical(outTop, outBottom);

            long imageId = c06.storeImage(merged, filename, zoomIn, percent);

            donePublisher.publishJobDone(jobId, imageId);

            System.out.println("C03: Done jobId=" + jobId + " imageId=" + imageId);

        } catch (Exception e) {
            System.out.println("C03 MDB error: " + e.getMessage());
            e.printStackTrace();
            // throw new RuntimeException(e);
        }
    }

    private static String hex(byte[] b, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(n, b.length); i++) {
            sb.append(String.format("%02X ", b[i]));
        }
        return sb.toString().trim();
    }

    private static String getenv(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    private static int getint(String key, int def) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) return def;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
