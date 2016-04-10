// :indentSize=4:tabSize=4:noTabs=true:folding=explicit:
package anchoredselection;

// {{{ Imports
import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.ActionSet;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.BeanShellAction;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Macros;
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

    static final String[] copyActionNames = new String[] {
        "copy", "copy-append",
        "copy-string-register", "copy-append-string-register"
    };
    static final String[] selectActionNames = new String[]{
        "select-none", "select-all",
        "select-fold", "select-paragraph", "select-block", "select-line-range",
        "select-line", "select-word"
    };
    static final String[] caretMoveActionNames = new String[]{
        "next-char", "prev-char",   // place caret at start/end of selection
        "next-line", "prev-line"    // lose virtual width of rect selection
    };
    static final String[] optionActionNames = new String[]{
        "combined-options", "global-options", "plugin-options"
    };
    static final String ACTION_METHOD_PREFIX =
        "anchoredselection.AnchoredSelectionPlugin.";

    static ActionSet overriddenBuiltInActionSet;
    static ActionSet builtinActionSet;
    static AnchorMap anchorMap;
    static boolean shuttingDown = false;

    @Override
    public void start()	{
        builtinActionSet = jEdit.getBuiltInActionSet();
        anchorMap = new AnchorMap();
        installActions();
        resetStatusBar();
        EditBus.addToBus(this);
    }

    @Override
    public void stop()	{
        shuttingDown = true;
        EditBus.removeFromBus(this);
        resetStatusBar();
        removeActions();
        caretHandler.removeAll();
        bufferHandler.removeAll();
    }

    /* Ugly hack to update the statusbar on plugin removal / restart.
     * Change the "view.status" property so it's no longer equal to
     * gui.StatusBar.currentBar but StringTokenizer produces the same tokens.
     * This will induce StatusBar.propertiesChanged to rebuild the status bar
     * (and request a widget from AnchorWidgetFactory which will only deliver
     * if AnchoredSelectionPlugin.shuttingDown is false).
     */
    private void resetStatusBar() {
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
                    skipCaretUpdate.add(textArea);
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
                updateAllWidgets();
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
            boolean isAnchored = isAnchored(textArea);
            if(isAnchored) {
                caretHandler.listenTo(textArea);
            } else {
                caretHandler.removeFrom(textArea);
            }
            updateWidget(editPane.getView(), isAnchored);
        }
    }

    @EBHandler
    public void handleViewUpdate(ViewUpdate updateMessage) {
        if(ViewUpdate.EDIT_PANE_CHANGED.equals(updateMessage.getWhat())) {
            updateWidget(updateMessage.getView());
        }
    }

    @EBHandler
    public void handlePropertiesChanged(PropertiesChanged changedMessage) {
        // jEdit sends this with a source null after the options dialog closes.
        if(changedMessage.getSource() == null) {
            installActions();
        }
    }


    static void updateWidget(View view) {
        updateWidget(view, isAnchored(view.getTextArea()));
    }

    static void updateWidget(View view, Boolean isAnchored) {
        AnchorWidgetFactory.updateWidget(view, isAnchored);
    }

    static void updateAllWidgets() {
        AnchorWidgetFactory.updateAllWidgets();
    }


    // {{{ public interface
    public static boolean isAnchored(TextArea textArea) {
        return anchorMap.contains(textArea);
    }

    public static void dropAnchor(View view) {
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
        updateWidget(view, true);
    }

    public static void raiseAnchor(View view) {
        TextArea textArea = view.getTextArea();
        JEditBuffer buffer = textArea.getBuffer();
        anchorMap.remove(textArea);
        caretHandler.removeFrom(textArea);
        if(!anchorMap.contains(buffer)) {
            bufferHandler.removeFrom(buffer);
        }
        updateWidget(view, false);
    }

    public static void toggleAnchor(View view) {
        if(isAnchored(view.getTextArea())) {
            recordMacro(view, ACTION_METHOD_PREFIX + "raiseAnchor(view);");
            raiseAnchor(view);
        } else {
            recordMacro(view, ACTION_METHOD_PREFIX + "dropAnchor(view);");
            dropAnchor(view);
        }
    }
    // }}}

    // {{{ action wrappers
    public static void raiseAnchorAndInvoke(View view, String actionName) {
        if(isAnchored(view.getTextArea())) {
            recordMacro(view, ACTION_METHOD_PREFIX + "raiseAnchor(view);");
            raiseAnchor(view);
        }
        invokeAction(view, actionName);
    }

    public static void invokeSelectVariant(View view, String actionName) {
        TextArea textArea = view.getTextArea();
        if(isAnchored(textArea)) {
            actionName = "select-" + actionName;
            // the action will fire two caret updates: first from caret move
            // and then from selection resizing. We skip the first to avoid
            // reseting the selection and losing the virtual width of a
            // rectangular selection
            skipCaretUpdate.add(textArea);
        }
        invokeAction(view, actionName);
    }
    public static void removeActionsAndInvoke(View view, String actionName) {
        removeActions();
        invokeAction(view, actionName);
    }
    // }}}

    private static EditAction wrapAction(String actionName, String method) {
        EditAction action = builtinActionSet.getAction(actionName);
        if(action == null) {
            return null;
        }
        StringBuilder code = new StringBuilder(100);
        code.append(ACTION_METHOD_PREFIX);
        code.append(method);
        code.append("(view, \"");
        code.append(actionName);
        code.append("\");");
        return new BeanShellAction(actionName, code.toString(), null,
                                    action.noRepeat(), true,
                                    action.noRememberLast());
    }

    static void installActions() {
        removeActions();
        EditAction action;
        overriddenBuiltInActionSet = new ActionSet(
            builtinActionSet.getLabel() + " - anchored selection compatible");
        for(String actionName: selectActionNames) {
            action = wrapAction(actionName, "raiseAnchorAndInvoke");
            if(action != null) {
                overriddenBuiltInActionSet.addAction(action);
            }
        }
        for(String actionName: copyActionNames) {
            action = wrapAction(actionName, "raiseAnchorAndInvoke");
            if(action != null) {
                overriddenBuiltInActionSet.addAction(action);
            }
        }
        for(String actionName: caretMoveActionNames) {
            action = wrapAction(actionName, "invokeSelectVariant");
            if(action != null) {
                overriddenBuiltInActionSet.addAction(action);
            }
        }
        for(String actionName: optionActionNames) {
            action = wrapAction(actionName, "removeActionsAndInvoke");
            if(action != null) {
                overriddenBuiltInActionSet.addAction(action);
            }
        }
        jEdit.addActionSet(overriddenBuiltInActionSet);
    }

    static void removeActions() {
        if(overriddenBuiltInActionSet != null) {
            jEdit.removeActionSet(overriddenBuiltInActionSet);
            overriddenBuiltInActionSet = null;
        }
    }

    /**
     *  Record with current macro recorder (if one is active).
     */
    private static void recordMacro(View view, String cmd) {
        Macros.Recorder recorder = view.getMacroRecorder();
        if(recorder != null) {
            recorder.record(cmd);
        }
    }

    private static void invokeAction(View view, String actionName) {
        EditAction action = builtinActionSet.getAction(actionName);
        if(!action.noRecord()) {
            recordMacro(view, action.getCode());
        }
        action.invoke(view);
    }
    // }}}
}
