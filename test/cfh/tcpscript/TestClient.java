/**
 * 
 */
package cfh.tcpscript;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import cfh.tcp.Connection;
import cfh.tcp.ConnectionListener;

public class TestClient {
        
        private final Connection connection;
        
        private final List<byte[]> received = new Vector<byte[]>();

        public TestClient(int port) throws IOException {
            connection = new Connection("localhost", port);
            connection.addListener(new ConnListener());
            connection.start();
        }
        
        public Connection getConnection() {
            return connection;
        }
        
        public void stop() throws IOException {
            connection.stop();
        }
        
        public void close() throws IOException {
            connection.close();
        }
        
        public void assertReceived(int count) {
            TestUtilities.sleep();
            synchronized (received) {
                assertEquals(count, received.size());
            }
        }
        
        public List<byte[]> getReceived() {
            synchronized (received) {
                return Collections.unmodifiableList(received);
            }
        }
        
        public byte[] getLastReceived() {
            synchronized (received) {
                if (received.isEmpty())
                    return null;
                else
                    return received.get(received.size()-1);
            }
        }
        
//  ----------------------------------------------------------------------------        
        private class ConnListener extends ConnectionListener.Adapter {
            @Override
            public void receivedData(Connection _, byte[] data) {
                synchronized (received) {
                    received.add(data);
                }
            }
        }
    }