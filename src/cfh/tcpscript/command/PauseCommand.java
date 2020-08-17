package cfh.tcpscript.command;

import javax.swing.JOptionPane;

import cfh.tcpscript.ScriptEngine;

/**
 * TODO
 * 
 * @author Carlos Heuberger
 * $Revision: 1.3 $
 */
class PauseCommand extends Command {

    PauseCommand() {
        super("pause", "<message>", "waits for user input");
    }
    
    @Override
    public void run(ScriptEngine executor, String arg) throws Exception {
        String message = (arg.isEmpty()) ? "script paused, continue?" : arg;
        int option = JOptionPane.showConfirmDialog(
                executor.getParent(),
                message, 
                executor.getName(), 
                JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) {
            executor.stop();
        }
    }

}
