package com.threerings.msoy.export {

import flash.display.DisplayObject;

import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.TextEvent;

/**
 * This file should be included by furniture, so that it can communicate
 * with the whirled.
 */
public class FurniControl extends EntityControl
{
    /**
     * Create a furni interface. The display object is your piece
     * of furni.
     */
    public function FurniControl (disp :DisplayObject)
    {
        super(disp);
    }

    override protected function isAbstract () :Boolean
    {
        return false;
    }
}
}
