package cfh.tcpscript.command;

import cfh.tcpscript.ScriptEngine;

/**
 * TODO
 * 
 * @author Carlos Heuberger
 * $Revision: 1.2 $
 */
class StopCommand extends Command {

    StopCommand() {
        super("stop", "", "stop the current script");
    }
    
    @Override
    public void run(ScriptEngine executor, String arg) {
        executor.stop();
    }

}
