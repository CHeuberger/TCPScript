package cfh.tcpscript;

import java.io.IOException;

import cfh.tcp.Connection;
import cfh.tcp.ConnectionListener;

public class Proxy {

    private final ScriptEngine executor;
    private final Channel channel1;
    private final Channel channel2;
    private final boolean bidirectional;
    
    private final ConnectionListener listener1 = new ConnectionListener.Adapter() {
        @Override
        public void shutdown(Connection connection) {
            stop();
        }
        @Override
        public void receivedData(Connection connection, byte[] data) {
            try {
                channel2.sendData(0, data);
            } catch (IOException ex) {
                executor.handle(ex);
            }
        }
    };
    
    private final ConnectionListener listener2 = new ConnectionListener.Adapter() {
        @Override
        public void shutdown(Connection connection) {
            stop();
        }
        @Override
        public void receivedData(Connection connection, byte[] data) {
            if (bidirectional) {
                try {
                    channel1.sendData(0, data);
                } catch (IOException ex) {
                    executor.handle(ex);
                }
            }
        }
    };

    
    Proxy(ScriptEngine executor, Channel channel1, Channel channel2, boolean bidirectional) {
        assert executor != null : "null executor";
        assert channel1 != null : "null channel1";
        assert channel2 != null : "null channel2";
        
        this.executor = executor;
        this.channel1 = channel1;
        this.channel2 = channel2;
        this.bidirectional = bidirectional;
    }
    
    void start() {
        channel1.addConnectionListener(listener1);
        channel2.addConnectionListener(listener2);
        executor.println(String.format("%s%s%s", channel1, bidirectional ? "\u21C4" : "\u21B7", channel2));
    }
    
    void stop() {
        channel1.removeConnectionListener(listener1);
        channel2.removeConnectionListener(listener2);
        executor.terminatedProxy(this);
        executor.println(String.format("%s%s%s", channel1, bidirectional ? "\u2226" : "\u2224", channel2));
    }
    
    public Channel getChannel1() {
        return channel1;
    }
    
    public Channel getChannel2() {
        return channel2;
    }
    
    public boolean isBidirectional() {
        return bidirectional;
    }
    
    @Override
    public String toString() {
        return String.format("%s%s%s", channel1, bidirectional ? "\u21C4" : "\u21B7", channel2);
    }
}
