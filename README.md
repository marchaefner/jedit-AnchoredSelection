# Anchored Selection Plugin for jEdit

This [jEdit] plugin provides an Anchored Selection Mode where the current selection will expand with caret movements.

The plugin brings:

  * An "Anchored selection" action (`toggle-anchor-select`) which can be bound to a keyboard shortcut.
  * A status bar widget which can be added in Global Options > Status Bar > Widgets.
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
  * when content at the anchor position is deleted, usually by typing, cutting or pasting in the current selection

### Known issues

  * Statusbar widget: Re/Unloading the plugin does not remove the widget or its handlers, i.e. unloading does not actually work. This needs a crafty workaround due to jEdits wanting statusbar implementation.
  * Changing keyboard shortcuts for overridden actions must be done in the overridden action set ("Builtin Commands - anchored selection compatible").

### TODOs
  * general code cleanup
  * add some docs
  * Maybe add options (raise anchor on copy / raise anchor on buffer switch)


[jEdit]: http://jedit.org/
