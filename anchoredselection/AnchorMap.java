// :indentSize=4:tabSize=4:noTabs=true:folding=explicit:
package anchoredselection;

// {{{ Imports
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.textarea.TextArea;

import javax.swing.text.Position;
import java.util.Map;
import java.util.WeakHashMap;
// }}}

/**
 *  A map of anchor positions.
 *
 *  An anchor is a position in a buffer in a text area (which is part of an edit
 *  pane). Specifically, a buffer can be held by multiple text areas at the same
 *  time.
 *
 *  This class provides simple interfaces for handling either the current buffer
 *  of a given text area or a buffer in all text areas.
 *
 *  Anchors are stored as javax.swing.text.Position which are generated and
 *  updated (upon content changes) by TextArea.
 */
class AnchorMap {
    private Map<TextArea, Map<JEditBuffer, Position>> bufferMaps =
            new WeakHashMap<TextArea, Map<JEditBuffer, Position>>();

    // {{{ set methods

    /** Set the current caret position of the current buffer of textArea as the
     *  new anchor. */
    void set(TextArea textArea) {
        set(textArea, textArea.getCaretPosition());
    }

    /** Set anchor of the current buffer of textArea. */
    void set(TextArea textArea, int anchorOffset) {
        Map<JEditBuffer, Position> anchorMap = bufferMaps.get(textArea);
        if(anchorMap == null) {
            anchorMap = new WeakHashMap<JEditBuffer, Position>();
            bufferMaps.put(textArea, anchorMap);
        }
        JEditBuffer buffer = textArea.getBuffer();
        anchorMap.put(buffer, buffer.createPosition(anchorOffset));
    }
    // }}}

    // {{{ get method

    /** @return anchor position of the current buffer of textArea */
    Integer get(TextArea textArea) {
        Map<JEditBuffer, Position> anchorMap = bufferMaps.get(textArea);
        if(anchorMap != null) {
            Position anchor = anchorMap.get(textArea.getBuffer());
            if(anchor != null) {
                return anchor.getOffset();
            }
        }
        return null;
    }
    // }}}

    // {{{ remove methods

    /** Delete anchor position of the current buffer of textArea, */
    void remove(TextArea textArea) {
        Map<JEditBuffer, Position> anchorMap = bufferMaps.get(textArea);
        if(anchorMap != null) {
            anchorMap.remove(textArea.getBuffer());
        }
    }

    /** Delete the anchor positions of buffer in all text areas if they are
     *  within the given range. */
    void remove(JEditBuffer buffer, int offset, int length) {
        for(Map<JEditBuffer, Position> anchorMap: bufferMaps.values()) {
            if(!anchorMap.containsKey(buffer)) {
                continue;
            }
            int anchor = anchorMap.get(buffer).getOffset();
            if(offset <= anchor && offset + length >= anchor) {
                anchorMap.remove(buffer);
            }
        }
    }
    // }}}

    // {{{ contains method

    /** @return whether the map contains an anchor position for the current
     *  buffer of textArea. */
    boolean contains(TextArea textArea) {
        return bufferMaps.containsKey(textArea) &&
                bufferMaps.get(textArea).containsKey(textArea.getBuffer());
    }

    /** @return whether the map contains an anchor position for buffer in any
     *  textArea. */
    boolean contains(JEditBuffer buffer) {
        for(Map<JEditBuffer, Position> anchorMap: bufferMaps.values()) {
            if(anchorMap.containsKey(buffer)) {
                return true;
            }
        }
        return false;
    }
    // }}}
}
