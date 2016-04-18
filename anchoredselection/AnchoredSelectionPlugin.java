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
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanging;
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

    // {{{ Data structures & handler scaffolding

    /* anchorMap holds the anchor position for each buffer in each text area */
    private static AnchorMap anchorMap = new AnchorMap();
    /* text areas for which the next caret update should be ignored */
    private static Set<TextArea> skipCaretUpdate = Collections.newSetFromMap(
                                        new WeakHashMap<TextArea, Boolean>());

    // {{{ Handler classes
    // build Handlers for easier adding/removal - for actual handling see below
    private static Handler<TextArea> caretHandler = new Handler<TextArea>() {
        CaretListener listener = new CaretListener() {
            public void caretUpdate(CaretEvent event) {
                handleCaretUpdate((TextArea)event.getSource());
            }
        };
        void addListener(TextArea textArea) {
            textArea.addCaretListener(listener);
        }
        void removeListener(TextArea textArea) {
            textArea.removeCaretListener(listener);
        }
    };

    private static Handler<JEditBuffer> bufferHandler = new Handler<JEditBuffer>() {
        BufferAdapter listener = new BufferAdapter() {
            public void preContentRemoved(JEditBuffer buffer, int startLine,
                                            int offset, int lines, int length) {
                handlePreContentRemoved(buffer, offset, length);
            }
        };
        void addListener(JEditBuffer buffer) {
            buffer.addBufferListener(listener);
        }
        void removeListener(JEditBuffer buffer) {
            buffer.removeBufferListener(listener);
        }
    };
    // }}} }}}

    // {{{ Plug in startup / teardown

    /** Override built-in actions and add status bar widgets */
    public void start()	{
        Actions.overrideBuiltInActions();
        StatusBarWidgetManager.start();
        EditBus.addToBus(this);
    }

    /** Remove all listeners, overridden actions and status bar widgets. */
    public void stop()	{
        EditBus.removeFromBus(this);
        StatusBarWidgetManager.stop();
        Actions.removeOverriddenActions();
        caretHandler.removeAll();
        bufferHandler.removeAll();
    }
    // }}}

    // {{{ Handler methods

    /**
     *  Re-set selection from the anchor to the new caret position.
     *
     *  Don't do anything if the selection is already as it should be (and
     *  naturally if no anchor exists for the current buffer or skipCaretUpdate
     *  has been set).
     */
    private static void handleCaretUpdate(TextArea textArea) {
        if(skipCaretUpdate.remove(textArea) || !anchorMap.contains(textArea)) {
            return;
        }
        int anchor = anchorMap.get(textArea);
        int caret = textArea.getCaretPosition();
        Selection selection = textArea.getSelectionAtOffset(caret);
        if(selection != null
                && selection.getStart() == Math.min(caret, anchor)
                && selection.getEnd() == Math.max(caret,anchor)) {
            return;
        }
        // resizeSelection will fire a caret update which can be ignored.
        skipCaretUpdate(textArea);
        textArea.resizeSelection(anchor, caret, 0,
                        textArea.isRectangularSelectionEnabled());
    }

    /**
     *  Update anchor position or remove anchor if its position was removed.
     *
     *  If the the buffer has no anchor left in any text areas remove this
     *  listener. Update the status bar widget in case the anchor was removed.
     */
    private static void handlePreContentRemoved(JEditBuffer buffer,
                                                int offset, int length) {
        anchorMap.remove(buffer, offset, length);
        if(!anchorMap.contains(buffer)) {
            bufferHandler.removeFrom(buffer);
        }
        StatusBarWidgetManager.updateAllWidgets();
    }

    /** If the buffer changes add or remove the caret listener of the edit panes
     *  text area and update the status bar widget. */
    @EditBus.EBHandler
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

    /** If the edit pane changes update the status bar widget. If a view gets
     *  activated this might be caused by canceling the options dialog -
     *  override built-in actions (if they are not already overridden). */
    @EditBus.EBHandler
    public void handleViewUpdate(ViewUpdate updateMessage) {
        Object what = updateMessage.getWhat();
        if(ViewUpdate.EDIT_PANE_CHANGED.equals(what)) {
            View view = updateMessage.getView();
            StatusBarWidgetManager.updateWidget(view,
                                                hasAnchor(view.getTextArea()));
        } else if(ViewUpdate.ACTIVATED.equals(what)) {
            Actions.overrideBuiltInActions();
        }
    }

    /** If the options pane is closed override build-in actions*/
    @EditBus.EBHandler
    public void handlePropertiesChanged(PropertiesChanged changedMessage) {
        Actions.overrideBuiltInActions();
    }

    /** If the options dialog opens remove overridden actions, if it's canceled
     *  re-install them. */
    @EditBus.EBHandler
    public void handlePropertiesChanging(PropertiesChanging message) {
        if(PropertiesChanging.State.LOADING.equals(message.getState())) {
            Actions.removeOverriddenActions();
        } else if(PropertiesChanging.State.CANCELED.equals(message.getState())) {
            Actions.overrideBuiltInActions();
        }
    }
    /// }}}

    // {{{ Interface

    /** Skip the next caret update of textArea. */
    static void skipCaretUpdate(TextArea textArea) {
        skipCaretUpdate.add(textArea);
    }

    /** Whether the current buffer of textArea has an achor set, i.e. whether
     *  anchored selection mode is enabled. */
    static boolean hasAnchor(TextArea textArea) {
        return anchorMap.contains(textArea);
    }

    /**
     *  Start anchored selection mode.
     *
     *  Set the current caret position as the anchor position. If the caret is
     *  at a selection make an educated guess where the selection start is
     *  (whether before or after the caret) and use it as the anchor position.
     *
     *  Install listeners (if not already installed) and update the status bar
     *  widget.
     */
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

    /**
     *  End anchored selection mode.
     *
     *  Forget the anchor position and remove listeners (keep the buffer
     *  listener if the buffer has an anchor in another text area) and update
     *  the status bar widget.
     */
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
