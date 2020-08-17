package cfh.tcpscript;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

public class ColumnIndicator implements CaretListener {

    private final JLabel output;
    
    public ColumnIndicator(JLabel statusColumn) {
        if (statusColumn == null)
            throw new IllegalArgumentException("null output");
        this.output = statusColumn;
    }

    @Override
    public void caretUpdate(CaretEvent ev) {
        if (ev.getSource() instanceof JTextComponent) {
            JTextComponent comp = (JTextComponent) ev.getSource();
            int from = comp.getSelectionStart();
            int to = comp.getSelectionEnd();
            int line; 
            if (comp instanceof JTextArea) {
                try {
                    line = ((JTextArea)comp).getLineOfOffset(comp.getCaretPosition());
                } catch (BadLocationException e) {
                    e.printStackTrace();
                    line = -1;
                }
            } else {
                line = -1;
            }
            String text = comp.getText();
            int lf;
            for (lf = from; lf > 0; lf--) {
                char ch = text.charAt(lf-1);
                if (ch == '\n' || ch == '\r')
                    break;
            }
            from -= lf;
            to -= lf;
            String str;
            if (to > from) {
                str = String.format("pos=%d-%d, count=%d", from, to, to-from);
            } else {
                str = String.format("pos=%d", from);
            }
            if (line != -1) {
                str = String.format("%s, line=%d", str, line);
            }
            output.setText(str);
        }
    }
}
