package cfh.tcpscript.command;

import java.text.ParseException;

import cfh.tcp.Connection;
import cfh.tcpscript.Channel;
import cfh.tcpscript.ScriptEngine;

/**
 * TODO
 * 
 * @author Carlos Heuberger
 * $Revision: 1.9 $
 */
class ConnectCommand extends Command {

    ConnectCommand() {
        super("connect", "<name> [<address>] <port>", "connect to server");
    }
    
    @Override
    public void run(ScriptEngine executor, String arg) 
    throws Exception {
        Channel channel;
        String[] words = arg.split("\\s++");
        if (words.length < 2) {
            throw new ParseException(createUsageMesssage("missing arguments"), 0);
        }
        String name = words[0];
        if (name.indexOf('-') != -1)
            throw new IllegalArgumentException("invalid name, '-' is not allowed");
        String host = (words.length >= 3) ? words[1] : null;
        int port = Integer.parseInt(words[words.length - 1]);
        channel = Channel.get(name);
        if (channel != null)
            throw new IllegalArgumentException("channel name already defined: " + name);

        Connection client = new Connection(host, port);
        client.addListener(executor.getLogConnListener(name, port));
        channel = Channel.create(name, client);
        try {
            channel.start();
        } catch (Exception ex) {
            channel.stop(0);
            Channel.remove(name);
            throw ex;
        }
    }
}
