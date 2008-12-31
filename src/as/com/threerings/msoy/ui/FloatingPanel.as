//
// $Id$

package com.threerings.msoy.ui {

import flash.display.DisplayObject;

import flash.events.Event;
import flash.events.MouseEvent;
import flash.events.TextEvent;

import mx.containers.TitleWindow;
import mx.containers.VBox;

import mx.controls.Button;
import mx.controls.ButtonBar;

import mx.core.mx_internal; // namespace
import mx.core.UIComponent;

import mx.events.CloseEvent;

import mx.managers.PopUpManager;

import com.threerings.util.ArrayUtil;
import com.threerings.util.CommandEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.PopUpUtil;

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

    /** Padding, used for various things, like the space between popups. */
    public static const PADDING :int = 10;

    /**
     * A convenience function for all FloatingPanels, to track up/down state and do the
     * right thing.
     *
     * @param createFn a no-arg function that creates (and populates?) your FloatingPanel
     * @param toggleButton option, use only if your button displays a toggle state for
     * the popup, so that it can be updated when the popup is popped down via another path
     * than the button..
     *
     * @return a Function that can be called to pop up or down the panel. Useful for attaching
     * to CommandButtons, etc.
     */
    public static function createPopper (createFn :Function, srcButton :Button = null) :Function
    {
        var thePanel :FloatingPanel;
        // let's try to automatically pop the dialog down if the srcButton is disposed of
        if (srcButton != null) {
            srcButton.addEventListener(Event.REMOVED_FROM_STAGE, function (... ignored) :void {
                if (thePanel != null && thePanel.isOpen()) {
                    thePanel.close();
                }
            });
        }
        return function (... args) :void {
            // if the srcButton is non-null, see if it's a toggle and see if the selected
            // value matches the pop state, if so, do nothing, as the pop-state could be
            // being adjusted as a result of the panel closing!
            if (srcButton != null && srcButton.toggle &&
                    srcButton.selected == (thePanel != null)) {
                return;
            }
            // otherwise, pop it up or down
            if (thePanel == null) {
                thePanel = createFn();
                // TODO: change closeCallback to an event dispatch, so that more than one may
                // be set and our callback can't be clobbered.
                thePanel.setCloseCallback(function () :void {
                    thePanel = null;
                    if (srcButton != null) {
                        // deselect the button. Should only be needed for toggle buttons, but
                        // can't hurt
                        srcButton.selected = false;
                    }
                });
                // auto-open the panel if it's not already
                if (!thePanel.isOpen()) {
                    thePanel.open();
                }
            } else {
                thePanel.close();
                // that will call our close callback, nulling out 'thePanel'
            }
        };
    }

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
    }

    /**
     * Set the callback that will be called when this dialog closes.
     */
    public function setCloseCallback (closeCallback :Function) :void
    {
        // TODO?: to be even more correct, we should perhaps call this callback when the
        // dialog is removed from its parent, even if done via some other mechanism
        _closeCallback = closeCallback;
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
        // bridge CommandEvents and LINK events out to the parent
        CommandEvent.configureBridge(this, _parent);
        addEventListener(TextEvent.LINK, _parent.dispatchEvent);

        didOpen();
    }

    /**
     * This property is true if the floating panel is currently open.
     */
    public function isOpen () :Boolean
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
        if (_closeCallback != null) {
            _closeCallback();
        }
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
            var box :VBox = new VBox();
            box.percentWidth = 100;
            box.setStyle("horizontalAlign", "right");
            box.setStyle("paddingRight", 5);
            box.setStyle("paddingBottom", 5);
            box.addChild(_buttonBar);
            addChild(box);
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
        return new CommandButton(getButtonLabel(buttonId), buttonClicked, buttonId);
    }

    /**
     * Overridable method to get the label for a button.
     */
    protected function getButtonLabel (buttonId :int) :String
    {
        switch (buttonId) {
        case CANCEL_BUTTON:
            return Msgs.GENERAL.get("b.cancel");

        case OK_BUTTON:
            return Msgs.GENERAL.get("b.ok");

        default:
            throw new ArgumentError("Unknown buttonId: " + buttonId);
        }
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

    override protected function createChildren () :void
    {
        super.createChildren();

        var closeBtn :UIComponent = getCloseButton();
        closeBtn.buttonMode = true;
        closeBtn.explicitWidth = 13;
        closeBtn.explicitHeight = 14;
    }

    override protected function layoutChrome (unscaledWidth :Number, unscaledHeight :Number) :void
    {
        super.layoutChrome(unscaledWidth, unscaledHeight);

        var closeBtn :UIComponent = getCloseButton();
        closeBtn.x = unscaledWidth - closeBtn.width - 5;
        closeBtn.y = 5;
    }

    /**
     * Provides easy access to the close button.
     */
    protected function getCloseButton () :Button
    {
        return mx_internal::closeButton;
    }

    /**
     * A small function to handle the event generated by the little 'X' close button.
     */
    protected function handleClose (event :CloseEvent) :void
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

    /**
     * Handles any extra tasks after opening the panel.
     */
    protected function didOpen () :void
    {
        PopUpUtil.avoidOtherPopups(this, _ctx.getPlaceViewBounds(), PADDING);
    }

    /** Provides client services. */
    protected var _ctx :MsoyContext;

    /** Called when we're closed. */
    protected var _closeCallback :Function;

    /** The button bar. */
    protected var _buttonBar :ButtonBar;

    /** An associative hash mapping buttonId to Button. */
    protected var _buttons :Object = new Object();

    /** The component that is the host of this dialog. */
    protected var _parent :DisplayObject;
}
}
