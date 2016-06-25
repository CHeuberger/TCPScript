package cfh.tcpscript;

import java.awt.Component;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cfh.tcp.Connection;
import cfh.tcp.ConnectionListener;
import cfh.tcp.Server;
import cfh.tcp.ServerListener;
import cfh.tcpscript.command.Command;

/**
 * TODO
 * 
 * @author Carlos Heuberger
 * $Revision: 1.8 $
 */
public class ScriptEngine implements Runnable {

    public static String getRevision() {
        return ScriptEngine.class.getName() + " $Revision: 1.8 $";
    }

    private static final List<ScriptEngine> engines = new ArrayList<ScriptEngine>();
    private static final List<Proxy> proxies = new ArrayList<Proxy>();
    
    public static void execute(String name, Reader script, Appendable output, Component parent) {
        if (script == null)
            throw new IllegalArgumentException("script must not be null");
        if (output == null)
            throw new IllegalArgumentException("output must not be null");
        if (parent == null)
            throw new IllegalArgumentException("parent must not be null");

        ScriptEngine engine = new ScriptEngine(name, script, output, parent);
        synchronized (engines) {
            engines.add(engine);
        }
        engine.start();
    }
    
    public static List<ScriptEngine> getEngines() {
        return Collections.unmodifiableList(engines);
    }
    
    public static void stopAll() {
        for (ScriptEngine engine : engines) {
            engine.stop();
        }
    }
    
    public static List<String> getProxyNames() {
        List<String> names = new ArrayList<String>(proxies.size());
        for (Proxy proxy : proxies) {
            names.add(proxy.toString());
        }
        return names;
    }
    
    public static void stopAllProxies() {
        List<Proxy> tmp = new ArrayList<Proxy>(proxies);
        for (Proxy proxy : tmp) {
            proxy.stop();
        }
    }
    
//  ============================================================================
    
    private final String name;
    private final LineNumberReader script;
    private final Appendable output;
    private final Component parent;
    
    private Thread thread = null;
    private String last;
    
    private ScriptEngine(String name, Reader script, Appendable output, Component parent) {
        assert script != null : "null script";
        assert output != null : "null output";
        assert parent != null : "null parent";

        this.name = name;
        if (script instanceof LineNumberReader) {
            this.script = (LineNumberReader) script;
        } else {
            this.script = new LineNumberReader(script);
        }
        this.output = output;
        this.parent = parent;
    }
    
    public String getName() {
        return name;
    }
    
    public Component getParent() {
        return parent;
    }
    
    public String getLast() {
        return last;
    }

    public Appendable getOutput() {
        return output;
    }
    
    private synchronized void start() {
        if (thread != null)
            throw new IllegalStateException("already running");
        
        thread = new Thread(this);
        thread.setName("ScriptExecutor: " + name);
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY - 2);
        thread.start();
    }
    
    public synchronized void stop() {
        Thread tmp = thread;
        thread = null;
        tmp.interrupt();
    }
    
    public void run() {
        String line;
        try {
            while (thread == Thread.currentThread() && (line = script.readLine()) != null) {
                if (line.trim().length() == 0 
                        || line.startsWith("//")
                        || line.startsWith("#") ) {
                    continue;
                }
                String[] words = line.split("\\s++", 2);
                if (words.length == 0 && words[0].length() == 0) {
                    continue;
                }
                last = (script.getLineNumber()-1) + ":" + line;
                String cmd = words[0];
                String arg = (words.length >= 2) ? words[1] : "";
                
                try {
                    Command command = Command.get(cmd);
                    if (command != null) {
                        command.run(this, arg);
                        continue;
                    }
                    throw new ParseException("unrecognized command '" + words[0] + "'", 
                            script.getLineNumber());
                } catch (Exception ex) {
                    handle(ex);
                    println("\t at " + script.getLineNumber() + ":'" + line + "'");
                }
            }
        } catch (IOException ex) {
            handle(ex);
        } finally {
            thread = null;
            synchronized (engines) {
                engines.remove(this);
            }
            try {
                script.close();
            } catch (IOException ex) {
                handle(ex);
            }
        }
    }
    
    public ServerListener getLogServListener(String name, int port) {
        return new LogServListener(String.format("S[%s:%d]", name, port));
    }
    
    public ConnectionListener getLogConnListener(String name, int port) {
        return new LogConnListener(String.format("C[%s:%d]", name, port));
    }
    
    public void println(String msg) {
        try {
            output.append(String.format("%tT.%1$tL  %s\n", System.currentTimeMillis(), msg));
        } catch (IOException ex) {
            System.out.println(msg);
            ex.printStackTrace();
        }
    }
    
    public void handle(Throwable ex) {
        ex.printStackTrace();
        println("######### " + ex);
    }
    
    public void startProxy(Channel channel1, Channel channel2, boolean bidirectional) {
        if (channel1 == null)
            throw new IllegalArgumentException("null channel1");
        if (channel2 == null)
            throw new IllegalArgumentException("null channel2");
        
        Proxy proxy = new Proxy(this, channel1, channel2, bidirectional);
        synchronized (proxies) {
            proxies.add(proxy);
        }
        proxy.start();
    }
    
    public void stopProxy(Channel channel1, Channel channel2, boolean bidirectional) {
        if (channel1 == null)
            throw new IllegalArgumentException("null channel1");
        if (channel2 == null)
            throw new IllegalArgumentException("null channel2");
        
        List<Proxy> selected = new ArrayList<Proxy>();
        synchronized (proxies) {
            for (Proxy proxy : proxies) {
                if (proxy.getChannel1().equals(channel1) && 
                        proxy.getChannel2().equals(channel2) && 
                        proxy.isBidirectional() == bidirectional) {
                    selected.add(proxy);
                }
            }
        }
        for (Proxy proxy : selected) {
            proxy.stop();
        }
    }
    
    public void terminatedProxy(Proxy proxy) {
        synchronized (proxies) {
            proxies.remove(proxy);
        }
    }
    
//  ############################################################################
    
    private class LogServListener implements ServerListener {
        private final String prefix;
        private final LogConnListener logConListener;
        
        protected LogServListener(String prefix) {
            this.prefix = prefix;
            logConListener = new LogConnListener(prefix);
        }
        
        public void started(Server server) {
            println(prefix + " started server");
        }
        
        public void connected(Server server, Connection connection) {
            connection.addListener(logConListener);
        }
        
        public void shutdown(Server server) {
            println(prefix + " server shut down");
        }

        public void handleException(Server server, Exception ex) {
            println(prefix + " ERROR: " + ex);
        }
    }
    
//  ############################################################################
    
    private class LogConnListener implements ConnectionListener {
        
        private final String prefix;
        
        public LogConnListener(String prefix) {
            this.prefix = prefix;
        }

        public void started(Connection connection) {
            println(prefix + "\u21AD" + "started connection " + connection.getRemoteAddress());
        }

        public void sentData(Connection connection, byte[] data) {
            println(prefix + "\u21A4" + StringHelper.toString(data));
        }

        public void receivedData(Connection connection, byte[] data) {
            println(prefix + "\u21E5" + StringHelper.toString(data));
        }

        public void shutdown(Connection connection) {
            println(prefix + "\u2022" + "shutdown connection " + connection.getRemoteAddress());
        }

        public void handleException(Connection connection, Exception ex) {
            println(prefix + " ERROR: " + ex);
        }
    }
}
