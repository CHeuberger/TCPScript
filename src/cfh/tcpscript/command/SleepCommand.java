package cfh.tcpscript.command;

import java.text.ParseException;

import cfh.tcpscript.ScriptEngine;

/**
 * TODO
 * 
 * @author Carlos Heuberger
 * $Revision: 1.3 $
 */
class SleepCommand extends Command {

    SleepCommand() {
        super("sleep", "<milliseconds>", "sleep for the given time");
    }
    
    @Override
    public void run(ScriptEngine executor, String arg) throws Exception {
        String[] words = arg.split("\\s++");
        if (words.length < 1) {
            throw new ParseException(createUsageMesssage("missing time"), 0);
        }
        long delta = Long.parseLong(words[0]);
        try {
            Thread.sleep(delta);
        } catch (InterruptedException ex) {
            executor.handle(ex);
            Thread.currentThread().interrupt();
        }
    }

}
