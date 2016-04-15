// :indentSize=4:tabSize=4:noTabs=true:folding=explicit:
package anchoredselection;

// {{{ Imports
import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.buffer.BufferAdapter;
import org.gjt.sp.jedit.buffer.BufferListener;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;

import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;
import java.util.Set;
import java.util.Collections;
import java.util.WeakHashMap;
// }}}

public class AnchoredSelectionPlugin extends EditPlugin {
    public static final String NAME = "anchoredselection";
    public static final String OPTION_PREFIX = "options.anchoredselection.";

    static AnchorMap anchorMap;

    @Override
    public void start()	{
        anchorMap = new AnchorMap();
        Actions.overrideBuiltInActions();
        StatusBarWidgetManager.start();
        EditBus.addToBus(this);
    }

    @Override
    public void stop()	{
        EditBus.removeFromBus(this);
        StatusBarWidgetManager.stop();
        Actions.removeOverriddenActions();
        caretHandler.removeAll();
        bufferHandler.removeAll();
    }

    static Set<TextArea> skipCaretUpdate =
                Collections.newSetFromMap(new WeakHashMap<TextArea, Boolean>());

    static Handler caretHandler = new Handler<TextArea>() {
        CaretListener listener = new CaretListener() {
            public void caretUpdate(CaretEvent event) {
                try {
                    resizeSelection((TextArea)event.getSource());
                } catch(ClassCastException ex) {}
            }
        };

        void resizeSelection(TextArea textArea) {
            if(!skipCaretUpdate.remove(textArea)) {
                Integer anchor = anchorMap.get(textArea);
                if(anchor != null) {
                    int caret = textArea.getCaretPosition();
                    Selection selection = textArea.getSelectionAtOffset(caret);
                    if(selection != null
                            && selection.getStart() == Math.min(caret, anchor)
                            && selection.getEnd() == Math.max(caret,anchor)) {
                        return;
                    }
                    skipCaretUpdate(textArea);
                    textArea.resizeSelection(anchor, caret, 0,
                                    textArea.isRectangularSelectionEnabled());
                }
            }
        }

        void addListener(TextArea textArea) {
            textArea.addCaretListener(listener);
        }
        void removeListener(TextArea textArea) {
            textArea.removeCaretListener(listener);
        }
    };

    static Handler bufferHandler = new Handler<JEditBuffer>() {
        BufferListener listener = new BufferAdapter() {
            public void preContentRemoved(JEditBuffer buffer, int startLine,
                                            int offset, int numLines, int length) {
                anchorMap.remove(buffer, offset, length);
                if(!anchorMap.contains(buffer)) {
                    bufferHandler.removeFrom(buffer);
                }
                StatusBarWidgetManager.updateAllWidgets();
            }
        };
        void addListener(JEditBuffer buffer) {
            buffer.addBufferListener(listener);
        }
        void removeListener(JEditBuffer buffer) {
            buffer.removeBufferListener(listener);
        }
    };

    @EBHandler
    public void handleEditPaneUpdate(EditPaneUpdate updateMessage) {
        if(EditPaneUpdate.BUFFER_CHANGED.equals(updateMessage.getWhat())) {
            EditPane editPane = updateMessage.getEditPane();
            TextArea textArea = editPane.getTextArea();
            boolean isAnchored = hasAnchor(textArea);
            if(isAnchored) {
                caretHandler.listenTo(textArea);
            } else {
                caretHandler.removeFrom(textArea);
            }
            StatusBarWidgetManager.updateWidget(editPane.getView(), isAnchored);
        }
    }

    @EBHandler
    public void handleViewUpdate(ViewUpdate updateMessage) {
        if(ViewUpdate.EDIT_PANE_CHANGED.equals(updateMessage.getWhat())) {
            View view = updateMessage.getView();
            StatusBarWidgetManager.updateWidget(view, hasAnchor(view.getTextArea()));
        }
    }

    @EBHandler
    public void handlePropertiesChanged(PropertiesChanged changedMessage) {
        // jEdit sends this with a source null after the options dialog closes.
        if(changedMessage.getSource() == null) {
            Actions.overrideBuiltInActions();
        }
    }
    /// }}}

    // {{{ Interface

    static void skipCaretUpdate(TextArea textArea) {
        skipCaretUpdate.add(textArea);
    }

    static boolean hasAnchor(TextArea textArea) {
        return anchorMap.contains(textArea);
    }

    static void dropAnchor(View view) {
        TextArea textArea = view.getTextArea();
        int caret = textArea.getCaretPosition();
        int anchor = caret;
        Selection selection = textArea.getSelectionAtOffset(caret);
        if(selection != null) {
            anchor = selection.getStart();
            if(anchor == caret) {
                anchor = selection.getEnd();
            }
        }
        anchorMap.set(textArea, anchor);
        caretHandler.listenTo(textArea);
        bufferHandler.listenTo(textArea.getBuffer());
        StatusBarWidgetManager.updateWidget(view, true);
    }

    static void raiseAnchor(View view) {
        TextArea textArea = view.getTextArea();
        JEditBuffer buffer = textArea.getBuffer();
        anchorMap.remove(textArea);
        caretHandler.removeFrom(textArea);
        if(!anchorMap.contains(buffer)) {
            bufferHandler.removeFrom(buffer);
        }
        StatusBarWidgetManager.updateWidget(view, false);
    }
    // }}}
}
