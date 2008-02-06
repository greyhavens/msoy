//
// $Id$

package com.threerings.msoy.ui {

import flash.display.DisplayObject;

import flash.events.MouseEvent;

import mx.containers.TitleWindow;

import mx.controls.Button;
import mx.controls.ButtonBar;

import mx.events.CloseEvent;

import mx.managers.PopUpManager;

import com.threerings.util.CommandEvent;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.Msgs;

public class FloatingPanel extends TitleWindow
{
    /** Button constants. You may define your own in a subclass,
     * start your ids at 100 to be safe (in case more are added here).
     *
     * Note that button ids also dictate the order in which buttons
     * are added to the button bar, so you may add new buttons with negative
     * ids to have them placed to the left of OK.
     */
    public static const OK_BUTTON :int = 0;
    public static const CANCEL_BUTTON :int = 1;

    /**
     * Create a Floating Panel.
     */
    public function FloatingPanel (ctx :MsoyContext, title :String)
    {
        _ctx = ctx;
        this.title = title;

        // Add a listener for the CLOSE event. It's only possible to be dispatched if
        // showCloseButton=true, which we allow subclasses to do with convenience.
        addEventListener(CloseEvent.CLOSE, handleClose);

        // add a listener to handle command events we generate
        addEventListener(CommandEvent.COMMAND, handleCommand);
    }

    /**
     * Pop up this FloatingPanel.
     *
     * @param modal if true, all other UI elements are inactive until this panel is closed.
     * @param parent if non-null, an alternate parent for this panel.  otherwise
     *               _ctx.getRootPanel() will be used.
     * @param avoid if non-null, an object on screen that will not be covered, if possible.
     */
    public function open (modal :Boolean = false, parent :DisplayObject = null,
                          avoid :DisplayObject = null) :void
    {
        if (parent == null) {
            parent = _ctx.getTopPanel();
        }
        _parent = parent;

        // TODO: avoiding

        PopUpManager.addPopUp(this, parent, modal);
        if (avoid == null) {
            PopUpManager.centerPopUp(this);
        }
    }

    /**
     * This property is true if the floating panel is currently open.
     */
    public function get isOpen () :Boolean
    {
        return _parent != null;
    }
    
    /**
     * Close this FloatingPanel.
     */
    public function close () :void
    {
        _parent = null;
        PopUpManager.removePopUp(this);
    }

    /**
     * A convenience function to add a button bar containing the specified button ids. You probably
     * want to call this at the bottom of your createChildren() method. Note that this is just for
     * standard buttons in the button bar. You can certainly add your own buttons elsewhere.
     *
     * TODO: consider instead creating a content area and a button area in createChildren() and
     * letting subclasses just populate those.
     */
    public function addButtons (... buttonIds) :void
    {
        buttonIds.sort(Array.NUMERIC);

        var butBox :ButtonBar = new ButtonBar();
        for each (var buttonId :int in buttonIds) {
            var but :Button = createButton(buttonId);

            // if not a CommandButton, add our own event handling...
            if (!(but is CommandButton)) {
                addListener(but, buttonId);
            }
            butBox.addChild(but);

            // store the button for later retrieval
            _buttons[buttonId] = but;
            // if we're showing a standard cancel button, also add the close "X"
            if (buttonId == CANCEL_BUTTON) {
                showCloseButton = true;
            }
        }

        addChild(butBox);
    }

    /**
     * Return the specified button, or null.
     */
    protected function getButton (buttonId :int) :Button
    {
        return (_buttons[buttonId] as Button);
    }

    /**
     * Function to add a listener to the buttn. We cannot inline this function because otherwise
     * the bound function in here will retain the latest value of buttonId.
     */
    protected function addListener (but :Button, buttonId :int) :void
    {
        but.addEventListener(MouseEvent.CLICK, function (evt :MouseEvent) :void {
            evt.stopImmediatePropagation();
            buttonClicked(buttonId);
        });
    }

    /**
     * Called to add a button to the button box at the bottom of the panel.  If this method returns
     * a CommandButton, the standard buttonClicked method will not be used.
     */
    protected function createButton (buttonId :int) :Button
    {
        var label :String;
        switch (buttonId) {
        case CANCEL_BUTTON:
            label = Msgs.GENERAL.get("b.cancel");
            break;

        case OK_BUTTON:
            label = Msgs.GENERAL.get("b.ok");
            break;

        default:
            throw new ArgumentError("Unknown buttonId: " + buttonId);
        }

        return new CommandButton(label, buttonClicked, buttonId);
    }

    /**
     * Called when a button has been pressed. The default implementation will automatically close
     * the panel if OK or CANCEL are clicked.
     */
    protected function buttonClicked (buttonId :int) :void
    {
        switch (buttonId) {
        case OK_BUTTON: // fall through to cancel
        case CANCEL_BUTTON:
            close();
            break;

        default:
            throw new ArgumentError("No button action [buttonId=" + buttonId + "]");
        }
    }

    /**
     * Handles CommandEvents. By default we shuttle the command to our real parent.
     */
    protected function handleCommand (event :CommandEvent) :void
    {
        if (_parent != null) {
            event.markAsHandled();
            // redispatch a new event...
            CommandEvent.dispatch(_parent, event.command, event.arg);
        }
    }

    /**
     * A small function to handle the event generated by the little 'X' close button.
     */
    private function handleClose (event :CloseEvent) :void
    {
        buttonClicked(CANCEL_BUTTON);
    }

    /** Provides client services. */
    protected var _ctx :MsoyContext;

    /** An associative hash mapping buttonId to Button. */
    protected var _buttons :Object = new Object();

    /** The component that is the host of this dialog. */
    protected var _parent :DisplayObject;
}
}
