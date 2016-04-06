// :indentSize=4:tabSize=4:noTabs=true:folding=explicit:
package anchoredselection;

// {{{ Imports
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.textarea.TextArea;

//import org.gjt.sp.util.Log;

import java.util.Map;
import java.util.WeakHashMap;

class AnchorMap {
    Map<TextArea, Map<JEditBuffer, Integer>> maps =
            new WeakHashMap<TextArea, Map<JEditBuffer, Integer>>();

    void set(TextArea textArea) {
        set(textArea, textArea.getCaretPosition());
    }

    void set(TextArea textArea, int anchorOffset) {
        Map<JEditBuffer, Integer> anchorMap = maps.get(textArea);
        if(anchorMap == null) {
            anchorMap = new WeakHashMap<JEditBuffer, Integer>();
            maps.put(textArea, anchorMap);
        }
        anchorMap.put(textArea.getBuffer(), anchorOffset);
    }

    Integer get(TextArea textArea) {
        Map<JEditBuffer, Integer> anchorMap = maps.get(textArea);
        if(anchorMap == null) {
            return null;
        } else {
            return anchorMap.get(textArea.getBuffer());
        }
    }

    void remove(TextArea textArea) {
        Map<JEditBuffer, Integer> anchorMap = maps.get(textArea);
        if(anchorMap != null) {
            anchorMap.remove(textArea.getBuffer());
        }
    }

    void remove(JEditBuffer buffer) {
        for(Map<JEditBuffer, Integer> anchorMap: maps.values()) {
            anchorMap.remove(buffer);
        }
    }

    boolean contains(TextArea textArea) {
        Map<JEditBuffer, Integer> anchorMap = maps.get(textArea);
        if(anchorMap == null) {
            return false;
        } else {
            return anchorMap.containsKey(textArea.getBuffer());
        }
    }
}