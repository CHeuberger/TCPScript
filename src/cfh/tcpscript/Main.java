package cfh.tcpscript;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Keymap;

import cfh.tcp.Connection;
import cfh.tcp.Server;
import cfh.tcpscript.command.Command;

/**
 * TODO
 *
 * @author Carlos Heuberger
 * $Revision: 1.26 $
 */
public class Main implements Appendable {

    private static final String VERSION;
    static {
        String v = Main.class.getPackage().getImplementationVersion();
        VERSION = v == null ? "dev" : "v " + v;
    }

    private static final String AUTHOR = "by Carlos Heuberger - " + VERSION;
    static final String TITLE = "TCP Script - " + VERSION;

    static final Font FONT = new Font("monospaced", Font.PLAIN, 12);

    private static final Preferences pref = Preferences.userNodeForPackage(Main.class);
    private static final String PREF_HELP_SEEN = "help_seen";
    private static final String PREF_DEF_DIRECTORY = "default_directory";
    private static final String PREF_DEF_FILE = "default_file";
    private static final String PREF_MAIN_POSTION_X = "main_location_x";
    private static final String PREF_MAIN_POSTION_Y = "main_location_y";
    private static final String PREF_MAIN_SIZE_W = "main_size_w";
    private static final String PREF_MAIN_SIZE_H = "main_size_h";

    public static Main main;

    public static void main(String[] args) {
        main = new Main();
    }

//  ============================================================================

    private JFrame mainFrame;

    private Box buttons;

    private JTextArea input;

    private JLabel statusName;
    private JLabel statusOther;
    private JLabel statusColumn;

    private JScrollPane outScroll;
    private JTextArea output;
    private JPanel status;

    private JCheckBox clearInput;
    private JCheckBox clearOutput;
    private JCheckBox closeMonitor;
    private JCheckBox closeChannel;
    private JCheckBox stopScripts;
    private JCheckBox stopProxies;

    private Action executeAction;
    private Action loadAction;
    private Action saveAction;
    private Action clearAction;
    private Action monitorAction;
    private Action helpAction;
    private Action quitAction;

    private boolean helpSeen;
    private File defDirectory;
    private Point mainPosition = null;
    private Dimension mainSize = null;

    private boolean changed = false;
    private File actualFile = null;

    private Monitor monitor;

    private Main() {
        initPreferences();
        initActions();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initGUI();
            }
        });
    }

    @Override
    public Appendable append(CharSequence seq) {
        append0(seq.toString());
        return this;
    }

    @Override
    public Appendable append(CharSequence seq, int start, int end) {
        return append(seq.subSequence(start, end));
    }

    @Override
    public Appendable append(char ch) {
        append0(Character.toString(ch));
        return this;
    }

    private void initPreferences() {
        helpSeen = pref.getBoolean(PREF_HELP_SEEN, false);
        String dir = pref.get(PREF_DEF_DIRECTORY, null);
        if (dir != null) {
            defDirectory = new File(dir);
        }
        String file = pref.get(PREF_DEF_FILE, null);
        if (file != null) {
            actualFile = new File(file);
        }

        int w = pref.getInt(PREF_MAIN_SIZE_W, 800);
        int h = pref.getInt(PREF_MAIN_SIZE_H, 600);
        if (w < 300) w = 300;
        if (h < 300) h = 300;
        mainSize = new Dimension(w, h);

        int x = pref.getInt(PREF_MAIN_POSTION_X, 40);
        int y = pref.getInt(PREF_MAIN_POSTION_Y, 40);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (x+w < 100) x = 100 - w;
        else if (x > screenSize.width-100) x = screenSize.width - 100;
        if (y+h < 100) y = 100 - h;
        else if (y >= screenSize.height-100) y = screenSize.height - 100;
        mainPosition = new Point(x, y);
    }

    private void savePreferences() {
        pref.putBoolean(PREF_HELP_SEEN, helpSeen);
        if (defDirectory != null) {
            pref.put(PREF_DEF_DIRECTORY, defDirectory.getAbsolutePath());
        }
        if (actualFile != null) {
            pref.put(PREF_DEF_FILE, actualFile.getAbsolutePath());
        }
        pref.putInt(PREF_MAIN_POSTION_X, mainPosition.x);
        pref.putInt(PREF_MAIN_POSTION_Y, mainPosition.y);
        pref.putInt(PREF_MAIN_SIZE_W, mainSize.width);
        pref.putInt(PREF_MAIN_SIZE_H, mainSize.height);
    }

    @SuppressWarnings("serial")
    private void initActions() {
        loadAction = new AbstractAction("Load"){
            {
                putValue(Action.SHORT_DESCRIPTION, "load script, SHIFT: load into selection");
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                doLoad(e.getModifiers());
            }
        };
        saveAction = new AbstractAction("Save"){
            {
                putValue(Action.SHORT_DESCRIPTION, "save script, SHIFT: save selected text");
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                doSave(e.getModifiers());
            }
        };
        executeAction = new AbstractAction("Execute") {
            {
                putValue(Action.SHORT_DESCRIPTION, "execute (selected) text, SHIFT: execute actual line");
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                doExecute(e.getModifiers());
            }
        };
        clearAction = new AbstractAction("Clear") {
            @Override
            public void actionPerformed(ActionEvent e) {
                doClear();
            }
        };
        monitorAction = new AbstractAction("Monitor") {
            @Override
            public void actionPerformed(ActionEvent e) {
                doMonitor();
            }
        };
        helpAction = new AbstractAction("Help") {
            @Override
            public void actionPerformed(ActionEvent e) {
                doHelp();
            }
        };
        quitAction = new AbstractAction("Quit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                doQuit();
            }
        };
    }

    private void initGUI() {
        monitor = new Monitor(mainFrame);

        clearInput = new JCheckBox("input area");
        clearOutput = new JCheckBox("output area");
        closeMonitor = new JCheckBox("closed monitor tabs");
        closeChannel = new JCheckBox("close all connectins");
        stopScripts = new JCheckBox("stop scripts");
        stopProxies = new JCheckBox("terminate proxies");

        buttons = Box.createHorizontalBox();

        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(createButton(loadAction));
        buttons.add(createButton(saveAction));
        buttons.add(Box.createHorizontalStrut(20));
        buttons.add(createButton(executeAction));
        buttons.add(Box.createHorizontalStrut(20));
        buttons.add(createButton(clearAction));
        buttons.add(Box.createHorizontalGlue());
        buttons.add(createButton(monitorAction));
        buttons.add(Box.createHorizontalGlue());
        buttons.add(createButton(quitAction));
        buttons.add(Box.createHorizontalStrut(40));
        buttons.add(createButton(helpAction));
        buttons.add(Box.createHorizontalStrut(10));

        input = new JTextArea();
        input.setFont(FONT);
        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                changed(true);
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                changed(true);
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                changed(true);
            }
        });
        Keymap keymap = input.getKeymap();
        keymap.addActionForKeyStroke(
                KeyStroke.getKeyStroke("control ENTER"), executeAction);
        keymap.addActionForKeyStroke(
                KeyStroke.getKeyStroke("shift ENTER"), executeAction);
        keymap.addActionForKeyStroke(
                KeyStroke.getKeyStroke("shift control ENTER"), executeAction);

        output = new JTextArea();
        output.setEditable(false);
        output.setFont(FONT);

        outScroll = new JScrollPane(output);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setTopComponent(new JScrollPane(input));
        split.setBottomComponent(outScroll);
        split.setOneTouchExpandable(true);
        split.setResizeWeight(0.0);

        statusName = createStatusLabel("123456789012345678901234567890", AUTHOR);
        statusOther = createStatusLabel("", "");
        statusColumn = createStatusLabel("123456789012345678901234567890", " ");
        CaretListener caretListener = new ColumnIndicator(statusColumn);
        input.addCaretListener(caretListener);
        output.addCaretListener(caretListener);

        status = new JPanel(new BorderLayout());
        status.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        status.add(statusName, BorderLayout.BEFORE_LINE_BEGINS);
        status.add(statusOther, BorderLayout.CENTER);
        status.add(statusColumn, BorderLayout.AFTER_LINE_ENDS);

        mainFrame = new JFrame(TITLE);
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                doQuit();
            }
        });
        mainFrame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                mainSize = mainFrame.getSize();
                savePreferences();
            }
            @Override
            public void componentMoved(ComponentEvent e) {
                mainPosition = mainFrame.getLocation();
                savePreferences();
            }
        });
        mainFrame.setLayout(new BorderLayout());
        mainFrame.add(buttons, BorderLayout.NORTH);
        mainFrame.add(split, BorderLayout.CENTER);
        mainFrame.add(status, BorderLayout.SOUTH);
        mainFrame.setSize(mainSize);
        mainFrame.validate();
        mainFrame.setLocation(mainPosition);
        mainFrame.setVisible(true);

        split.setDividerLocation(0.7);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                input.requestFocus();
            }
        });
    }

    private JButton createButton(Action action) {
        JButton button = new JButton(action);
        button.setMargin(new Insets(2, 4, 2, 4));
        return button;
    }

    private JLabel createStatusLabel(String template, String initial) {
        JLabel label = new JLabel(template);
        label.setBorder(BorderFactory.createLoweredBevelBorder());
        label.setPreferredSize(label.getPreferredSize());
        label.setText(initial);
        label.setHorizontalAlignment(JLabel.CENTER);
        return label;
    }

    private void doExecute(int modifiers) {
        String script;
        String name;
        if ((modifiers & InputEvent.SHIFT_MASK) != 0) {
            String text = input.getText();
            int pos = input.getCaretPosition();
            int start;
            int end;
            try {
                int line = input.getLineOfOffset(pos);
                name = "Line:" + line;
                start = input.getLineStartOffset(line);
                end = input.getLineEndOffset(line);
            } catch (BadLocationException ex) {
                handle(ex);
                JOptionPane.showMessageDialog(mainFrame, ex);
                return;
            }
            script = text.substring(start, end);
            if ((modifiers & InputEvent.CTRL_MASK) != 0) {
                char ch = (end > start) ? text.charAt(end - 1) : '\0';
                if (ch != '\n' && ch != '\r') {
                    input.append("\n");
                    end++;
                }
            }
            input.setCaretPosition(end);
        } else {
            script = input.getSelectedText();
            if (script == null) {
                script = input.getText();
                name = "Input";
            } else {
                int start = input.getSelectionStart();
                try {
                    int line = input.getLineOfOffset(start);
                    name = "Selected:" + line;
                } catch (BadLocationException ex) {
                    handle(ex);
                    JOptionPane.showMessageDialog(mainFrame, ex);
                    return;
                }
            }
        }
        if (script != null) {
            StringReader reader = new StringReader(script);
            ScriptEngine.execute(name, reader, main, mainFrame);
        }
    }

    private void doLoad(int modifiers) {
        JFileChooser chooser = new JFileChooser();
        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Textfiles (*.txt)", "txt"));
        chooser.setDialogTitle("Load Script");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (actualFile != null) {
            chooser.setSelectedFile(actualFile);
        } else {
            chooser.setCurrentDirectory(defDirectory);
        }
        int option = chooser.showOpenDialog(mainFrame);
        if (option == JFileChooser.APPROVE_OPTION) {
            defDirectory = chooser.getCurrentDirectory();
            actualFile = chooser.getSelectedFile();
            savePreferences();
            try {
                boolean overwrite = false;
                BufferedReader in = new BufferedReader(new FileReader(actualFile));
                try {
                    if ((modifiers & ActionEvent.SHIFT_MASK) == 0) {
                        overwrite = true;
                        if (changed) {
                            int reply = JOptionPane.showConfirmDialog(
                                    mainFrame,
                                    "Text changed, overwrite?",
                                    "Confirm",
                                    JOptionPane.OK_CANCEL_OPTION);
                            if (reply != JOptionPane.OK_OPTION)
                                return;
                            overwrite = true;
                        }
                        if (overwrite) {
                            input.setText(null);
                        }
                    } else {
                        String selected = input.getSelectedText();
                        if (selected != null && !selected.isEmpty()) {
                            int reply = JOptionPane.showConfirmDialog(
                                    mainFrame,
                                    "Test changed, substitute selection?",
                                    "Confirm",
                                    JOptionPane.OK_CANCEL_OPTION);
                            if (reply != JOptionPane.OK_OPTION)
                                return;
                        }
                    }
                    String line;
                    while ( (line = in.readLine()) != null) {
                        input.replaceSelection(line + "\n");
                    }
                } finally {
                    in.close();
                }
                changed(!overwrite);
            } catch (IOException ex) {
                handle(ex);
                JOptionPane.showMessageDialog(mainFrame, ex);
            }
            updateFileName();
        }
    }

    private void doSave(int modifiers) {
        boolean selected = (modifiers & ActionEvent.SHIFT_MASK) != 0;
        String text = (selected) ?  input.getSelectedText() : input.getText();
        if (text == null || text.trim().length() == 0) {
            JOptionPane.showMessageDialog(mainFrame, "nothing to save");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Textfiles (*.txt)", "txt"));
        chooser.setDialogTitle("Save Script");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (actualFile != null) {
            chooser.setSelectedFile(actualFile);
        } else {
            chooser.setCurrentDirectory(defDirectory);
        }
        int option = chooser.showSaveDialog(mainFrame);
        if (option == JFileChooser.APPROVE_OPTION) {
            defDirectory =chooser.getCurrentDirectory();
            File file = chooser.getSelectedFile();
            if (file.exists()) {
                int confirm = JOptionPane.showConfirmDialog(
                        mainFrame,
                        new Object[] {"Do you realy want to overwrite the file?", file},
                        "Confrim Overwrite",
                        JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION)
                    return;
            }
            if (!selected) {
                actualFile = file;
            }
            savePreferences();

            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(file));
                try {
                    out.write(text);
                } finally {
                    out.close();
                }
                changed(false);
            } catch (IOException ex) {
                handle(ex);
                JOptionPane.showMessageDialog(mainFrame, ex);
            }
            updateFileName();
        }
    }

    private void doClear() {
        int option = JOptionPane.showConfirmDialog(
                mainFrame,
                new JCheckBox[] { clearInput, clearOutput, closeMonitor, closeChannel, stopScripts, stopProxies },
                "Clear",
                JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            if (stopScripts.isSelected()) {
                ScriptEngine.stopAll();
            }
            if (stopProxies.isSelected()) {
                ScriptEngine.stopAllProxies();
            }
            if (closeChannel.isSelected()) {
                Set<String> names = new HashSet<String>(Channel.getChannelNames());
                for (String name : names) {
                    try {
                        Channel channel = Channel.get(name);
                        if (channel != null) {
                            channel.stop(0);
                        }
                    } catch (IOException ex) {
                        handle(ex);
                        append("        stopping " + name);
                    }
                    Channel.remove(name);
                }
            }
            if (clearInput.isSelected()) {
                if (changed) {
                    int reply = JOptionPane.showConfirmDialog(
                            mainFrame,
                            "Text changed, clear?",
                            "Confirm",
                            JOptionPane.OK_CANCEL_OPTION);
                    if (reply == JOptionPane.YES_OPTION) {
                        input.setText(null);
                        actualFile = null;
                        changed(false);
                    }
                } else {
                    input.setText(null);
                    actualFile = null;
                    changed(false);
                }
            }
            if (clearOutput.isSelected()) {
                output.setText(null);
            }
            if (closeMonitor.isSelected()) {
                monitor.closeTabs(false);
            }
        }
    }

    private void doMonitor() {
        monitor.toggleVisible();
    }

    private void doQuit() {
        boolean connected = !Channel.getChannelNames().isEmpty();
        if (changed || connected) {
            String message = changed ? "Text changed. " : "";
            if (connected) {
                message += "There are open channels. ";
            }
            message += "Quit?";
            int reply = JOptionPane.showConfirmDialog(
                    mainFrame,
                    message,
                    "Confirm",
                    JOptionPane.OK_CANCEL_OPTION);
            if (reply != JOptionPane.YES_OPTION)
                return;
        }

        TimerTask terminator = new TimerTask() {
            @Override
            public void run() {
                System.out.println("terminated");
                System.exit(1);
            }
        };
        Timer timer = new Timer("terminator", true);
        timer.schedule(terminator, 5000L);

        savePreferences();
        Set<String> names = new HashSet<String>(Channel.getChannelNames());
        for (String name : names) {
            try {
                Channel channel = Channel.get(name);
                if (channel != null) {
                    channel.stop(0);
                }
            } catch (IOException ex) {
                handle(ex);
                append("        closing " + name);
                timer.cancel();
            }
        }
        monitor.dispose();
        mainFrame.dispose();
    }

    private void doHelp() {
        JTextArea help = new JTextArea(32, 90);
        help.setEditable(false);
        help.setFont(FONT);

        listAbout(help);
        listShortcuts(help);
        listCommands(help);
        listChannels(help);
        listStatus(help);
        if (!helpSeen) {
            help.setCaretPosition(0);
            helpSeen = true;
            savePreferences();
        }
        JOptionPane.showMessageDialog(mainFrame, new JScrollPane(help));
    }

    private void listAbout(JTextArea text) {
        String v = Connection.class.getPackage().getImplementationVersion();
        text.append(
                "====================  TCP SCRIPT  by Carlos Heuberger - " + VERSION + "\n" +
                "A program to execute scripts for openning TCP connections to\n" +
                "send and receive data packets through them.\n" +
                "Usefull to help testing communications software where repetitive\n" +
                "sending of data packets is needed.\n" +
                "\n" +
                "Server: " + (v==null ? "dev" : "v " + v) + "\n" +
                "\n\n");
    }

    private void listShortcuts(JTextArea text) {
        text.append(
                "====================  GUI  SHORTCUTS  ====================\n" +
                "CTRL-Enter   execute selected text, or whole text if nothing selected\n" +
                "SHIFT-Enter  execute current line and moves to next line\n" +
                "\n" +
                "SHIFT-LOAD   load into selection\n" +
                "SHIFT-SAVE   save selected text\n" +
                "\n\n");
    }

    private void listChannels(JTextArea text) {
        Set<String> names = Channel.getChannelNames();

        int max = 7;
        for (String name : names) {
            if (name.length() > max)
                max = name.length();
        }
        String format = "%c\u2502%-" + max + "s\u2502%5.5s\u2502%-10.10s\u2502%-10.10s\u2502%s\n";
        text.append("====================  CHANNEL  LIST  ====================\n");
        text.append(String.format(format, 'T', "CHANNEL", "PORT", "HEADER", "FOOTER", "REMOTE"));

        for (String name : names) {
            Channel channel = Channel.get(name);
            Object peer = channel.getPeer();
            char type = ' ';
            String port;
            String info;
            if (peer instanceof Server) {
                Server server = (Server) peer;
                type = 'S';
                port = Integer.toString(server.getPort());
                info = "";
            } else if (peer instanceof Connection) {
                Connection connection = (Connection) peer;
                type = 'C';
                port = Integer.toString(connection.getLocalPort());
                info = connection.getRemoteAddress().toString();
            } else {
                type = '?';
                port = "";
                info = peer.getClass().getName();
            }
            String header = channel.getHeader();
            if (header == null) {
                header = "";
            }
            String footer = channel.getFooter();
            if (footer == null) {
                footer = "";
            }
            text.append(String.format(format, type, name, port, header, footer, info));
        }
        text.append("\n\n");
    }

    private void listStatus(JTextArea text) {
    	text.append(
//    			"====================  STATUS  ====================\n" +
    			"Charset:  " + Charset.defaultCharset().displayName() + "\n");
    	List<ScriptEngine> engines = ScriptEngine.getEngines();
        if (!engines.isEmpty()) {
            text.append("Scripts:  ");
            boolean first = true;
            for (ScriptEngine engine : engines) {
                if (first) {
                    first = false;
                } else {
                    text.append("          ");
                }
                text.append(engine.getName() + " [" + engine.getLast() + "]\n");
            }
        }

    	List<String> proxies = ScriptEngine.getProxyNames();
    	if (!proxies.isEmpty()) {
            text.append("Proxies:  ");
            boolean first = true;
            for (String name : proxies) {
                if (first) {
                    first = false;
                } else {
                    text.append("          ");
                }
                text.append(name + "\n");
            }
    	}
        text.append("\n\n");
    }

    private void listCommands(JTextArea text) {
        Set<String> names = Command.getNames();

        int maxc = 7;  // command
        int maxu = 5;  // usage
        for (String name : names) {
            if (name.length() > maxc)
                maxc = name.length();
            String usage = Command.get(name).getUsage();
            if (usage.length() > maxu)
                maxu = usage.length();
        }
        String format = "%-" + maxc + "s %-" + maxu + "s\u2502%s\n";
        text.append("====================  SCRIPT  COMMANDS  ====================\n");
        text.append(String.format(format, "COMMAND", "PARAMETERS", "HELP"));

        for (String name : names) {
            Command cmd = Command.get(name);
            text.append(String.format(format, name, cmd.getUsage(), cmd.getDecription()));
        }
        text.append("\n");
        text.append(
                "<name>  channel name choosen at creation (listen, connect).\n" +
        	"        The name must not contain whitespaces or hyphens ('-').\n" +
        	"        The hyphen '-' is used to specify one connection of a server.\n" +
        	"        Example: if the server got the name 'srv' (\"listen srv 1234\"),\n" +
        	"                 the second connection will be 'srv-2' (send or close).\n" +
        	"                 'srv' can be used for all connections from that server.\n" +
        	"\n" +
        	"<header><footer>  data sent at the start/end of each data packet.\n" +
        	"                  For the server, header and footer are only used when sending\n" +
        	"                  to the server itself. Each connection has a own set of them.\n" +
        	"\n\n");
    }

    private void handle(Throwable ex) {
      ex.printStackTrace();
      append("####### Exception: " + ex + "\n");
      for (Throwable t = ex.getCause(); t != null; t = t.getCause()) {
          append("    Cause:     " + t + "\n");
      }
    }

    private synchronized void append0(final String str) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                output.append(str);
                JScrollBar bar = outScroll.getVerticalScrollBar();
                bar.setValue(bar.getMaximum());
                output.setCaretPosition(output.getDocument().getLength());
            }
        });
    }

    private void changed(boolean value) {
        changed = value && ! input.getText().isEmpty();
        updateFileName();
    }

    private void updateFileName() {
        if (actualFile == null || input.getText().isEmpty()) {
            statusName.setText(AUTHOR);
        } else {
            String name = actualFile.getName();
            if (changed) {
                name += " *";
            }
            statusName.setText(name);
        }
    }
}
