package ro.ase.dad.c01;

import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;

public class JmsClient implements AutoCloseable {

    public interface OnJobDone {
        void onDone(String jobId, long imageId);
    }

    private final ConnectionFactory cf;
    private final String bmpTopicName;
    private final String doneQueueName;

    private JMSContext consumerCtx;

    public JmsClient(String brokerHost, int brokerPort, String user, String pass,
                     String bmpTopicName, String doneQueueName) {

        String url = "tcp://" + brokerHost + ":" + brokerPort;
        this.cf = new ActiveMQConnectionFactory(user, pass, url);
        this.bmpTopicName = bmpTopicName;
        this.doneQueueName = doneQueueName;
    }

    public void publishBmpJob(String jobId, boolean zoomIn, int percent, String filename, byte[] bmpBytes) {
        try (JMSContext ctx = cf.createContext()) {
            Topic bmpTopic = ctx.createTopic(bmpTopicName);
            JMSProducer producer = ctx.createProducer();

            System.out.println("Publishing to " + bmpTopicName + " at " +
                    ((ActiveMQConnectionFactory) cf).getBrokerURL());

            producer
                    .setProperty("jobId", jobId)
                    .setProperty("zoomIn", zoomIn)
                    .setProperty("percent", percent)
                    .setProperty("filename", filename)
                    .send(bmpTopic, bmpBytes);

            System.out.println("Published jobId=" + jobId + " bytes=" + bmpBytes.length);
        }
    }

    public void startDoneConsumer(OnJobDone handler) {
        this.consumerCtx = cf.createContext();
        Queue doneQueue = consumerCtx.createQueue(doneQueueName);

        JMSConsumer consumer = consumerCtx.createConsumer(doneQueue);
        consumer.setMessageListener(m -> {
            try {
                String jobId = m.getStringProperty("jobId");
                long imageId = m.getLongProperty("imageId");
                handler.onDone(jobId, imageId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void close() {
        try {
            if (consumerCtx != null) consumerCtx.close();
        } catch (Exception ignored) {}
    }
}