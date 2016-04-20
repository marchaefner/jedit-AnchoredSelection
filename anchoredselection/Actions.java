// :indentSize=4:tabSize=4:noTabs=true:folding=explicit:
package anchoredselection;

// {{{ Imports
import org.gjt.sp.jedit.ActionSet;
import org.gjt.sp.jedit.BeanShellAction;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Macros;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.textarea.TextArea;
// }}}

public class Actions {
    // {{{ variables

    /** prefix for constructing qualified names of plugin actions */
    private static String ACTION_METHOD_PREFIX = "anchoredselection.Actions.";
    /** jEdits built-in actions */
    private static ActionSet builtinActionSet = jEdit.getBuiltInActionSet();
    /** ActionSet that contains the overriden/wrapped actions */
    private static ActionSet overriddenBuiltInActionSet;
    // }}}

    // {{{ names of actions to be overridden

    /** Built-in copy actions. These will be wrapped with
     *  {@link #raiseAnchorAndInvoke} */
    static final String[] copyActionNames = new String[] {
        "copy", "copy-append",
        "copy-string-register", "copy-append-string-register"
    };
    /** Built-in select actions that end anchored selection mode (as it is
     *  unclear where the new anchor position would be). These will be wrapped
     *  with {@link #raiseAnchorAndInvoke} */
    static final String[] selectActionNames = new String[]{
        "select-none", "select-all",
        "select-fold", "select-paragraph", "select-block", "select-line-range",
        "select-line", "select-word"
    };
    /** Built-in caret movements that need special care because they behave
     *  differently within selections. These will be wrapped with
     *  {@link #invokeSelectVariant} */
    static final String[] caretMoveActionNames = new String[]{
        // actions which place caret at start/end of selection
        "next-char", "prev-char",
        "home", "end", "smart-home", "smart-end",
        "line-home", "line-end", "whitespace-home", "whitespace-end",
        // actions which lose virtual width of rect selection
        "next-line", "prev-line"
    };
    /** Built-in actions that open options dialog. These will be wrapped with
     *  {@link #invokeOptions}. This is a workaround for the combined options
     *  dialog since jEdit 5 which does not send a PropertiesChanging. */
    static final String[] optionActionNames = new String[]{
        "combined-options", "global-options", "plugin-options"
    };
    // }}}

    // {{{ public interface

    /** Returns whether anchored selection is enabled. */
    public static boolean isAnchoredSelectionEnabled(View view)
    {
        return AnchoredSelectionPlugin.hasAnchor(view.getTextArea());
    }

    /** Toggles anchored selection. */
    public static void toggleAnchoredSelectionEnabled(View view)
    {
        if(isAnchoredSelectionEnabled(view)) {
            macroRecord(view, ACTION_METHOD_PREFIX +
                                "setAnchoredSelectionEnabled(view, false);");
            setAnchoredSelectionEnabled(view, false);
        } else {
            macroRecord(view, ACTION_METHOD_PREFIX +
                                "setAnchoredSelectionEnabled(view, true);");
            setAnchoredSelectionEnabled(view, true);
        }
    }

    /**
     *  Set anchored selection on or off according to the value of
     *  <code>anchoredSelectionMode</code>.
     *
     *  @param anchoredSelectionMode Should anchored selection be enabled?
     */
    public static void setAnchoredSelectionEnabled(View view,
                                                boolean anchoredSelectionMode) {
        if(anchoredSelectionMode) {
            AnchoredSelectionPlugin.dropAnchor(view);
        } else {
            AnchoredSelectionPlugin.raiseAnchor(view);
        }
    }
    // }}}

    // {{{ action wrappers

    /** Wrapper for internal use that ends anchored selection mode before
     *  invoking the built-in action. */
    public static void raiseAnchorAndInvoke(View view, String actionName) {
        if(isAnchoredSelectionEnabled(view)) {
            macroRecord(view, ACTION_METHOD_PREFIX +
                                "setAnchoredSelectionEnabled(view, false);");
            setAnchoredSelectionEnabled(view, false);
        }
        invokeBuiltInAction(view, actionName);
    }

    /** Wrapper for internal use that invokes the corresponding selecting
     *  version of the built-in action if anchored selection mode is enabled. */
    public static void invokeSelectVariant(View view, String actionName) {
        if(isAnchoredSelectionEnabled(view)) {
            actionName = "select-" + actionName;
            // the action will fire two caret updates: first from caret move
            // and then from selection resizing. We skip the first to avoid
            // reseting the selection and losing the virtual width of a
            // rectangular selection
            AnchoredSelectionPlugin.skipCaretUpdate(view.getTextArea());
        }
        invokeBuiltInAction(view, actionName);
    }

    /** Wrapper for internal use that works around the missing
     *  PropertiesChanging message by calling the handling method explicitly. */
    public static void invokeOptions(View view, String actionName) {
        AnchoredSelectionPlugin.handleOptionsOpening();
        invokeBuiltInAction(view, actionName);
    }
    // }}}

    // {{{ wrapper installation / removal

    /** Create a new action that calls one the above action wrappers */
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

    /** Create actions for the wrapper method and add them to the action set. */
    private static void addWrappers(String[] actionNames, String wrapperName) {
        EditAction action;
        for(String actionName: actionNames) {
            action = wrapAction(actionName, wrapperName);
            if(action != null) {
                overriddenBuiltInActionSet.addAction(action);
            }
        }
    }

    /** Build a new action set and add action wrappers */
    static void overrideBuiltInActions() {
        if(overriddenBuiltInActionSet != null) {
            return;
        }
        overriddenBuiltInActionSet = new ActionSet(
            builtinActionSet.getLabel() + " - anchored selection compatible");
        addWrappers(selectActionNames, "raiseAnchorAndInvoke");
        addWrappers(copyActionNames, "raiseAnchorAndInvoke");
        addWrappers(caretMoveActionNames, "invokeSelectVariant");
        addWrappers(optionActionNames, "invokeOptions");
        jEdit.addActionSet(overriddenBuiltInActionSet);
    }

    /** Uninstall action set with wrapped actions */
    static void removeOverriddenActions() {
        if(overriddenBuiltInActionSet != null) {
            jEdit.removeActionSet(overriddenBuiltInActionSet);
            overriddenBuiltInActionSet = null;
        }
    }
    // }}}

    // {{{ utility functions

    /** Record with current macro recorder (if one is active). */
    private static void macroRecord(View view, String cmd) {
        Macros.Recorder recorder = view.getMacroRecorder();
        if(recorder != null) {
            recorder.record(cmd);
        }
    }

    /** Invoke built-in action and records with macro recorder (if appropriate) */
    private static void invokeBuiltInAction(View view, String actionName) {
        EditAction action = builtinActionSet.getAction(actionName);
        if(!action.noRecord()) {
            macroRecord(view, action.getCode());
        }
        action.invoke(view);
    }
    // }}}
}
