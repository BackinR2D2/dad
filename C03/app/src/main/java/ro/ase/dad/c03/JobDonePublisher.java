package ro.ase.dad.c03;

import jakarta.ejb.Stateless;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.Session;

import javax.naming.InitialContext;

@Stateless
public class JobDonePublisher {

    private volatile ConnectionFactory cachedCf;
    private volatile Queue cachedQueue;

    private ConnectionFactory lookupOutboundCF() {
        ConnectionFactory cf = cachedCf;
        if (cf != null) return cf;

        try {
            InitialContext ic = new InitialContext();
            cf = (ConnectionFactory) ic.lookup("java:openejb/Resource/jms/OutboundConnectionFactory");
            cachedCf = cf;
            return cf;
        } catch (Exception e) {
            throw new RuntimeException("JNDI lookup failed for OutboundConnectionFactory: " + e.getMessage(), e);
        }
    }

    private Queue lookupJobDoneQueue() {
        Queue q = cachedQueue;
        if (q != null) return q;

        try {
            InitialContext ic = new InitialContext();
            q = (Queue) ic.lookup("java:openejb/Resource/jms/jobDoneQueue");
            cachedQueue = q;
            return q;
        } catch (Exception e) {
            throw new RuntimeException("JNDI lookup failed for jobDoneQueue: " + e.getMessage(), e);
        }
    }

    public void publishJobDone(String jobId, long imageId) {
        ConnectionFactory cf = lookupOutboundCF();
        Queue q = lookupJobDoneQueue();

        try (JMSContext ctx = cf.createContext(Session.AUTO_ACKNOWLEDGE)) {
            JMSProducer producer = ctx.createProducer();
            Message msg = ctx.createMessage();
            msg.setStringProperty("jobId", jobId);
            msg.setLongProperty("imageId", imageId);
            producer.send(q, msg);
        } catch (Exception e) {
            System.err.println("C03: Failed to publish job done: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
