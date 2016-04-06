# Anchored Selection Plugin for jEdit

This [jEdit] plugin provides an Anchored Selection Mode where the current selection will expand with caret movements.

The plugin brings:

  * An "Anchored selection" action (`toggle-anchor-select`) which can be bound to a keyboard shortcut.
  * A status bar widget which can be added in Global Options > Status Bar > Widgets (requires jEdit 5).
  * An API for macros:
      * `anchoredselection.AnchoredSelectionPlugin.isAnchored(textArea)`
      * `anchoredselection.AnchoredSelectionPlugin.toggleAnchor(view)`
      * `anchoredselection.AnchoredSelectionPlugin.dropAnchor(view)`
      * `anchoredselection.AnchoredSelectionPlugin.raiseAnchor(view)`
    (These may change their names/location in the foreseeable future.)

In anchored selection mode the selection will extend from the anchor to the current caret position. The caret may be moved by keyboard, mouse, search or for any other reason.

Anchored selection mode ends automatically:

  * for any other (incompatible) select action ("Select None" `ESCAPE`, "Select All" `S+a`, "Select Fold" `C+e w`, etc.)
  * on copy actions ("Copy" `C+c`, "Copy Append" `C+e C+a`, etc.)
  * when the content of the buffer changes, e.g. by typing, cutting or pasting

### Known issues

  * Rectangular selection mode: Does not work as would be expected - selection loses virtual width, i.e. it can only be as wide as the actual content of the caret line.
  * Rectangular selection mode: Clicking with the mouse outside of actual content inserts whitespace which ends the anchored selection mode prematurely.
  * Statusbar widget: Re/Unloading the plugin does not remove the widget or its handlers, i.e. unloading does not actually work. This needs a crafty workaround due to jEdits wanting statusbar implementation.
  * Changing keyboard shortcuts for overridden actions must be done in the overridden action set ("Builtin Commands - anchored selection compatible").
  * Statusbar widget: Does not appear anywhere in jEdit 4.5.2 - needs investigation.

### TODOs
  * general code cleanup
  * add some docs
  * Extend `contentInserted/Removed` to only end anchored selection mode when the selection content has changed. If the the buffer changed above the selection adjust the anchor position. (This should also fix mouse clicking in rectangular selection mode.)
  * Try to be much smarter about virtual width/columns in rectangular selection mode. Since all related functionality seems to be private/protected in textarea this means overriding more actions.
  * Maybe add options (raise anchor on copy / raise anchor on buffer switch)


[jEdit]: http://jedit.org/
