package ro.ase.dad.c03;

import jakarta.ejb.Stateless;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Message;
import jakarta.jms.Queue;

import javax.naming.InitialContext;

@Stateless
public class JobDonePublisher {

    private ConnectionFactory lookupCF() {
        try {
            InitialContext ic = new InitialContext();

            return (ConnectionFactory) ic.lookup("java:openejb/Resource/jms/ConnectionFactory");
        } catch (Exception e) {
            throw new RuntimeException("JNDI lookup failed for ConnectionFactory: " + e.getMessage(), e);
        }
    }

    private Queue lookupQueue() {
        try {
            InitialContext ic = new InitialContext();
            return (Queue) ic.lookup("java:openejb/Resource/jms/jobDoneQueue");
        } catch (Exception e) {
            throw new RuntimeException("JNDI lookup failed for Queue: " + e.getMessage(), e);
        }
    }

    public void publishJobDone(String jobId, long imageId) {
        ConnectionFactory cf = lookupCF();
        Queue q = lookupQueue();

        try (JMSContext ctx = cf.createContext(jakarta.jms.Session.AUTO_ACKNOWLEDGE)) {
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