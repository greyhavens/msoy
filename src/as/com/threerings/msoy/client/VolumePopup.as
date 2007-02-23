//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;    
import flash.events.MouseEvent;
import flash.geom.Rectangle;
import flash.media.SoundMixer;
import flash.media.SoundTransform;

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
 */
public class VolumePopup extends Canvas
{
    /** Constructor. */
    public function VolumePopup (trigger : DisplayObject)
    {
        owner = DisplayObjectContainer (Application.application);

        styleName = "volumeControl";

        // Initialize the window
        var r : Rectangle = trigger.getBounds (trigger.stage);
        width = r.width;
        height = 100;
        x = trigger.x;
        y = r.y - height;
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        
        // Initialize slider
        _slider = new VSlider ();
        _slider.x = 5;
        _slider.y = 10;
        _slider.height = 80;
        _slider.minimum = 0;
        _slider.maximum = 1;
        _slider.tickInterval = 0.5;
        _slider.liveDragging = true;

        SoundMixer.soundTransform = new SoundTransform (Prefs.getSoundVolume());
        _slider.value = SoundMixer.soundTransform.volume;

        BindingUtils.bindSetter(
            function (val :Number) :void
            {
                SoundMixer.soundTransform = new SoundTransform (val);
                Prefs.setSoundVolume (val);
            },
            _slider, "value");

        addChild (_slider);
    }

    /** Show the popup, by adding it to the application's display list,
        and register for appropriate events. */
    public function show () : void
    {
        // If another instance is visible, get rid of it
        if (_currentInstance != null) {
            _currentInstance.hide ();
        }
        
        owner.addChild (this);
        addEventListener (MouseEvent.ROLL_OUT, mouseOutHandler, false, 0, true);
        
        // Setting the skin happens after adding to the parent's draw list -
        // this ensures styles are properly loaded
        var cls :Class = getStyle("backgroundSkin");
        setStyle("backgroundImage", cls);

        _currentInstance = this;
    }

    /** Remove the pop-up from the display list, and unregister
        from any events. This should make the object ready to be GC'd,
        if there are no external references holding it. */
    public function hide () : void
    {
        visible = false;
        removeEventListener (MouseEvent.ROLL_OUT, mouseOutHandler, false);
        _currentInstance = null;
    }



    // EVENT HANDLERS

    /** Watch for the mouse leaving the area. */
    private function mouseOutHandler (event : MouseEvent) : void
    {
        if (event.relatedObject != null)
        {
            // We rolled out into room view, or other element - close up.
            hide ();
        }
    }


    // PROTECTED SINGLETON CONSTRUCTOR


    // PRIVATE VARIABLES

    /** The actual volume slider. */
    protected var _slider : VSlider;

    /** Pointer to any other instance of the popup being currently displayed.
        Unfortunately, ActionScript doesn't support singleton semantics
        very well - instead, we just hold on to a reference, and hide any
        previous instance before displaying a new one. */
    protected static var _currentInstance : VolumePopup;

}
   

}
