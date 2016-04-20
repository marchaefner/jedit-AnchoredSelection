// :indentSize=4:tabSize=4:noTabs=true:folding=explicit:
package anchoredselection;

// {{{ Imports
import org.gjt.sp.jedit.EditPlugin;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.buffer.JEditBuffer;

import java.util.Set;
import java.util.Collections;
import java.util.WeakHashMap;
// }}}

public class AnchoredSelectionPlugin extends EditPlugin {
    public static final String NAME = "anchoredselection";
    public static final String OPTION_PREFIX = "options.anchoredselection.";

    // {{{ Data structures

    /* anchorMap holds the anchor position for each buffer in each text area */
    private static AnchorMap anchorMap = new AnchorMap();
    /* text areas for which the next caret update should be ignored */
    private static Set<TextArea> skipCaretUpdate = Collections.newSetFromMap(
                                        new WeakHashMap<TextArea, Boolean>());
    // }}}

    // {{{ Plug in startup / teardown

    /** Override built-in actions and add status bar widgets */
    public void start()	{
        Actions.overrideBuiltInActions();
        StatusBarWidgetManager.start();
        Handlers.start();
    }

    /** Remove all listeners, overridden actions and status bar widgets. */
    public void stop()	{
        Handlers.stop();
        StatusBarWidgetManager.stop();
        Actions.removeOverriddenActions();
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
    static void handleCaretUpdate(TextArea textArea) {
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
     *  If the the buffer has no anchor left in any text areas remove its
     *  listener. Also remove caret listeners from any text areas that
     *  where the anchor was removed and update all status bar widgets.
     */
    static void handlePreContentRemoved(JEditBuffer buffer,
                                                int offset, int length) {
        anchorMap.remove(buffer, offset, length);
        if(!anchorMap.contains(buffer)) {
            Handlers.bufferHandler.removeFrom(buffer);
        }
        for(View view: jEdit.getViews()) {
            TextArea textArea = view.getTextArea();
            if(hasAnchor(textArea)) {
                StatusBarWidgetManager.updateWidget(view, true);
            } else {
                Handlers.caretHandler.removeFrom(textArea);
                StatusBarWidgetManager.updateWidget(view, false);
            }
        }
    }

    /** If the buffer changes add or remove the caret listener of the edit panes
     *  text area and update the status bar widget. */
    static void handleBufferChanged(EditPane editPane) {
        TextArea textArea = editPane.getTextArea();
        boolean isAnchored = hasAnchor(textArea);
        if(isAnchored) {
            Handlers.caretHandler.listenTo(textArea);
        } else {
            Handlers.caretHandler.removeFrom(textArea);
        }
        StatusBarWidgetManager.updateWidget(editPane.getView(), isAnchored);
    }

    /** If the edit pane changes update the status bar widget. */
    static void handleEditPaneChanged(View view) {
        StatusBarWidgetManager.updateWidget(view,
                                            hasAnchor(view.getTextArea()));
    }

    /** If the options dialog is opened hide overridden actions.
     *  (This handler might be called multiple times.) */
    static void handleOptionsOpening() {
        Actions.removeOverriddenActions();
    }

    /** If options dialog is closed override built-in actions.
     *  (This handler will be called multiple times.) */
    static void handleOptionsClosed() {
        Actions.overrideBuiltInActions();
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
        Handlers.caretHandler.listenTo(textArea);
        Handlers.bufferHandler.listenTo(textArea.getBuffer());
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
        Handlers.caretHandler.removeFrom(textArea);
        if(!anchorMap.contains(buffer)) {
            Handlers.bufferHandler.removeFrom(buffer);
        }
        StatusBarWidgetManager.updateWidget(view, false);
    }
    // }}}
}
