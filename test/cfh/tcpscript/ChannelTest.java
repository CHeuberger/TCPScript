package cfh.tcpscript;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cfh.tcp.Connection;
import cfh.tcp.Server;

public class ChannelTest {

    private Server testServer;
    private TestServListener testServListener;
    
    private TestClient testClient;
    private Connection testConnection;

    @Before
    public void setUp() throws Exception {
        testServer = new Server(0);
        testServListener = new TestServListener();
        testServer.addListener(testServListener);
        testServer.start();
        
        testClient = new TestClient(testServer.getPort());
        testConnection = testClient.getConnection();
    }

    @After
    public void tearDown() throws Exception {
        testClient.stop();
        try {
            testClient.close();
        } catch (IOException ignored) {
        }

        testServer.stop();
        List<String> copy = new ArrayList<String>(Channel.getChannelNames());
        for (String name : copy) {
            try {
                Channel channel = Channel.get(name);
                if (channel != null) {
                    channel.stop(0);
                }
            } catch (IOException ignore) {
            }
            Channel.remove(name);
        }
    }

//  --------------------------------------------------------------------------------
    
    @Test
    public void testCreate_Server() {
        final String name = TestUtilities.getChannelName();
        assertTrue(Channel.get(name) == null);
        
        final Channel channel = Channel.create(name, testServer);
        assertSame(testServer, channel.getPeer());
        assertEquals(true, Channel.getChannelNames().contains(name));
        assertSame(channel, Channel.get(name));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testCreate_Server_NullName() {
        Channel.create(null, testServer);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testCreate_Server_DuplicatedName() {
        final String name = TestUtilities.getChannelName();
        Channel.create(name, testServer);
        Channel.create(name, testServer);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testCreate_Server_NullServer() {
        Channel.create(TestUtilities.getChannelName(), (Server) null);
    }

    @Test
    public void testCreate_Connection() {
        final String name = TestUtilities.getChannelName();
        assertTrue(Channel.get(name) == null);
        
        final Channel channel = Channel.create(name, testConnection);
        assertSame(testConnection, channel.getPeer());
        assertEquals(true, Channel.getChannelNames().contains(name));
        assertSame(channel, Channel.get(name));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testCreate_Connection_NullName() {
        Channel.create(null, testConnection);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testCreate_Connection_DuplicatedName() {
        final String name = TestUtilities.getChannelName();
        Channel.create(name, testConnection);
        Channel.create(name, testConnection);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testCreate_Connection_NullConnection() {
        Channel.create(TestUtilities.getChannelName(), (Connection) null);
    }

    @Test
    public void testGet() {
        final String name1 = TestUtilities.getChannelName();
        final String name2 = TestUtilities.getChannelName();
        
        assertNull(Channel.get(name1));
        assertNull(Channel.get(name2));
        
        final Channel channel1 = Channel.create(name1, testServer);
        assertSame(channel1, Channel.get(name1));
        assertEquals(null, Channel.get(name2));
        
        final Channel channel2 = Channel.create(name2, testConnection);
        assertSame(channel1, Channel.get(name1));
        assertSame(channel2, Channel.get(name2));
    }

    @Test
    public void testGetChannelNames() {
        final String name1 = TestUtilities.getChannelName();
        final String name2 = TestUtilities.getChannelName();
        Set<String> names;

        names = Channel.getChannelNames();
        assertEquals(true, names.isEmpty());
        
        Channel.create(name1, testServer);
        names = Channel.getChannelNames();
        assertEquals(1, names.size());
        assertEquals(name1, names.toArray()[0]);
        
        Channel.create(name2, testServer);
        names = Channel.getChannelNames();
        assertEquals(2, names.size());
        assertEquals(true, names.contains(name1));
        assertEquals(true, names.contains(name2));
    }

    @Test
    public void testFindName() {
        final String name1 = TestUtilities.getChannelName();
        final String name2 = TestUtilities.getChannelName();
        Channel.create(name1, testServer);
        Channel.create(name2, testConnection);
        assertEquals(name1, Channel.findName(testServer));
        assertEquals(name2, Channel.findName(testConnection));
    }

    @Test
    public void testRemove() {
        final String name1 = TestUtilities.getChannelName();
        final String name2 = TestUtilities.getChannelName();
        final Channel channel1 = Channel.create(name1, testServer);
        final Channel channel2 = Channel.create(name2, testServer);
        assertTrue(Channel.getChannelNames().size() == 2);
        
        assertSame(channel1, Channel.remove(name1));
        assertEquals(1, Channel.getChannelNames().size());
        
        assertEquals(null, Channel.remove(name1));
        assertEquals(1, Channel.getChannelNames().size());
        
        assertEquals(channel2, Channel.remove(name2));
        assertEquals(0, Channel.getChannelNames().size());
    }

//  --------------------------------------------------------------------------------
    
    @Test
    public void testChannel() {
        // abstract class constructor, 
        // tested in testCreate_Connection and testCreate_Server
    }

    @Test
    public void testSetHeader() throws IOException {
        final String msg1 = "msg1";
        final String msg2 = "msg2";
        final String header = "-=";
        final Channel server = Channel.create(TestUtilities.getChannelName(), testServer);
        
        byte[] last;
        
        server.sendData(0, msg1);
        testClient.assertReceived(1);
        last = testClient.getLastReceived();
        assertTrue(msg1.equals(new String(last)));
        
        server.setHeader(header);
        server.sendData(0, msg2);
        testClient.assertReceived(2);
        last = testClient.getLastReceived();
        assertEquals(header+msg2, new String(last));
    }

    @Test
    public void testSetFooter() throws IOException {
        final String msg1 = "msg1";
        final String msg2 = "msg2";
        final String footer = "=-";
        final Channel server = Channel.create(TestUtilities.getChannelName(), testServer);
        
        byte[] last;
        
        server.sendData(0, msg1);
        testClient.assertReceived(1);
        last = testClient.getLastReceived();
        assertTrue(msg1.equals(new String(last)));
        
        server.setFooter(footer);
        server.sendData(0, msg2);
        testClient.assertReceived(2);
        last = testClient.getLastReceived();
        assertEquals(msg2+footer, new String(last));
    }

    @Test
    public void testGetHeader() {
        final String header1 = "-=";
        final String header2 = "01";
        final Channel server = Channel.create(TestUtilities.getChannelName(), testServer);
        final Channel client = Channel.create(TestUtilities.getChannelName(), testConnection);
        
        assertEquals("", server.getHeader());
        
        server.setHeader(header1);
        assertEquals(header1, server.getHeader());
        
        assertEquals("", client.getHeader());
        
        client.setHeader(header2);
        assertEquals(header2, client.getHeader());
    }

    @Test
    public void testGetFooter() {
        final String footer1 = "ab";
        final String footer2 = "<<";
        final Channel server = Channel.create(TestUtilities.getChannelName(), testServer);
        final Channel client = Channel.create(TestUtilities.getChannelName(), testConnection);
        
        assertEquals("", server.getFooter());
        
        server.setFooter(footer1);
        assertEquals(footer1, server.getFooter());
        
        assertEquals("", client.getFooter());
        
        client.setFooter(footer2);
        assertEquals(footer2, client.getFooter());
    }
    
    @Test
    public void testGetName() {
        final String name = TestUtilities.getChannelName();
        final Channel channel = Channel.create(name, testConnection);
        
        assertEquals(name, channel.getName());
    }

    @Test
    public void testStart() {
        // tested in ServerChannelTest and ClientServerTest
    }

    @Test
    public void testStop() {
        // tested in ServerChannelTest and ClientServerTest
    }

    @Test
    public void testWaitConnect() {
        // tested in ServerChannelTest and ClientServerTest
    }

    @Test
    public void testWaitPattern() {
        // tested in ServerChannelTest and ClientServerTest
    }

    @Test
    public void testSendData() throws IOException {
        final String msg1 = "msg1";
        final String msg2 = "msg2";
        final Channel server = Channel.create(TestUtilities.getChannelName(), testServer);
        final Channel client = Channel.create(TestUtilities.getChannelName(), testConnection);
        
        byte[] last;
        
        server.sendData(0, msg1);
        testClient.assertReceived(1);
        last = testClient.getLastReceived();
        assertTrue(msg1.equals(new String(last)));
        
        client.sendData(0, msg2);
        testServListener.assertReceived(1);
        last = testServListener.getLastReceived();
        assertEquals(msg2, new String(last));
    }

    @Test
    public void testSendData0() {
        // tested in ServerChannelTest and ClientServerTest
    }

    @Test
    public void testGetPeer() {
        final Channel server = Channel.create(TestUtilities.getChannelName(), testServer);
        final Channel client = Channel.create(TestUtilities.getChannelName(), testConnection);
        
        assertEquals(testServer, server.getPeer());
        
        assertEquals(testConnection, client.getPeer());
    }

    @Test
    public void testGetSubchannelCount() throws IOException {
        final int port = TestUtilities.getPortNumber();
        final Server serv1 = new Server(port);
        final Channel server = Channel.create(TestUtilities.getChannelName(), serv1);
        server.start();
        
        assertEquals(0, server.getSubchannelCount());
        
        final Connection connection = new Connection("localhost", serv1.getPort());
        final Channel client = Channel.create(TestUtilities.getChannelName(), connection);
        TestUtilities.sleep();
        assertEquals(1, server.getSubchannelCount());
        assertEquals(0, client.getSubchannelCount());
    }
}
