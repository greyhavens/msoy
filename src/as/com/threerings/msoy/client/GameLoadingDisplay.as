//
// $Id$

package com.threerings.msoy.client {

import flash.display.LoaderInfo;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.ErrorEvent;
import flash.events.IEventDispatcher;
import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;

import flash.utils.Dictionary;

import caurina.transitions.Tweener;

import mx.events.ResizeEvent;

import com.threerings.msoy.client.PlaceBox;

import com.threerings.msoy.ui.LoadingSpinner;
import com.threerings.msoy.ui.ScalingMediaContainer;

public class GameLoadingDisplay extends PlaceLoadingDisplay 
{
    public function GameLoadingDisplay (box :PlaceBox, logo :ScalingMediaContainer)
    {
        super(box);
        addChild(_logo = logo);
        _logo.alpha = 0;
    }

    // from PlaceLoadingDisplay 
    override protected function doStaticLayout () :void
    {
        super.doStaticLayout();

        // tell the spinner to tween out to the corner        
        Tweener.addTween(_spinner, 
            { x: 20, y: 20, time: 1, transition: "easeoutcubic" });

        // start the logo in the middle of the screen        
        _logo.x = (_box.width - _logo.maxW) / 2;
        _logo.y = (_box.height - _logo.maxH) / 2;

        // tell the logo to slowly fade in
        Tweener.addTween(_logo, { alpha: 1, time: 1, delay: 1, transition: "easeincubic" });
    }
    
    /** 
     * Called to perform a transition away from static loading, 
     * eg. by sliding the loader out of view. 
     */
    override protected function doTransitionOut () :void
    {
        // note: do not call super; we leave the spinner as is
         
        // just fade the logo out   
        Tweener.addTween(_logo, { alpha: 0, time: 1, transition: "easeoutcubic" }); 
    }

    protected var _logo :ScalingMediaContainer;
}
}
