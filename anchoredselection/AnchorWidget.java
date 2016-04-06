// :indentSize=4:tabSize=4:noTabs=true:folding=explicit:
package anchoredselection;

//{{{ Imports
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.statusbar.Widget;
import org.gjt.sp.jedit.gui.statusbar.ToolTipLabel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
//}}}

class AnchorWidget implements Widget {
    private final JLabel widget;
    private final View view;

    public AnchorWidget(final View view) {
        this.view = view;
        widget = new ToolTipLabel();
        widget.setHorizontalAlignment(SwingConstants.CENTER);
        widget.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                AnchoredSelectionPlugin.toggleAnchor(view);
            }
        });
    }

    public JComponent getComponent() {
        return widget;
    }

    public void propertiesChanged() {
        update();
    }

    public void update() {
        update(AnchoredSelectionPlugin.isAnchored(view.getTextArea()));
    }

    void update(boolean isAnchored) {
        this.widget.setToolTipText(jEdit.getProperty("view.status.anchor-tooltip"));
        boolean anchorGlyphSupported = widget.getFont().canDisplay(9875);
        if(isAnchored) {
            widget.setEnabled(true);
            widget.setText(anchorGlyphSupported ? "\u2693" : "A");
        } else {
            widget.setEnabled(false);
            widget.setText(anchorGlyphSupported ? "\u2693" : "a");
        }
    }
}
