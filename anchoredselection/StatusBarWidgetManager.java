// :indentSize=4:tabSize=4:noTabs=true:folding=explicit:
package anchoredselection;

//{{{ Imports
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.statusbar.Widget;
import org.gjt.sp.jedit.gui.statusbar.StatusWidgetFactory;
import java.util.Map;
import java.util.WeakHashMap;
//}}}

public class StatusBarWidgetManager implements StatusWidgetFactory {
    static Map<View, StatusBarWidget> widgets =
            new WeakHashMap<View, StatusBarWidget>();

    public Widget getWidget(View view) {
        if(AnchoredSelectionPlugin.shuttingDown) {
            return null;
        }
        StatusBarWidget widget = new StatusBarWidget(view);
        widgets.put(view, widget);
        return widget;
    }

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
}
