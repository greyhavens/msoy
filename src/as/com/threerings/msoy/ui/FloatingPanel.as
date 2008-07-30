//
// $Id$

package com.threerings.msoy.ui {

import flash.display.DisplayObject;

import flash.events.MouseEvent;

import mx.containers.TitleWindow;

import mx.controls.Button;
import mx.controls.ButtonBar;

import mx.core.mx_internal; // namespace
import mx.core.UIComponent;

import mx.events.CloseEvent;

import mx.managers.PopUpManager;

import com.threerings.util.ArrayUtil;
import com.threerings.util.CommandEvent;
import com.threerings.util.Log;

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
     * ids to have them placed to the left of cancel.
     */
    public static const CANCEL_BUTTON :int = 0;
    public static const OK_BUTTON :int = int.MAX_VALUE;

    public static const DEFAULT_BUTTON_WIDTH :int = 72;

    /**
     * Create a Floating Panel.
     */
    public function FloatingPanel (ctx :MsoyContext, title :String = "")
    {
        _ctx = ctx;
        this.title = title;

        // Add a listener for the CLOSE event. It's only possible to be dispatched if
        // showCloseButton=true, which we allow subclasses to do with convenience.
        addEventListener(CloseEvent.CLOSE, handleClose);

        // add a listener to handle command events we generate (we use priority -1 so that if a
        // controller is listening to this panel directly, it will get the event first)
        addEventListener(CommandEvent.COMMAND, handleCommand, false, -1);
    }

    /**
     * Pop up this FloatingPanel.
     *
     * @param modal if true, all other UI elements are inactive until this panel is closed.
     * @param parent if non-null, an alternate parent for this panel.  otherwise
     *               _ctx.getRootPanel() will be used.
     * @param boolean center if true, center.
     */
    public function open (
        modal :Boolean = false, parent :DisplayObject = null, center :Boolean = true) :void
    {
        // fall back to the top panel if no explicit parent was provided
        _parent = (parent == null) ? _ctx.getTopPanel() : parent;

        PopUpManager.addPopUp(this, _parent, modal);
        if (center) {
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
     * A convenience function to add a button bar containing the specified button ids or 
     * already instantiated Button objects (CommandButtons, probably). You probably
     * want to call this at the bottom of your createChildren() method. Note that this is just for
     * standard buttons in the button bar. You can certainly add your own buttons elsewhere.
     *
     * Any already instantiated buttons will be given a button value of 100, but their
     * relative order will not change.
     *
     * TODO: consider instead creating a content area and a button area in createChildren() and
     * letting subclasses just populate those.
     */
    public function addButtons (... buttonSources) :void
    {
        if (_buttonBar == null) {
            // only set to the default width if no width has already been set
            setButtonWidth(DEFAULT_BUTTON_WIDTH);
        }
        if (_buttonBar.parent == null) {
            addChild(_buttonBar);
        }

        ArrayUtil.stableSort(buttonSources, buttonSort);
        for each (var source :Object in buttonSources) {
            if (source is Button) {
                _buttonBar.addChild(source as Button);
                continue;
            }

            var buttonId :int = source as int;
            var but :Button = createButton(buttonId);
            // if not a CommandButton, add our own event handling...
            if (!(but is CommandButton)) {
                addListener(but, buttonId);
            }
            _buttonBar.addChild(but);

            // store the button for later retrieval
            _buttons[buttonId] = but;
            // if we're showing a standard cancel button, also add the close "X"
            if (buttonId == CANCEL_BUTTON) {
                showCloseButton = true;
            }
        }
    }

    /**
     * Set the width used by all buttons, or 0 to clear the default width.
     */
    public function setButtonWidth (width :int) :void
    {
        if (_buttonBar == null) {
            _buttonBar = new ButtonBar();
            _buttonBar.percentWidth = 100;
        }
        if (width > 0) {
            _buttonBar.setStyle("buttonWidth", width);
        } else {
            _buttonBar.clearStyle("buttonWidth");
        }
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
            Log.getLog(this).info("Forwarding " + event);
            event.markAsHandled();
            // redispatch a new event...
            CommandEvent.dispatch(_parent, event.command, event.arg);
        } else {
            Log.getLog(this).info("Not forwarding " + event);
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var closeBtn :UIComponent = mx_internal::closeButton;
        closeBtn.explicitWidth = 13;
        closeBtn.explicitHeight = 14;
    }

    override protected function layoutChrome (unscaledWidth :Number, unscaledHeight :Number) :void
    {
        super.layoutChrome(unscaledWidth, unscaledHeight);

        var closeBtn :UIComponent = mx_internal::closeButton;
        closeBtn.x = unscaledWidth - closeBtn.width - 5;
        closeBtn.y = 5;
    }

    /**
     * A small function to handle the event generated by the little 'X' close button.
     */
    private function handleClose (event :CloseEvent) :void
    {
        buttonClicked(CANCEL_BUTTON);
    }

    /** Handles sorting the buttons to be added. */
    protected function buttonSort (o1 :Object, o2 :Object) :int
    {
        var v1 :Number = (o1 is Number) ? (o1 as Number) : 100;
        var v2 :Number = (o2 is Number) ? (o2 as Number) : 100;

        if (v1 > v2) {
            return 1;
        } else if (v1 < v2) {
            return -1;
        } else {
            return 0;
        }
    }

    /** Provides client services. */
    protected var _ctx :MsoyContext;

    /** The button bar. */
    protected var _buttonBar :ButtonBar;

    /** An associative hash mapping buttonId to Button. */
    protected var _buttons :Object = new Object();

    /** The component that is the host of this dialog. */
    protected var _parent :DisplayObject;
}
}
