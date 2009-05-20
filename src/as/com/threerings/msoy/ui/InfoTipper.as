//
// $Id$

package com.threerings.msoy.ui {

import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.controls.ToolTip;
import mx.events.ToolTipEvent;
import mx.managers.ToolTipManager;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.Msgs;

/**
 * The style name of the "class" style name of the tip that pops up. The default, if undefined,
 * is "infoTip", meaning it will look for the css style named ".infoTip". That's a
 * "class selector", but when a style is named after the class of the component, that's called
 * a "type selector". I'm just the messenger, but someone does need shooting.
 *
 * @default "infoTip"
 */
[Style(name="tipStyle", type="String", inherit="no")]

/**
 * A standardized button-looking thing that instantly pops up a tooltip-like thing
 * when the mouse is hovered on it.
 */
public class InfoTipper extends HBox
{
    /**
     * Create an InfoTipper, optionally specifying the starter tip.
     */
    public function InfoTipper (tip :String = null)
    {
        _btn = new CommandButton(Msgs.GENERAL.get("b.info"), sink);
        _btn.styleName = "orangeButton";
        _btn.scaleY = .8;
        _btn.addEventListener(MouseEvent.ROLL_OVER, handleRoll);
        _btn.addEventListener(MouseEvent.ROLL_OUT, handleRoll);
        _btn.addEventListener(ToolTipEvent.TOOL_TIP_SHOW, handleTipShow);
        addChild(_btn);
        setTip(tip);
    }

    /**
     * Set the tip String to be shown when the user hovers over this widget.
     */
    public function setTip (tip :String) :void
    {
        // We use "errorString" instead of "toolTip" to make an "error" tooltip, which
        // doesn't go away when clicked. We style this tip in the show handler, so it
        // doesn't end up looking like a an error. Lordy.
        _btn.errorString = tip;
    }

    /**
     * Does nothing. A target for the commandButton.
     */
    protected function sink () :void
    {
        // de nada
    }

    /**
     * Handle the mouse rolling over the tipping button.
     */
    protected function handleRoll (event :MouseEvent) :void
    {
        const show :Boolean = (event.type == MouseEvent.ROLL_OVER);
        // when we hover over the _infoTipper, show tips immediately, else: restore normal time
        ToolTipManager.showDelay = show ? 0 : 500;
        ToolTipManager.hideDelay = show ? Infinity : 10000;
        _btn.enabled = !show;
    }

    protected function handleTipShow (event :ToolTipEvent) :void
    {
        var tip :ToolTip = ToolTip(event.toolTip);
        tip.styleName = getStyle("tipStyle") || "infoTip";
        // text must be jiggled, otherwise the tip won't size properly after the style change
        // fawking flex
        tip.text = tip.text; // this is enough jiggling
        tip.validateNow();
    }

    /** The button what makes the tips. */
    protected var _btn :CommandButton;
}
}
