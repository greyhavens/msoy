//
// $Id: PlaceBox.as 6357 2007-10-25 16:24:55Z zell $

package com.threerings.msoy.client {

import flash.display.DisplayObject;

import mx.core.Container;
import mx.core.UIComponent;

/**
 * Provide an organized way for callers to layer display objects onto one another at
 * different priority levels (which they will have to work out amongst themselves).
 *
 * This is by no means foolproof and calls can easily be made directly to the Container
 * we extend; it's still an improvement on separate pieces of our code base remotely
 * fiddling with rawChildren and competing for the top spot.
 *
 * TODO: We may want to remove the Base Layer concept and replace it with explicitly
 * prioritized layers.
 */
public class LayeredContainer extends Container
{
    public function setBaseLayer (base :DisplayObject) :void
    {
        clearBaseLayer();
        _base = base;

        if (base is UIComponent) {
            addChildAt(base, 0);
        } else {
            rawChildren.addChildAt(base, 0);
        }
        Log.getLog(this).debug("Base layer set [base=" + base + ", parent=" + base.parent + "]");
    }

    public function clearBaseLayer () :void
    {
        if (_base != null) {
            if (_base is UIComponent) {
                removeChild(_base);
            } else {
                rawChildren.removeChild(_base);
            }
            Log.getLog(this).debug("Base layer cleared [base=" + _base + "]");
            _base = null;
        }
    }

    /**
     * Adds a display object to overlay the main view as it changes. The lower the layer
     * argument, the lower the overdraw priority the layer has among other layers.
     */
    public function addOverlay (overlay :DisplayObject, layer :int) :void
    {
        _layers[overlay] = layer;
        // step through the children until we find one whose layer is larger than ours
        for (var ii :int = 0; ii < this.rawChildren.numChildren; ii ++) {
            var child :DisplayObject = this.rawChildren.getChildAt(ii);

            var childLayer :int = _layers[child] as int;
            if (childLayer > layer) {
                this.rawChildren.addChildAt(overlay, ii);
                return;
            }
        }
        // if no such child found, just append
        this.rawChildren.addChild(overlay);
    }

    /**
     * Removes a previously added overlay.
     */
    public function removeOverlay (overlay :DisplayObject) :void
    {
        if (_layers[overlay]) {
            _layers[overlay] = null;
        } else {
            Log.getLog(this).warning("removing unknown overlay [overlay=" + overlay + "]");
            // but I guess we'll remove it anyway
        }
        this.rawChildren.removeChild(overlay);
    }


    /** A mapping of overlays to the numerical layer priority at which they were added. */
    protected var _layers :Object = new Object();

    protected var _base :DisplayObject;
}
}
