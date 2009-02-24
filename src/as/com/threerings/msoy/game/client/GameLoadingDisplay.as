//
// $Id$

package com.threerings.msoy.game.client {

import caurina.transitions.Tweener;


import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.PlaceLoadingDisplay;

import com.threerings.msoy.ui.ScalingMediaContainer;

public class GameLoadingDisplay extends PlaceLoadingDisplay 
{
    public function GameLoadingDisplay (box :PlaceBox, logo :ScalingMediaContainer)
    {
        super(box);
        
        // add our loading image behind everything else
        addChildAt(_logo = logo, 0);
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

        // tell the logo to fade in
        Tweener.addTween(_logo, { alpha: 1, time: 1, delay: 1, transition: "easeinsine" });
    }
    
    /** 
     * Called to perform a transition away from static loading, 
     * eg. by sliding the loader out of view. 
     */
    override protected function doTransitionOut () :void
    {
        // note: do not call super; we leave the spinner as is
        Tweener.removeTweens(_spinner, "x", "y");
    }

    protected var _logo :ScalingMediaContainer;
}
}
