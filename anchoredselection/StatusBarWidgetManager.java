// :indentSize=4:tabSize=4:noTabs=true:folding=explicit:
package anchoredselection;

//{{{ Imports
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.statusbar.Widget;
import org.gjt.sp.jedit.gui.statusbar.StatusWidgetFactory;
import java.util.Map;
import java.util.WeakHashMap;
//}}}

public class StatusBarWidgetManager implements StatusWidgetFactory {
    private static Map<View, StatusBarWidget> widgets =
            new WeakHashMap<View, StatusBarWidget>();
    private static boolean shuttingDown = false;

    // {{{ StatusWidgetFactory interface implementation

    /* @return StatusBarWidget instance unless stop has been called */
    public Widget getWidget(View view) {
        if(shuttingDown) {
            return null;
        }
        StatusBarWidget widget = new StatusBarWidget(view);
        widgets.put(view, widget);
        return widget;
    }
    // }}}

    // {{{ start / stop methods

    /* Initialization - called upon plug-in start
     * Induce status bar redraw which request widget from getWidget */
    static void start() {
        resetStatusBar();
    }

    /* Teardown - called upon plug-in unloading
     * Set shuttingDown state and induce status bar redraw which will request
     * a widget from getWidget but only get a null. */
    static void stop() {
        shuttingDown = true;
        resetStatusBar();
    }
    // }}}

    // {{{ Interface method for AnchoredSelectionPlugin methods.
    static void updateWidget(View view, Boolean isAnchored) {
        StatusBarWidget widget = widgets.get(view);
        if(widget != null) {
            widget.update(isAnchored);
        }
    }

    static void updateAllWidgets() {
        for(StatusBarWidget widget: widgets.values()) {
            widget.update();
        }
    }
    // }}}

    // {{{ resetStatusBar workaround
    /* Ugly hack to update the statusbar on plugin removal / restart.
     * Change the "view.status" property so it's no longer equal to
     * gui.StatusBar.currentBar but StringTokenizer produces the same tokens.
     * This will induce StatusBar.propertiesChanged to rebuild the status bar
     * (and request a widget from this factory which will only deliver
     * if shuttingDown is false, i.e. stop has not been called).
     */
    private static void resetStatusBar() {
        String statusBar = jEdit.getProperty("view.status");
        if(statusBar == null || statusBar.length() == 0) {
            return;
        }
        if(statusBar.startsWith(" ")) {
            statusBar = statusBar.substring(1);
        } else {
            statusBar = " " + statusBar;
        }
        jEdit.setProperty("view.status", statusBar);
        for(View view: jEdit.getViews()) {
            view.getStatus().propertiesChanged();
        }
    }
    // }}}
}
