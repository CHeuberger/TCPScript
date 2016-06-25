package cfh.tcpscript;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import cfh.tcp.Connection;
import cfh.tcp.Server;


public class Monitor {

    public static String getRevision() {
        return Monitor.class.getName() + " $Revision: 1.4 $";
    }

    private final Preferences pref = Preferences.userNodeForPackage(getClass());
    private final String PREF_FRAME_POSTION_X = "frame_location_x";
    private final String PREF_FRAME_POSTION_Y = "frame_location_y";
    private final String PREF_FRAME_SIZE_W = "frame_size_w";
    private final String PREF_FRAME_SIZE_H = "frame_size_h";

    private JFrame frame;
    private JTabbedPane tabs;
    private Map<Channel, MonitorPanel> monitors = new HashMap<Channel, MonitorPanel>();

    private Point framePosition = null;
    private Dimension frameSize = null;

    private Channel.Listener channelListener = new Channel.Listener() {
        @Override
        public void created(Channel channel, Connection client) {
            doCreate(channel, client);
        }
        @Override
        public void created(Channel channel, Server server) {
        }
        @Override
        public void removed(Channel channel) {
            doRemove(channel);
        }
    };

    Monitor(JFrame parent) {
        initPreferences();
        initGUI();
    }

    void dispose() {
        doClose();
    }

    private void initGUI() {
        tabs = new JTabbedPane();

        frame = new JFrame(Main.TITLE + " - Monitor");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                doClose();
            }
        });
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                frameSize = frame.getSize();
                savePreferences();
            }
            @Override
            public void componentMoved(ComponentEvent e) {
                framePosition = frame.getLocation();
                savePreferences();
            }
        });
        frame.add(tabs);
        frame.setSize(frameSize);
        frame.validate();
        frame.setLocation(framePosition);
        frame.setVisible(true);

        Channel.addListener(channelListener);
    }

    private void doCreate(final Channel channel, final Connection client) {
        if (monitors.containsKey(channel)) {
            doRemove(channel);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MonitorPanel panel = new MonitorPanel(channel, client);
                monitors.put(channel, panel);
                tabs.addTab(channel.getName(), panel);
                tabs.setSelectedComponent(panel);
            }
        });
    }

    private void doRemove(final Channel channel) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MonitorPanel panel = monitors.remove(channel);
                if (panel != null) {
                    panel.unregister();
                    int index = tabs.indexOfComponent(panel);
                    if (index != -1) {
                        tabs.setTitleAt(index, "[" + panel.getChannelName() + "]");
                    }
                }
            }
        });
    }

    private void doClose() {
        closeTabs(true);
        frame.dispose();
    }

    public void toggleVisible() {
        boolean normal = (frame.getExtendedState() & JFrame.ICONIFIED) == 0;
        if (normal && frame.isVisible()) {
            frame.setVisible(false);
        } else {
            frame.setExtendedState(frame.getExtendedState() & ~JFrame.ICONIFIED);
            frame.setVisible(true);
        }
    }

    public void closeTabs(boolean force) {
        if (force) {
            for (Channel channel : monitors.keySet()) {
                doRemove(channel); 
            }
            tabs.removeAll();
        } else {
            for (int i = tabs.getTabCount()-1; i >= 0; i -= 1) {
                Component comp = tabs.getComponentAt(i);
                if (!monitors.containsValue(comp)) {
                    tabs.removeTabAt(i);
                }
            }
        }
        tabs.revalidate();
    }

    private void initPreferences() {
        int w = pref.getInt(PREF_FRAME_SIZE_W, 800);
        int h = pref.getInt(PREF_FRAME_SIZE_H, 600);
        if (w < 300) w = 300;
        if (h < 300) h = 300;
        frameSize = new Dimension(w, h);

        int x = pref.getInt(PREF_FRAME_POSTION_X, 40);
        int y = pref.getInt(PREF_FRAME_POSTION_Y, 40);        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (x+w < 100) x = 100 - w;
        else if (x > screenSize.width-100) x = screenSize.width - 100;
        if (y+h < 100) y = 100 - h;
        else if (y >= screenSize.height-100) y = screenSize.height - 100;
        framePosition = new Point(x, y);
    }

    private void savePreferences() {
        pref.putInt(PREF_FRAME_POSTION_X, framePosition.x);
        pref.putInt(PREF_FRAME_POSTION_Y, framePosition.y);
        pref.putInt(PREF_FRAME_SIZE_W, frameSize.width);
        pref.putInt(PREF_FRAME_SIZE_H, frameSize.height);
    }
}
