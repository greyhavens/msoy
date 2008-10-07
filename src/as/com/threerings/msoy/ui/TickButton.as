//
// $Id$

package com.threerings.msoy.ui {

import flash.events.MouseEvent;

import mx.controls.sliderClasses.Slider;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.Msgs;

/**
 * A button that can be associated with a Slider, that jumps to the next tick value.
 */
public class TickButton extends CommandButton
{
    /**
     * Construct a TickButton for the specified slider.
     * Note that a command or callback can still be set on this button.
     */
    public function TickButton (slider :Slider)
    {
        super(null, function () :void {}); // bogus function, in case none other is ever set.
        _slider = slider;

        styleName = "tickButton";
        toolTip = Msgs.GENERAL.get("i.tickButton");
    }

    /**
     * Jump to the next tick value.
     */
    protected function jumpTick () :void
    {
        const ticks :Array = _slider.tickValues;
        if (ticks == null) {
            return;
        }
        const value :Number = _slider.value;
        const dex :int = ticks.indexOf(value);
        if (dex != -1) {
            // we're at a tick, jump to the next tick
            _slider.value = Number(ticks[(dex + 1) % ticks.length]);

        } else {
            // jump to the closest tick
            var closeValue :Number = value;
            var closeness :Number = Number.MAX_VALUE;
            for each (var tickVal :Number in ticks) {
                var diff :Number = Math.abs(tickVal - value);
                if (diff < closeness) {
                    closeness = diff;
                    closeValue = tickVal;
                }
            }
            _slider.value = closeValue;
        }
    }

    override protected function clickHandler (event :MouseEvent) :void
    {
        super.clickHandler(event);
        jumpTick();
    }

    /** The slider we operate on. */
    protected var _slider :Slider;
}
}
