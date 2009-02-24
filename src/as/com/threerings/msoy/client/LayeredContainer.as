//
// $Id$

package com.threerings.msoy.client {

import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.geom.Matrix;

import flash.utils.Dictionary;

import mx.core.Container;
import mx.core.UIComponent;

import com.threerings.flex.FlexWrapper;
import com.threerings.util.Log;

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
    implements Snapshottable
{
    public const log :Log = Log.getLog(this);

    public function setBaseLayer (base :DisplayObject) :void
    {
        clearBaseLayer();
        addChildAt(_base = wrap(base), 0);
//         log.debug("Base layer set [base=" + base + "]");
    }

    public function clearBaseLayer () :void
    {
        if (_base != null) {
            removeChild(_base);
//             log.debug("Base layer cleared [base=" + _base + "]");
            _base = null;
        }
    }

    // from interface Snapshottable
    public function snapshot (
        bitmapData :BitmapData, matrix :Matrix, childPredicate :Function = null) :Boolean
    {
        return SnapshotUtil.snapshot(this, bitmapData, matrix,
            // enhance the predicate to avoid snapping the base
            function (disp :DisplayObject) :Boolean {
                return (disp != _base) && (childPredicate == null || childPredicate(disp));
            });
    }

    /**
     * Adds a display object to overlay the main view as it changes. The lower the layer argument,
     * the lower the overdraw priority the layer has among other layers. The supplied DisplayObject
     * must have a name and it mustn't conflict with any other overlay name. Fortunately if you
     * don't name your display object it will be assigned a unique name.
     */
    public function addOverlay (overlay :DisplayObject, layer :int) :void
    {
        _layers[overlay] = layer;

        // step through the children until we find one whose layer is larger than ours
        for (var ii :int = 0; ii < numChildren; ii++) {
            if (getLayer(getChildAt(ii)) > layer) {
                addChildAt(wrap(overlay), ii);
                return;
            }
        }

        // if no such child found, just append
        addChild(wrap(overlay));
    }

    /**
     * Removes a previously added overlay.
     */
    public function removeOverlay (overlay :DisplayObject) :void
    {
        delete _layers[overlay];

        // remove this child from the display the hard way
        for (var ii :int = 0; ii < numChildren; ii++) {
            var child :DisplayObject = unwrap(getChildAt(ii));
            if (child == overlay) {
                child = removeChildAt(ii);
                if (child is FlexWrapper) {
                    (child as FlexWrapper).removeChildAt(0);
                }
                break;
            }
        }
    }

    public function containsOverlay (overlay :DisplayObject) :Boolean
    {
        return (unwrap(overlay) in _layers);
    }

    /**
     * Return the layer of the specified overlay, or 0 if it's not present.
     */
    public function getLayer (overlay :DisplayObject) :int
    {
        return int(_layers[unwrap(overlay)]);
    }

    protected function wrap (object :DisplayObject) :DisplayObject
    {
        return (object is UIComponent) ? object : new FlexWrapper(object);
    }

    protected function unwrap (object :DisplayObject) :DisplayObject
    {
        return (object is FlexWrapper) ? (object as FlexWrapper).getChildAt(0) : object;
    }

    /** A mapping of overlays to the numerical layer priority at which they were added. */
    protected var _layers :Dictionary = new Dictionary(true);

    protected var _base :DisplayObject;
}
}
