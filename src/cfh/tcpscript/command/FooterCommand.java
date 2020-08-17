package cfh.tcpscript.command;

import java.text.ParseException;

import javax.naming.NameNotFoundException;

import cfh.tcpscript.Channel;
import cfh.tcpscript.ScriptEngine;

/**
 * TODO
 * 
 * @author Carlos Heuberger
 * $Revision: 1.5 $
 */
class FooterCommand extends Command {

    FooterCommand() {
        super("footer", "<name> [<footer>]", 
                "sets the footer of a channel");
    }
    
    @Override
    public void run(ScriptEngine executor, String arg) throws Exception {
        String[] words = arg.split("\\s++", 2);
        if (words.length < 1) {
            throw new ParseException(createUsageMesssage("missing arguments"), 0);
        }
        String name = words[0];
        Channel channel = Channel.get(name);
        if (channel == null) {
            throw new NameNotFoundException(name);
        }
        String footer = (words.length < 2) ? "" : words[1];
        channel.setFooter(footer);
    }

}
