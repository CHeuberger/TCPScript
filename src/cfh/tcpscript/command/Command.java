package cfh.tcpscript.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import cfh.tcpscript.ScriptEngine;

/**
 * TODO
 * 
 * @author Carlos Heuberger
 * $Revision: 1.8 $
 */
public abstract class Command {
    
    public static String getRevision() {
        return Command.class.getName() + " $Revision: 1.8 $";
    }
    
    private static final SortedMap<String, Command> commands = new TreeMap<String, Command>();
    
    static {
        init();
    }
    
    public static Set<String> getNames() {
        return commands.keySet();
    }
    
    public static Command get(String name) {
        assert name != null : "null name";
        Command command = commands.get(name);
        if (command != null)
            return command;
        List<Command> results = new ArrayList<Command>();
        for (String key : commands.keySet()) {
            if (key.startsWith(name))
                results.add(commands.get(key));
        }
        if (results.size() == 1)
            return results.remove(0);
        return null;
    }
    
    public static void init() {
        new AbortCommand();
        new ConnectCommand();
        new FooterCommand();
        new HeaderCommand();
        new ListenCommand();
        new SendCommand();
        new CloseCommand();
        new PrintCommand();
        new PauseCommand();
        new StopCommand();
        new SleepCommand();
        new WaitCommand();
        new ProxyCommand();
    }
    
//  ============================================================================
    
    private final String name;
    private final String usage;
    private final String description;
    
    protected Command(String name, String usage, String description) {
        if (name == null)
            throw new IllegalArgumentException("name must not be null");
        this.name = name;
        this.usage = (usage != null) ? usage : null;
        this.description = (description != null) ? description : null;
        
        if (commands.containsKey(name)) {
            System.err.println("warning: remapping command " + name);
        }
        commands.put(name, this);
    }
    
    public String getName() {
        return name;
    }
    
    public String getUsage() {
        return usage;
    }
    
    public String getDecription() {
        return description;
    }
    
    public abstract void run(ScriptEngine executor, String arg) throws Exception;
    
    protected String createUsageMesssage(String msg) {
        return String.format("%s, usage: %s %s", msg, name, usage);
    }
}
