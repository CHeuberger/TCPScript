package cfh.tcpscript;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Vector;

import cfh.tcp.Connection;
import cfh.tcp.ConnectionListener;


public class TestConnListener implements ConnectionListener {
    
    private boolean wasStarted = false;
    private boolean wasShutdown = false;
    private final List<byte[]> sent = new Vector<byte[]>();
    private byte[] lastSent = null;
    private final List<byte[]> received = new Vector<byte[]>();
    private byte[] lastReceived = null;
    private final List<Exception> exceptions = new Vector<Exception>();
    
    public void assertWasStarted(boolean started) {
        Thread.yield();
        assertEquals("Started", started, wasStarted);
    }
    
    public void assertWasShutdown(boolean shutdown) {
        Thread.yield();
        assertEquals("Shutdown", shutdown, wasShutdown);
    }
    
    public void assertStatus(boolean started, boolean shutdown) {
        Thread.yield();
        assertEquals("Started", started, wasStarted);
        assertEquals("Shutdown", shutdown, wasShutdown);
    }
    
    public void assertExceptions(int count) {
        Thread.yield();
        assertEquals("Exceptions", count, exceptions.size());
    }
    
    public void assertReceived(int count) {
        sleep();
        assertEquals(count, received.size());
    }
    
    public void assertSent(int count) {
        sleep();
        assertEquals(count, sent.size());
    }
    
    public byte[] getLastSent() {
        return lastSent;
    }
    
    public byte[] getLastReceived() {
        return lastReceived;
    }

    @Override
    public void started(Connection connection) {
        wasStarted = true;
    }
    
    @Override
    public void shutdown(Connection connection) {
        wasShutdown = true;
    }

    @Override
    public void sentData(Connection connection, byte[] data) {
        sent.add(data);
        lastSent = data;
    }
    
    @Override
    public void receivedData(Connection connection, byte[] data) {
        received.add(data);
        lastReceived = data;
    }
    
    @Override
    public void handleException(Connection connection, Exception ex) {
        exceptions.add(ex);
    }
    
//  ================================================================================
    
    static void sleep() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }
    }
}
