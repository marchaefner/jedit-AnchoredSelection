// :indentSize=4:tabSize=4:noTabs=true:folding=explicit:
package anchoredselection;

import java.util.Set;
import java.util.Collections;
import java.util.WeakHashMap;

abstract class Handler<T> {
    abstract void addListener(T target);
    abstract void removeListener(T target);

    Set<T> listeningTo = Collections.newSetFromMap(new WeakHashMap<T, Boolean>());

    void listenTo(T target) {
        if(!listeningTo.contains(target)) {
            addListener(target);
            listeningTo.add(target);
        }
    }

    void removeFrom(T target) {
        if(listeningTo.remove(target)) {
            removeListener(target);
        }
    }

    void removeAll() {
        for(T target: listeningTo) {
            removeListener(target);
        }
        listeningTo.clear();
    }
}