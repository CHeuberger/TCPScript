/*
 * 
 */
package cfh.tcpscript;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cfh.tcp.Connection;
import cfh.tcp.ConnectionListener;
import cfh.tcp.Server;
import cfh.tcp.ServerListener;

public class TestServListener implements ServerListener {
    
    private boolean wasStarted = false;
    private boolean wasShutdown = false;
    private final  ConnListener connListener = new ConnListener();
    private final List<Connection> connections = new ArrayList<Connection>();
    private final List<Exception> exceptions = new ArrayList<Exception>();
    private final Map<Connection, List<byte[]>> received = new HashMap<Connection, List<byte[]>>();
    private int totalReceived = 0;
    private byte[] lastReceived = null;

    public void assertConnections(int count) {
        Thread.yield();
        assertEquals("Connections", count, connections.size());
    }
    
    public void assertExceptions(int count) {
        Thread.yield();
        assertEquals("Exceptions", count, exceptions.size());
    }
    
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
    
    public void assertReceived(Connection connection, int count) {
        List<byte[]> list = received.get(connection);
        assertNotNull(list);
        assertEquals(count, list.size());
    }
    
    public void assertReceived(int count) {
        sleep();
        assertEquals(count, totalReceived);
    }
    
    public byte[] getLastReceived() {
        return lastReceived;
    }
    
    public List<Connection> getConnections() {
        return connections;
    }
    
    public void connected(Server server, Connection connection) {
        connections.add(connection);
        received.put(connection, new ArrayList<byte[]>());
        connection.addListener(connListener);
    }
    
    public void handleException(Server server, Exception ex) {
        exceptions.add(ex);
    }
    
    public void shutdown(Server server) {
        wasShutdown = true;
    }
    
    public void started(Server server) {
        wasStarted = true;
    }
    
    public byte[] getLastReceived(Connection  connection) {
        List<byte[]> list = received.get(connection);
        if (list == null || list.isEmpty()) 
            return null;
        else
            return list.get(list.size()-1);
    }
    
//  ================================================================================
    
    private class ConnListener extends ConnectionListener.Adapter {
        @Override
        public void receivedData(Connection connection, byte[] data) {
            received.get(connection).add(data);
            totalReceived++;
            lastReceived = data;
        }
    }
    
//  ================================================================================
    
    static void sleep() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }
    }
}
