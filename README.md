# Anchored Selection Plugin for jEdit

This [jEdit] plugin provides an Anchored Selection Mode where the current selection will expand with caret movements.

The plugin brings:

  * An "Anchored selection" action (`toggle-anchor-select`) which can be bound to a keyboard shortcut.
  * A status bar widget which can be added in Global Options > Status Bar > Widgets.
  * An API for macros:
      * `anchoredselection.Actions.isAnchoredSelectionEnabled(view)`
      * `anchoredselection.Actions.setAnchoredSelectionEnabled(view, boolean)`
      * `anchoredselection.Actions.toggleAnchoredSelectionEnabled(view)`

In anchored selection mode the selection will extend from the anchor to the current caret position. The caret may be moved by keyboard, mouse, search or for any other reason.

Anchored selection mode ends automatically:

  * for any other (incompatible) select action ("Select None" `ESCAPE`, "Select All" `S+a`, "Select Fold" `C+e w`, etc.)
  * on copy actions ("Copy" `C+c`, "Copy Append" `C+e C+a`, etc.)
  * when content at the anchor position is deleted, usually by typing, cutting or pasting in the current selection

### TODOs
  * general code cleanup
  * add some docs
  * Maybe add options (raise anchor on copy / raise anchor on buffer switch)


[jEdit]: http://jedit.org/
