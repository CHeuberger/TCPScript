package cfh.tcpscript;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static java.util.concurrent.TimeUnit.*;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cfh.tcp.Server;


public class ServerChannelTest {
    
    private int port;
    private String name;
    private TestServListener servListener;
    private Server server;
    private Channel channel;
    
    @Before
    public void setUp() {
        port = TestUtilities.getPortNumber();
        name = TestUtilities.getChannelName();
        servListener = new TestServListener();
        server = new Server(port);
        server.addListener(servListener);
        channel = Channel.create(name, server);
    }

    @After
    public void tearDown() throws Exception {
        channel.stop(0);
        channel = null;
        server = null;
        servListener = null;
        name = null;
        port = -1;
    }

//  --------------------------------------------------------------------------------
    
    @Test
    public void testStart() throws Exception {
        Socket socket;
        
        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress("localhost", port), 200);
            fail("expected SocketTimeoutException");
        } catch (SocketTimeoutException expected) {
        }
        socket.close();

        channel.start();
        socket = new Socket();
        socket.connect(new InetSocketAddress("localhost", port), 200);
        assertEquals(true, socket.isConnected());
        socket.close();
    }

    @Test
    public void testStop() throws Exception {
        Socket socket;
        
        channel.start();
        
        socket = new Socket();
        socket.connect(new InetSocketAddress("localhost", port), 200);
        assertTrue(socket.isConnected());
        socket.close();
        
        channel.stop(0);
        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress("localhost",port), 200);
            fail("expected SocketTimeoutException");
        } catch (SocketTimeoutException expected) {
        }
        socket.close();
    }

    @Test
    public void testWaitConnect() throws Exception {
        channel.start();
        
        Callable<Long> waitConnection = new Callable<Long>() {
            public Long call() throws Exception {
                channel.waitConnect(null);
                return System.currentTimeMillis();
            }
        };
        Future<Long> future = Executors.newSingleThreadExecutor().submit(waitConnection);
        try {
            future.get(500, MILLISECONDS);
            fail("Timeout expected: no connection");
        } catch (TimeoutException expected) {
        }
        
        Socket socket = new Socket("localhost", port);
        long connected = System.currentTimeMillis();
        long terminated = future.get(500, MILLISECONDS);
        assertEquals(true, Math.abs(terminated-connected) < 200);

        socket.shutdownOutput();
        channel.stop(0);
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    @Test
    public void testWaitPattern() throws Exception {
        final String regex = ".*/127\\.0\\.0\\.1:\\d+";
        channel.start();
        
        Callable<Long> waitPattern = new Callable<Long>() {
            public Long call() throws Exception {
                channel.waitPattern(regex, null);
                return System.currentTimeMillis();
            }
        };
        Future<Long> future = Executors.newSingleThreadExecutor().submit(waitPattern);
        try {
            future.get(500, MILLISECONDS);
            fail("Timeout expected: not connected");
        } catch (TimeoutException expected) {
        }
        
        Socket socket = new Socket("localhost", port);
        long connected = System.currentTimeMillis();
        long matched = future.get(500, MILLISECONDS);
        assertEquals(true, Math.abs(matched-connected) < 500);

        socket.shutdownOutput();
        channel.stop(0);
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    @Test
    public void testSendData0() throws IOException {
        final String msg1 = "testSendData:msg1";
        final String msg2 = "testSendData:msg2";
        final String msg3 = "testSendData:msg3";

        channel.start();
        final TestClient client1 = new TestClient(port);
        final TestClient client2 = new TestClient(port);

        client1.assertReceived(0);
        client2.assertReceived(0);

        channel.sendData(0, msg1.getBytes());
        client1.assertReceived(1);
        client2.assertReceived(1);
        
        channel.sendData(2, msg2.getBytes());
        client1.assertReceived(1);
        client2.assertReceived(2);
        
        channel.sendData(1, msg3.getBytes());
        client1.assertReceived(2);
        client2.assertReceived(2);
    }

    @Test
    public void testGetSubchannelCount() throws IOException {
        channel.start();
        
        assertEquals(0, channel.getSubchannelCount());
        
        final TestClient client1 = new TestClient(port);
        TestUtilities.sleep();
        assertEquals(1, channel.getSubchannelCount());
        
        final TestClient client2 = new TestClient(port);
        TestUtilities.sleep();
        assertEquals(2, channel.getSubchannelCount());
        
        client1.close();
        TestUtilities.sleep();
        assertEquals(1, channel.getSubchannelCount());
        
        client2.close();
        TestUtilities.sleep();
        assertEquals(0, channel.getSubchannelCount());
    }

    @Test
    public void testServerChannel() {
        // private class
        // tested in ChannelTest.testCreate_Server()
    }
    
    @Test
    public void testGetPeer() {
        assertEquals(server, channel.getPeer());
    }

    @Test
    public void testSetHeader() throws IOException {
        final String msg1 = "testSetHeader:msg1";
        final String msg2 = "testSetHeader:msg2";
        final String header = ">|";

        channel.start();
        final TestClient client = new TestClient(port);
        client.assertReceived(0);
        byte[] last;
        
        channel.sendData(0, msg1);
        client.assertReceived(1);
        last = client.getLastReceived();
        assertEquals(msg1, new String(last));
        
        channel.setHeader(header);
        channel.sendData(0, msg2);
        client.assertReceived(2);
        last = client.getLastReceived();
        assertEquals(header + msg2, new String(last));
    }

    @Test
    public void testSetFooter() throws IOException {
        final String msg1 = "testSetFooter:msg1";
        final String msg2 = "testSetFooter:msg2";
        final String footer = "|<";

        channel.start();
        final TestClient client = new TestClient(port);
        client.assertReceived(0);
        byte[] last;
        
        channel.sendData(0, msg1);
        client.assertReceived(1);
        last = client.getLastReceived();
        assertEquals(msg1, new String(last));
        
        channel.setFooter(footer);
        channel.sendData(0, msg2);
        client.assertReceived(2);
        last = client.getLastReceived();
        assertEquals(msg2 + footer, new String(last));
    }


    @Test
    public void testGetHeader() {
        final String header = "-[";
        
        assertEquals("", channel.getHeader());
        
        channel.setHeader(header);
        assertEquals(header, channel.getHeader());
    }

    @Test
    public void testGetFooter() {
        final String footer = "]-";
        
        assertEquals("", channel.getFooter());
        
        channel.setFooter(footer);
        assertEquals(footer, channel.getFooter());
    }

    @Test
    public void testGetName() {
        assertEquals(name, channel.getName());
    }
    
    @Test
    public void testSendData() throws IOException {
        final String msg1 = "testSendData:msg1";
        final String msg2 = "testSendData:msg2";

        channel.start();
        final TestClient client1 = new TestClient(port);
        final TestClient client2 = new TestClient(port);
        client1.assertReceived(0);
        client2.assertReceived(0);
        byte[] last;
        
        channel.sendData(0, msg1);
        client1.assertReceived(1);
        last = client1.getLastReceived();
        assertEquals(msg1, new String(last));
        client2.assertReceived(1);
        last = client2.getLastReceived();
        assertEquals(msg1, new String(last));
        
        channel.sendData(2, msg2);
        client1.assertReceived(1);
        last = client1.getLastReceived();
        assertEquals(msg1, new String(last));
        client2.assertReceived(2);
        last = client2.getLastReceived();
        assertEquals(msg2, new String(last));
    }
}
