// :indentSize=4:tabSize=4:noTabs=true:folding=explicit:
package anchoredselection;

// {{{ Imports
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.buffer.JEditBuffer;

import org.gjt.sp.jedit.buffer.BufferAdapter;
import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;

import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanging;
import org.gjt.sp.jedit.msg.PropertiesChanged;

import java.util.Set;
import java.util.Collections;
import java.util.WeakHashMap;
// }}}

/** Assortment of listeners for EditBus, Buffer and TextArea that delegate
 *  handling to the appropriate methods in AnchoredSelectionPlugin.  */
public class Handlers {
    // {{{ singleton boilerplate
    private static Handlers instance = new Handlers();
    private Handlers(){}
    // }}}

    // {{{ start / stop methods
    static void start() {
        EditBus.addToBus(instance);
    }

    static void stop() {
        EditBus.removeFromBus(instance);
        bufferHandler.removeAll();
        caretHandler.removeAll();
    }
    // }}}

    // {{{ EditBus listeners
    @EditBus.EBHandler
    public void handleEditPaneUpdate(EditPaneUpdate msg) {
        if(EditPaneUpdate.BUFFER_CHANGED.equals(msg.getWhat())) {
            AnchoredSelectionPlugin.handleBufferChanged(msg.getEditPane());
        }
    }

    @EditBus.EBHandler
    public void handleViewUpdate(ViewUpdate msg) {
        Object what = msg.getWhat();
        if(ViewUpdate.EDIT_PANE_CHANGED.equals(what)) {
            AnchoredSelectionPlugin.handleEditPaneChanged(msg.getView());
        } else if(ViewUpdate.ACTIVATED.equals(what)) {
            // Workaround for missing PropertiesChanging when canceling
            // combined options dialog
            AnchoredSelectionPlugin.handleOptionsClosed();
        }
    }

    @EditBus.EBHandler
    public void handlePropertiesChanged(PropertiesChanged msg) {
        AnchoredSelectionPlugin.handleOptionsClosed();
    }

    @EditBus.EBHandler
    public void handlePropertiesChanging(PropertiesChanging msg) {
        if(PropertiesChanging.State.LOADING.equals(msg.getState())) {
            AnchoredSelectionPlugin.handleOptionsOpening();
        } else if(PropertiesChanging.State.CANCELED.equals(msg.getState())) {
            AnchoredSelectionPlugin.handleOptionsClosed();
        }
    }
    // }}}

    // {{{ caret and buffer listeners
    static Handler<TextArea> caretHandler = new Handler<TextArea>() {
        CaretListener listener = new CaretListener() {
            public void caretUpdate(CaretEvent event) {
                AnchoredSelectionPlugin.handleCaretUpdate(
                        (TextArea)event.getSource());
            }
        };
        void addListener(TextArea textArea) {
            textArea.addCaretListener(listener);
        }
        void removeListener(TextArea textArea) {
            textArea.removeCaretListener(listener);
        }
    };

    static Handler<JEditBuffer> bufferHandler = new Handler<JEditBuffer>() {
        BufferAdapter listener = new BufferAdapter() {
            public void preContentRemoved(JEditBuffer buffer, int startLine,
                                            int offset, int lines, int length) {
                AnchoredSelectionPlugin.handlePreContentRemoved(
                        buffer, offset, length);
            }
        };
        void addListener(JEditBuffer buffer) {
            buffer.addBufferListener(listener);
        }
        void removeListener(JEditBuffer buffer) {
            buffer.removeBufferListener(listener);
        }
    };
    // }}}

    // {{{ Handler class
    abstract static class Handler<T> {
          // {{{ abstract methods for adding and removing the listener
          abstract void addListener(T target);
          abstract void removeListener(T target);
          // }}}

          // {{{ data structures
          /** Set of listener targets that are currently being handled. */
          private Set<T> listeningTo =
                  Collections.newSetFromMap(new WeakHashMap<T, Boolean>());
          // }}}

          // {{{ interface: listenTo, removeFrom, removeAll

          /** Calls addListener unless target is already being listened to. */
          void listenTo(T target) {
              if(!listeningTo.contains(target)) {
                  addListener(target);
                  listeningTo.add(target);
              }
          }

          /** Calls removeListener if target is being listened to. */
          void removeFrom(T target) {
              if(listeningTo.remove(target)) {
                  removeListener(target);
              }
          }

          /** Call removeListener for all targets that are listened to. */
          void removeAll() {
              for(T target: listeningTo) {
                  removeListener(target);
              }
              listeningTo.clear();
          }
          // }}}
    }
    /// }}}
}
