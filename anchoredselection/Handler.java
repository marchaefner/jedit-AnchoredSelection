// :indentSize=4:tabSize=4:noTabs=true:folding=explicit:
package anchoredselection;

// {{{ Imports
import java.util.Set;
import java.util.Collections;
import java.util.WeakHashMap;
// }}}

/**
 *  Simple wrapper for listeners that unifies and simplifies adding and removal.
 */
abstract class Handler<T> {
    // {{{ abstract methods for adding and removing the listener
    abstract void addListener(T target);
    abstract void removeListener(T target);
    // }}}

    /** Set of listener targets that are currently being handled. */
    private Set<T> listeningTo =
            Collections.newSetFromMap(new WeakHashMap<T, Boolean>());

    //{{{ interface: listenTo, removeFrom, removeAll

    /** Calls addListener method unless target is already being listened to. */
    void listenTo(T target) {
        if(!listeningTo.contains(target)) {
            addListener(target);
            listeningTo.add(target);
        }
    }

    /** Calls removeListener method if target is already being listened to. */
    void removeFrom(T target) {
        if(listeningTo.remove(target)) {
            removeListener(target);
        }
    }

    /** Call removeListener method for all targets that are listened to. */
    void removeAll() {
        for(T target: listeningTo) {
            removeListener(target);
        }
        listeningTo.clear();
    }
    //}}}

}