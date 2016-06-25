package cfh.tcpscript;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.CaretListener;

import cfh.tcp.Connection;
import cfh.tcp.ConnectionListener;


@SuppressWarnings("serial")
public class MonitorPanel extends JPanel {

    private final Channel channel;
    private final Connection connection;
    
    private JLabel status;
    private JButton close;
    
    private JScrollPane scroll;
    private JTextArea area;
    
    private JTextField enter;
    private JLabel columnStatus;

    private final ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void started(Connection connection) {
        }
        @Override
        public void shutdown(Connection connection) {
            status.setText("disconnected " + connection.getRemoteAddress());
            status.setForeground(Color.RED);
            append("disconnected " + connection.getRemoteAddress());
            enter.setEditable(false);
        }
        @Override
        public void sentData(Connection connection, byte[] data) {
            appendOutput(data);
        }
        @Override
        public void receivedData(Connection connection, byte[] data) {
            appendInput(data);
        }
        @Override
        public void handleException(Connection connection, Exception ex) {
            append("ERROR: " + ex);
        }
    };
    
    MonitorPanel(Channel channel, Connection connection) {
        assert channel != null;
        assert connection != null;
    
        this.channel = channel;
        this.connection = connection;
        
        initGUI();
    }

    private void initGUI() {
        status = new JLabel();
        status.setText("connected " + connection.getRemoteAddress());
        status.setForeground(Color.GREEN.darker());
        
        close = new JButton(new AbstractAction("Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                getParent().remove(MonitorPanel.this);
            }
        });
        close.setEnabled(false);
        close.setMargin(new Insets(0, 0, 0, 0));
        close.setBorderPainted(false);
        
        Box buttons = Box.createHorizontalBox();
        buttons.add(status);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(close);
        
        area = new JTextArea();
        area.setEditable(false);
        area.setFont(Main.FONT);
        
        scroll = new JScrollPane(area);
        
        enter = new JTextField();
        enter.setFont(Main.FONT);
        enter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                String text = enter.getText();
                enter.selectAll();
                try {
                    channel.sendData(0, text);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(MonitorPanel.this, ex);
                }
            }
        });
        
        columnStatus = new JLabel("123456789012345678901234567890");
        columnStatus.setBorder(BorderFactory.createLoweredBevelBorder());
        columnStatus.setPreferredSize(columnStatus.getPreferredSize());
        columnStatus.setText("");
        columnStatus.setHorizontalAlignment(JLabel.CENTER);
        CaretListener caretListener = new ColumnIndicator(columnStatus);
        area.addCaretListener(caretListener);
        enter.addCaretListener(caretListener);
        
        JPanel enterPanel = new JPanel(new BorderLayout());
        enterPanel.add(enter, BorderLayout.CENTER);
        enterPanel.add(columnStatus, BorderLayout.AFTER_LINE_ENDS);
        
        setLayout(new BorderLayout());
        add(buttons, BorderLayout.BEFORE_FIRST_LINE);
        add(scroll, BorderLayout.CENTER);
        add(enterPanel, BorderLayout.AFTER_LAST_LINE);
        
        connection.addListener(connectionListener);
        
        append("connected " + connection.getRemoteAddress());
    }
    
    void unregister() {
        connection.removeListener(connectionListener);
        close.setEnabled(true);
    }
    
    public String getChannelName() {
        return channel.getName();
    }
    
    private void appendInput(byte[] data) {
        append("IN  " + StringHelper.toString(data) + " " + StringHelper.toHexString(data));
    }
    
    private void appendOutput(byte[] data) {
        append("OUT " + StringHelper.toString(data) + " " + StringHelper.toHexString(data));
    }
    
    private synchronized void append(String msg) {
        area.append(String.format("%tT.%1$tL  %s%n", System.currentTimeMillis(), msg));
        JScrollBar bar = scroll.getVerticalScrollBar();
        bar.setValue(bar.getMaximum());
        area.setCaretPosition(area.getDocument().getLength());
    }
}
