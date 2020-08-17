package cfh.tcpscript;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import cfh.tcp.Connection;
import cfh.tcp.ConnectionListener;
import cfh.tcp.Server;
import cfh.tcp.ServerListener;

/**
 * TODO
 * 
 * @author Carlos Heuberger
 * $Revision: 1.19 $
 */
public abstract class Channel {

    public static String getRevision() {
        return Channel.class.getName() + " $Revision: 1.19 $";
    }
    
    // should be synchronized
    private static final Map<String, Channel> channels = new Hashtable<String, Channel>();
    
    private static final List<Listener> listeners = new ArrayList<Listener>();
    
    private static final ShutServListener shutServListener = new ShutServListener();
    private static final ShutConnListener shutConnListener = new ShutConnListener();

    public static Channel create(String name, Server server) {
        checkCreateArg(name, server);
        
        ServerChannel channel = new ServerChannel(name, server);
        channels.put(name, channel);
        server.addListener(shutServListener);
        for (Listener listener : listeners) {
            listener.created(channel, server);
        }
        return channel;
    }

    public static Channel create(String name, Connection client) {
        checkCreateArg(name, client);
        
        ClientChannel channel = new ClientChannel(name, client);
        channels.put(name, channel);
        client.addListener(shutConnListener);
        for (Listener listener : listeners) {
            listener.created(channel, client);
        }
        return channel;
    }
    
    public static Channel get(String name) {
        return channels.get(name);
    }
    
    public static Set<String> getChannelNames() {
        return channels.keySet();
    }
    
    public static String findName(Object peer) {
        ArrayList<Channel> copy = new ArrayList<Channel>(channels.values());
        for (Channel channel : copy) {
            if (channel.getPeer() == peer)
                return channel.getName();
        }
        return null;
    }
    
    public static Channel remove(String name) {
        Channel channel = channels.remove(name);
        if (channel != null) {
            for (Listener listener : listeners) {
                listener.removed(channel);
            }
        }
        return channel;
    }
    
    public static void addListener(Listener listener) {
        listeners.add(listener);
    }
    
    public static void removeListener(Listener listener) {
        listeners.remove(listener);
    }
    
    private static void checkCreateArg(String name, Object peer) {
        if (name == null)
            throw new IllegalArgumentException("name must not be null");
        if (channels.containsKey(name))
            throw new IllegalArgumentException("name already defined: " + name);
        if (peer == null)
            throw new IllegalArgumentException("peer must not be null");
    }

//  ============================================================================
    
    private final String name;
    private String header = "";
    private String footer = "";
    
    protected Channel(String name) {
        this.name = name;
    }
    
    public void setHeader(String header) {
        this.header = (header != null) ? header : "";
    }
    
    public void setFooter(String footer) {
        this.footer = (footer != null) ? footer : "";
    }
    
    public String getHeader() {
        return header;
    }
    
    public String getFooter() {
        return footer;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Channel)) return false;
        Channel other = (Channel) obj;
        return other.name.equals(name);
    }
    
    public abstract void start() throws IOException;
    
    public abstract void stop(int subchannel) throws IOException;

    public abstract void abort(int subchannel) throws IOException;
 
    public abstract void waitConnect(Appendable debug) throws InterruptedException;
    
    public abstract void waitPattern(String regex, Appendable debug) throws InterruptedException;

    public void sendData(int subchannel, String data) throws IOException {
        sendData(subchannel, StringHelper.toByte(header + data + footer));
    }
    
    public abstract void sendData(int subchannel, byte[] data) throws IOException;
    
    public abstract Object getPeer();
    
    public abstract int getSubchannelCount();

    public abstract void addConnectionListener(ConnectionListener listener);
    
    public abstract void removeConnectionListener(ConnectionListener listener);

//  ############################################################################

    private static class ServerChannel extends Channel {
        
        private final Server server;
        
        protected ServerChannel(String name, Server server) {
            super(name);
            this.server = server;
            server.addListener(new SubchannelServListener(name));
        }
        
        @Override
        public void start() throws IOException {
            server.start();
        }

        @Override
        public void stop(int subchannel) throws IOException {
            if (subchannel == 0) {
                server.stop();
                ArrayList<Connection> copy = new ArrayList<Connection>(server.getConnections());
                for (Connection connection : copy) {
                    connection.stop();
                }
            } else {
                Connection connection = server.getConnections().get(subchannel-1);
                connection.stop();
            }
        }
        
        @Override
        public void abort(int subchannel) throws IOException {
            if (subchannel == 0) {
                server.stop();
                ArrayList<Connection> copy = new ArrayList<Connection>(server.getConnections());
                for (Connection connection : copy) {
                    connection.close();
                }
            } else {
                Connection connection = server.getConnections().get(subchannel-1);
                connection.close();
            }
        }
        
        @Override
        public void waitConnect(final Appendable debug) throws InterruptedException {
            final Lock lock = new ReentrantLock();
            final Condition connected = lock.newCondition();
            
            ServerListener listener = new ServerListener.Adapter() {
                @Override
                public void connected(Server _, Connection connection) {
                    if (debug != null) {
                        try {
                            debug.append("debug: " + ServerChannel.this + "  connected to " + connection + "\n");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } 
                    }
                    lock.lock();
                    try {
                        connected.signalAll();
                    } finally {
                        lock.unlock();
                    }
                }
            };

            lock.lock();
            try {
                server.addListener(listener);
                try {
                    if (debug != null) {
                        try {
                            debug.append("debug: " + this + " waiting for connection\n");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } 
                    }
                    connected.await();
                } finally {
                    server.removeListener(listener);
                }
            } finally {
                lock.unlock();
            }
        }
        
        @Override
        public void waitPattern(final String regex, final Appendable debug) throws InterruptedException {
            final Lock lock = new ReentrantLock();
            final Condition connected = lock.newCondition();
            
            ServerListener listener = new ServerListener.Adapter() {
                @Override
                public void connected(Server _, Connection connection) {
                    String remote = connection.getRemoteAddress().toString();
                    if (remote.matches(regex)) {
                        if (debug != null) {
                            try {
                                debug.append("debug: " + ServerChannel.this + " connected to \"" + remote + "\"" +
                                		" matched by \"" + regex + "\"\n");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            } 
                        }
                        lock.lock();
                        try {
                            connected.signalAll();
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        try {
                            debug.append("debug: " + ServerChannel.this + " connected to \"" + remote + "\"" +
                                    " NOT matched with \"" + regex + "\"\n");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } 
                    }
                }
            };

            lock.lock();
            try {
                server.addListener(listener);
                try {
                    if (debug != null) {
                        try {
                            debug.append("debug: " + this + " waiting for connection matched by \"" + regex + "\"\n");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } 
                    }
                    connected.await();
                } finally {
                    server.removeListener(listener);
                }
            } finally {
                lock.unlock();
            }
        }
            
        @Override
        public void sendData(int subchannel, byte[] data) throws IOException {
            if (subchannel == 0) {
                server.sendData(data);
            } else {
                Connection connection = server.getConnections().get(subchannel-1);
                connection.sendData(data);
            }
        }
        
        @Override
        public Server getPeer() {
            return server;
        }

        @Override
        public int getSubchannelCount() {
            return server.getConnections().size();
        }

        @Override
        public void addConnectionListener(ConnectionListener listener) {
            new RuntimeException("not implemented").printStackTrace();
        }
        
        @Override
        public void removeConnectionListener(ConnectionListener listener) {
            new RuntimeException("not implemented").printStackTrace();
        }

        @Override
        public String toString() {
        return "S[" + getName() + ":" + server.getPort() + "]";
        }
    }

//  ############################################################################
    
    private static class ClientChannel extends Channel {
        
        private final Connection client;
        
        protected ClientChannel(String name, Connection client) {
            super(name);
            this.client = client;
        }
        
        @Override
        public void start() {
            client.start();
        }
        
        @Override
        public void stop(int subchannel) throws IOException {
            if (subchannel != 0)
                throw new IllegalArgumentException("No subchannels");
            client.stop();
        }
        
        @Override
        public void abort(int subchannel) throws IOException {
            if (subchannel != 0)
                throw new IllegalArgumentException("No subchannels");
            client.close();
        }
        
        @Override
        public void waitConnect(final Appendable debug) throws InterruptedException {
            final Lock lock = new ReentrantLock();
            final Condition received = lock.newCondition();
            
            ConnectionListener listener = new ConnectionListener.Adapter() {
                @Override
                public void receivedData(Connection connection, byte[] data) {
                    if (debug != null) {
                        try {
                            debug.append("debug: " + ClientChannel.this + " got packet: " + StringHelper.toString(data) + "\n");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } 
                    }
                    lock.lock();
                    try {
                        received.signalAll();
                    } finally {
                        lock.unlock();
                    }
                }
            };

            lock.lock();
            try {
                client.addListener(listener);
                try {
                    if (debug != null) {
                        try {
                            debug.append("debug: " + this + " waiting for packet\n");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } 
                    }
                    received.await();
                } finally {
                    client.removeListener(listener);
                }
            } finally {
                lock.unlock();
            }
        }
        
        @Override
        public void waitPattern(final String regex, final Appendable debug) throws InterruptedException {
            final Lock lock = new ReentrantLock();
            final Condition matched = lock.newCondition();
            
            ConnectionListener listener = new ConnectionListener.Adapter() {
                @Override
                public void receivedData(Connection connection, byte[] data) {
                    String text = new String(data);
                    if (debug != null) {
                        try {
                            debug.append("debug: " + ClientChannel.this + " got data       \"" + text + "\"\n");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } 
                    }
                    lock.lock();
                    try {
                        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
                        if (pattern.matcher(text).matches()) {
                            if (debug != null) {
                                try {
                                    debug.append("debug: " + ClientChannel.this + " matched by     \"" + regex + "\"\n");
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                } 
                            }
                            matched.signalAll();
                        } else {
                            if (debug != null) {
                                try {
                                    debug.append("debug: " + ClientChannel.this + " NOT matched by \"" + regex + "\"\n");
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                } 
                            }
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            };

            lock.lock();
            try {
                client.addListener(listener);
                try {
                    if (debug != null) {
                        try {
                            debug.append("debug: " + this + " waiting for match to \"" + regex + "\"\n");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } 
                    }
                    matched.await();
                } finally {
                    client.removeListener(listener);
                }
            } finally {
                lock.unlock();
            }
        }
        
        @Override
        public void sendData(int subchannel, byte[] data) throws IOException {
            if (subchannel != 0)
                throw new IllegalArgumentException("No subchannels for clients");
            client.sendData(data);
        }
        
        @Override
        public Connection getPeer() {
            return client;
        }

        @Override
        public int getSubchannelCount() {
            return 0;
        }
        
        @Override
        public void addConnectionListener(ConnectionListener listener) {
            client.addListener(listener);
        }
        
        @Override
        public void removeConnectionListener(ConnectionListener listener) {
            client.removeListener(listener);
        }

        @Override
        public String toString() {
            return "C[" + getName() + ":" + client.getRemotePort() + "]";
        }
    }
    
//  ############################################################################
    
    private static class SubchannelServListener extends ServerListener.Adapter {
        
        private final String prefix;
        private int nextNumber;
        
        private SubchannelServListener(String prefix) {
            this.prefix = prefix;
            nextNumber = 1;
        }

        @Override
        public void connected(Server server, Connection connection) {
            String name = prefix + "-" + nextNumber;
            nextNumber++;
            Channel.create(name, connection);  // register
        }
    }
    
//  ############################################################################
    
    private static class ShutServListener extends ServerListener.Adapter {
        @Override
        public void shutdown(Server server) {
            server.removeListener(this);
            String name = findName(server);
            if (name != null) {
                remove(name);
            }
        }
    }
    
//  ############################################################################
    
    private static class ShutConnListener extends ConnectionListener.Adapter {
        @Override
        public void shutdown(Connection connection) {
            connection.removeListener(this);
            String name = findName(connection);
            if (name != null) {
                remove(name);
            }
        }
    }
    
//  ############################################################################
    
    public interface Listener {
        
        public void created(Channel channel, Server server);
        
        public void created(Channel channel, Connection client);
        
        public void removed(Channel channel);
    }
}
