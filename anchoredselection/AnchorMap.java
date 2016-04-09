// :indentSize=4:tabSize=4:noTabs=true:folding=explicit:
package anchoredselection;

// {{{ Imports
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.textarea.TextArea;

import javax.swing.text.Position;
import java.util.Map;
import java.util.WeakHashMap;
// }}}

class AnchorMap {
    Map<TextArea, Map<JEditBuffer, Position>> maps =
            new WeakHashMap<TextArea, Map<JEditBuffer, Position>>();

    void set(TextArea textArea) {
        set(textArea, textArea.getCaretPosition());
    }

    void set(TextArea textArea, int anchorOffset) {
        Map<JEditBuffer, Position> anchorMap = maps.get(textArea);
        if(anchorMap == null) {
            anchorMap = new WeakHashMap<JEditBuffer, Position>();
            maps.put(textArea, anchorMap);
        }
        JEditBuffer buffer = textArea.getBuffer();
        anchorMap.put(buffer, buffer.createPosition(anchorOffset));
    }

    Integer get(TextArea textArea) {
        Map<JEditBuffer, Position> anchorMap = maps.get(textArea);
        if(anchorMap != null) {
            Position anchor = anchorMap.get(textArea.getBuffer());
            if(anchor != null) {
                return anchor.getOffset();
            }
        }
        return null;
    }

    void remove(TextArea textArea) {
        Map<JEditBuffer, Position> anchorMap = maps.get(textArea);
        if(anchorMap != null) {
            anchorMap.remove(textArea.getBuffer());
        }
    }

    void remove(JEditBuffer buffer, int offset, int length) {
        for(Map<JEditBuffer, Position> anchorMap: maps.values()) {
            if(!anchorMap.containsKey(buffer)) {
                continue;
            }
            int anchor = anchorMap.get(buffer).getOffset();
            if(offset <= anchor && offset + length >= anchor) {
                anchorMap.remove(buffer);
            }
        }
    }

    boolean contains(TextArea textArea) {
        Map<JEditBuffer, Position> anchorMap = maps.get(textArea);
        if(anchorMap == null) {
            return false;
        } else {
            return anchorMap.containsKey(textArea.getBuffer());
        }
    }

    boolean contains(JEditBuffer buffer) {
        for(Map<JEditBuffer, Position> anchorMap: maps.values()) {
            if(anchorMap.containsKey(buffer)) {
                return true;
            }
        }
        return false;
    }
}
