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

class StatusBarWidget implements Widget {
    private final String TOOLTIP_PROPERTY =
            "anchoredselection.status.anchor-tooltip";
    private final JLabel widget;
    private final View view;

    // {{{ Widget interface implementation
    public StatusBarWidget(final View view) {
        this.view = view;
        widget = new ToolTipLabel();
        widget.setHorizontalAlignment(SwingConstants.CENTER);
        widget.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                Actions.toggleAnchoredSelectionEnabled(view);
            }
        });
    }

    public JComponent getComponent() {
        return widget;
    }

    public void propertiesChanged() {
        update();
    }
    // }}}

    // {{{ update methods

    /* Acquire current anchored selection state and update widget label.
     * Called by the above propertiesChanged and from StatusBarWidgetManager
     * in response to AnchoredSelectionPlugin methods */
    public void update() {
        update(Actions.isAnchoredSelectionEnabled(view));
    }

    /* Update widget label - set enabled/disabled depending on isAnchored.
     * Use a fancy anchor glyph if possible, else use the letter "A" where
     * the case depends on isAnchored as well.
     * Called by StatusBarWidgetManager in response to AnchoredSelectionPlugin
     * methods. */
    void update(boolean isAnchored) {
        this.widget.setToolTipText(jEdit.getProperty(TOOLTIP_PROPERTY));
        boolean anchorGlyphSupported = widget.getFont().canDisplay(0x2693);
        if(isAnchored) {
            widget.setEnabled(true);
            widget.setText(anchorGlyphSupported ? "\u2693" : "A");
        } else {
            widget.setEnabled(false);
            widget.setText(anchorGlyphSupported ? "\u2693" : "a");
        }
    }
    // }}}
}
