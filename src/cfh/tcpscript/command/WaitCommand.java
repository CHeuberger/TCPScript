package cfh.tcpscript.command;

import java.text.ParseException;

import javax.naming.NameNotFoundException;

import cfh.tcpscript.Channel;
import cfh.tcpscript.ScriptEngine;

/**
 * TODO
 * 
 * @author Carlos Heuberger
 * $Revision: 1.8 $
 */
class WaitCommand extends Command {

    WaitCommand() {
        super("wait", "<name> [<pattern>]", "wait till connected (server) or message received (client)");
    }
    
    @Override
    public void run(ScriptEngine executor, String arg) throws Exception {
        boolean debug = arg.startsWith("-d ");
        String[] words = arg.substring(debug?3:0).split("\\s++", 2);
        if (words.length < 1) {
            throw new ParseException(createUsageMesssage("missing argument"), 0);
        }
        String name = words[0];
        Channel channel = Channel.get(name);
        if (channel == null) {
            throw new NameNotFoundException(name);
        }
        if (words.length == 1) {
            channel.waitConnect(debug?executor.getOutput():null);
        } else {
            String regex = words[1];
            channel.waitPattern(regex, debug?executor.getOutput():null);
        }
    }
}
