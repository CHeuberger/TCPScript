package cfh.tcpscript.command;

import java.text.ParseException;

import javax.naming.NameNotFoundException;

import cfh.tcpscript.Channel;
import cfh.tcpscript.ScriptEngine;

/**
 * TODO
 * 
 * @author Carlos Heuberger
 * $Revision: 1.1 $
 */
class ProxyCommand extends Command {

    ProxyCommand() {
        super("proxy", "[stop] <name1> [<|<>|>] <name2>", "starts or stops proxy between <name1> and <name2>, ");
    }
    
    @Override
    public void run(final ScriptEngine executor, String arg) 
    throws Exception {
        String[] words = arg.split("\\s++");
        if (words.length < 2) {
            throw new ParseException(createUsageMesssage("missing arguments"), 0);
        }
        
        int stop = 0;
        if (words[0].equals("stop")) {
            stop = 1;
        }
        String name1 = words[stop];
        String name2;
        boolean bidirectional;
        if (words.length > 2+stop) {
            name2 = words[2+stop];
            if (words[1+stop].equals("<>")) {
                bidirectional = true;
            } else if (words[1+stop].equals(">")) {
                bidirectional = false;
            } else if (words[1+stop].equals("<")) {
                bidirectional = false;
                name2 = name1;
                name1 = words[2+stop];
            } else {
                throw new ParseException(createUsageMesssage("unrecognized argument: " + words[1]), 0);
            }
        } else {
            name2 = words[1+stop];
            bidirectional = false;
        }
        
        final Channel channel1 = Channel.get(name1);
        if (channel1 == null)
            throw new NameNotFoundException(name1);
        
        final Channel channel2 = Channel.get(name2);
        if (channel2 == null)
            throw new NameNotFoundException(name2);
        
        if (stop == 0) {
            executor.startProxy(channel1, channel2, bidirectional);
        } else {
            executor.stopProxy(channel1, channel2, bidirectional);
        }
    }
}
