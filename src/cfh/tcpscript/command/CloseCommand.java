package cfh.tcpscript.command;

import java.text.ParseException;

import javax.naming.NameNotFoundException;

import cfh.tcpscript.Channel;
import cfh.tcpscript.ScriptEngine;

/**
 * TODO
 * 
 * @author Carlos Heuberger
 * $Revision: 1.7 $
 */
class CloseCommand extends Command {

    CloseCommand() {
        super("close", "<name>", "close the channel");
    }
    
    @Override
    public void run(ScriptEngine executor, String arg) throws Exception {
        String[] words = arg.split("\\s++");
        if (words.length < 1) {
            throw new ParseException(createUsageMesssage("missing argument"), 0);
        }
        String name = words[0];
        Channel channel = Channel.get(name);
        if (channel == null) {
            throw new NameNotFoundException(name);
        }

        channel.stop(0);
    }

}
