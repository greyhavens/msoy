//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;    
import flash.events.MouseEvent;
import flash.geom.Rectangle;

import mx.binding.utils.BindingUtils;
import mx.containers.Canvas;
import mx.controls.VSlider;
import mx.core.Application;
import mx.core.IFlexDisplayObject;
import mx.core.ScrollPolicy;


/** Background skin to be loaded from the style sheet. */
[Style(name="backgroundSkin", type="Class", inherit="no")]

    
/**
 * Helper UI element for the volume control.
 *
 * Volume control has "weak singleton" semantics - an instance can be created with
 * the 'new' keyword, but if any other instance previously existed, it will be closed
 * and removed from display prior to displaying the new one.
 */
public class ZoomPopup extends Canvas
{
    /** Constructor. */
    public function ZoomPopup (trigger :DisplayObject)
    {
        owner = DisplayObjectContainer(Application.application.systemManager);

        styleName = "zoomControl";

        // Initialize the window
        var r :Rectangle = trigger.getBounds(trigger.stage);
        width = 29;
        height = 100;
        x = r.x - 1;
        y = r.y - height;
        verticalScrollPolicy = horizontalScrollPolicy = ScrollPolicy.OFF;
        
        // Initialize slider
        _slider = new VSlider();
        _slider.x = 4;
        _slider.y = 10;
        _slider.height = 80;
        _slider.minimum = 0;
        _slider.maximum = 1;
        _slider.liveDragging = true;

        _slider.value = Prefs.getZoom();
        BindingUtils.bindSetter(Prefs.setZoom, _slider, "value");

        addChild(_slider);
    }

    /** Show the popup, by adding it to the application's display list,
     *  and register for appropriate events. */
    public function show () :void
    {
        destroyCurrentInstance();

        owner.addChild(this);
        addEventListener(MouseEvent.ROLL_OUT, mouseOutHandler, false, 0, true);
        
        // Setting the skin happens after adding to the parent's draw list -
        // this ensures styles are properly loaded
        var cls :Class = getStyle("backgroundSkin");
        setStyle("backgroundImage", cls);

        _currentInstance = this;
    }

    /** Makes the pop-up invisible, but doesn't remove it. */
    public function hide () :void
    {
        visible = false;
    }

    /** Remove the pop-up from the display list, and unregister
     *  from any events. This should make the object ready to be GC'd,
     *  if there are no external references holding it. */
    public function destroy () :void
    {
        owner.removeChild(this);
        removeEventListener(MouseEvent.ROLL_OUT, mouseOutHandler, false);
        _currentInstance = null;
    }

    /** If an instance exists, hide it. */
    public static function destroyCurrentInstance () :void
    {
        if (_currentInstance != null) {
            _currentInstance.destroy();
        }
    }

    /** Returns true if a visible instance of this window exists. */
    public static function popupExists () :Boolean
    {
        return (_currentInstance != null && _currentInstance.visible);
    }

    // EVENT HANDLERS

    /** Watch for the mouse leaving the area. */
    private function mouseOutHandler (event :MouseEvent) : void
    {
        if (event.relatedObject != null) {
            // We rolled out into room view, or other element - close up,
            // but don't delete the object, in case there are still events
            // queued up for it.
            hide();
        }
    }

    /** The actual volume slider. */
    protected var _slider : VSlider;

    /** Pointer to any other instance of the popup being currently displayed.
        Unfortunately, ActionScript doesn't support singleton semantics
        very well - instead, we just hold on to a reference, and hide any
        previous instance before displaying a new one. */
    protected static var _currentInstance :ZoomPopup;

}
   

}
