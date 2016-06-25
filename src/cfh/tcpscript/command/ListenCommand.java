package cfh.tcpscript.command;

import java.net.InetAddress;
import java.text.ParseException;

import cfh.tcp.Server;
import cfh.tcpscript.Channel;
import cfh.tcpscript.ScriptEngine;

/**
 * TODO
 * 
 * @author Carlos Heuberger
 * $Revision: 1.8 $
 */
class ListenCommand extends Command {

    ListenCommand() {
        super("listen", "<name> <port> [<local-addr>]", 
                "listen for incomming connections");
    }

    @Override
    public void run(ScriptEngine executor, String arg) throws Exception {
        Channel channel;
        String[] words = arg.split("\\s++");
        if (words.length < 2) {
            throw new ParseException(createUsageMesssage("missing arguments"), 0);
        }
        String name = words[0];
        if (name.indexOf('-') != -1)
            throw new IllegalArgumentException("invalid name, '-' is not allowed");
        int port = Integer.parseInt(words[1]);
        channel = Channel.get(name);
        if (channel != null)
            throw new IllegalArgumentException("channel name already defined: " + name);
        
        InetAddress bind = null;
        if (words.length >= 3) {
            bind = InetAddress.getByName(words[2]);
        }
        Server server = new Server(port, -1, bind);
        server.addListener(executor.getLogServListener(name, port));
        channel = Channel.create(name, server);
        try {
            channel.start();
        } catch (Exception ex) {
            channel.stop(0);
            Channel.remove(name);
            throw ex;
        }
    }
}
