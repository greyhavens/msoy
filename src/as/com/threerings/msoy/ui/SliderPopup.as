package com.threerings.msoy.ui {

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
import mx.events.SliderEvent;

import com.threerings.util.Util;

/** Background skin to be loaded from the style sheet. */
[Style(name="backgroundSkin", type="Class", inherit="no")]

/**
 * Helper UI element for generic popup slider controls, such as volume and zoom.
 *
 * Slider control has "weak singleton" semantics - an instance can be created with
 * the 'new' keyword, but if any other instance previously existed, it will be closed
 * and removed from display prior to displaying the new one.
 */
public class SliderPopup extends Canvas
{
    public function SliderPopup (
        trigger :DisplayObject, startValue :Number, bindTo :Function,
        sliderInitProps :Object = null)
    {
        styleName = "sliderPopup";

        _trigger = trigger;
        owner = DisplayObjectContainer(Application.application.systemManager);

        // Initialize the window
        var r : Rectangle = _trigger.getBounds(trigger.stage);
        width = 29;
        height = 100;
        x = r.x - 1;
        y = r.y - height;
        verticalScrollPolicy = horizontalScrollPolicy = ScrollPolicy.OFF;
        
        // Initialize slider
        _slider = new VSlider();
        //_slider.getThumbAt(0).scaleX = 2;
        _slider.sliderThumbClass = SliderPopupThumb;
        _slider.x = 4;
        _slider.y = 10;
        _slider.height = 80;
        Util.init(_slider, sliderInitProps, { minimum: 0, maximum: 1, liveDragging: true });
        _slider.value = startValue;

        BindingUtils.bindSetter(bindTo, _slider, "value");

        addChild(_slider);
    }

    /** Show the popup, by adding it to the application's display list,
     *  and register for appropriate events. */
    public function show () : void
    {
        destroyCurrentInstance();
        
        owner.addChild(this);
        addEventListener(MouseEvent.ROLL_OUT, mouseOutHandler, false, 0, true);
        addEventListener(MouseEvent.ROLL_OVER, mouseOverHandler, false, 0, true);
        _slider.addEventListener(SliderEvent.THUMB_RELEASE, thumbReleaseHandler, false, 0, true);
        
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

    /**
     * Helper method to either close the currently displayed SliderPopup and show this one,
     * or untoggle this one if it was already being displayed.
     */
    public static function toggle (
        trigger :DisplayObject, startValue :Number, bindTo :Function,
        sliderInitProps :Object = null) :void
    {
        if (popupExists() && _currentInstance._trigger == trigger) {
            destroyCurrentInstance();
        } else {
            new SliderPopup(trigger, startValue, bindTo, sliderInitProps).show();
        }
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
    protected function mouseOutHandler (event : MouseEvent) : void
    {
        if (event.relatedObject != null) {
            _cursorOffCanvas = true;

            if ( ! event.buttonDown) {
                // We rolled out into room view, or other element - close up,
                // but don't delete the object, in case there are still events
                // queued up for it.
                hide();
            }
        }
    }

    protected function mouseOverHandler (event :MouseEvent) :void
    {
        _cursorOffCanvas = false;
    }

    protected function thumbReleaseHandler (event :SliderEvent): void
    {
        if (_cursorOffCanvas) {
            hide();
        }
    }

    /** The actual slider. */
    protected var _slider : VSlider;

    /** True if the mouse cursor has left the canvas area. */
    protected var _cursorOffCanvas :Boolean;

    /** The object that triggered this popup. */
    protected var _trigger :DisplayObject;

    /** Pointer to any other instance of the popup being currently displayed.
        Unfortunately, ActionScript doesn't support singleton semantics
        very well - instead, we just hold on to a reference, and hide any
        previous instance before displaying a new one. */
    protected static var _currentInstance : SliderPopup;
}

}
