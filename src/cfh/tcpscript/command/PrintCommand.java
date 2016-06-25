package cfh.tcpscript.command;

import cfh.tcpscript.ScriptEngine;
import cfh.tcpscript.StringHelper;

/**
 * TODO
 * 
 * @author Carlos Heuberger
 * $Revision: 1.5 $
 */
class PrintCommand extends Command {

    PrintCommand() {
        super("print", "<message>", "print the message;");
    }
    
    @Override
    public void run(ScriptEngine executor, String arg) {
        executor.println(StringHelper.convertSlash(arg));
    }

}
