// :indentSize=4:tabSize=4:noTabs=true:folding=explicit:
package anchoredselection;

//{{{ Imports
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.statusbar.Widget;
import org.gjt.sp.jedit.gui.statusbar.StatusWidgetFactory;
import java.util.Map;
import java.util.WeakHashMap;
//}}}

public class AnchorWidgetFactory implements StatusWidgetFactory {
    static Map<View, AnchorWidget> widgets = new WeakHashMap<View, AnchorWidget>();

    public Widget getWidget(View view) {
        AnchorWidget widget = new AnchorWidget(view);
        widgets.put(view, widget);
        return widget;
    }

    static void updateWidget(View view, Boolean isAnchored) {
        AnchorWidget widget = widgets.get(view);
        if(widget != null) {
            widget.update(isAnchored);
        }
    }

    static void updateAllWidgets() {
        for(AnchorWidget widget: widgets.values()) {
            widget.update();
        }
    }
}
