package com.threerings.msoy.world.client
{

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.events.MouseEvent;
import flash.events.TimerEvent;
import flash.geom.Rectangle;
import flash.utils.Timer;

import mx.containers.Box;

import mx.core.UIComponent;
import mx.core.ScrollPolicy;

import mx.events.ChildExistenceChangedEvent;


/**
 * A button ribbon is a UI widget that hides multiple display objects (e.g. buttons) under
 * a single selected object. If the object is clicked on and the mouse button held down for 
 * a longer period of time, the ribbon of all objects opens up and lets the user
 * pick a different one.
 *
 * The widget extends the Box component from Flex, so elements can be manipulated using
 * standard addChild/removeChild function calls.
 */
public class Ribbon extends Box
{
    /**
     * Constructor
     */
    public function Ribbon ()
    {
        _timer = new Timer(FOLDOUT_DELAY, 1);
        _timer.addEventListener(TimerEvent.TIMER, handleFoldoutTimer);
        this.horizontalScrollPolicy = ScrollPolicy.OFF;
        this.verticalScrollPolicy = ScrollPolicy.OFF;
    }

    /**
     * Retrieves the index of the active child, in [0, numChildren-1] if any children exist,
     * -1 otherwise.
     */
    public function get selectedIndex () :int
    {
        return _selectedIndex;
    }

    /**
     * Sets the active child based on its index, in [0, numChildren-1],
     * or -1 to disactivate all children.
     */
    public function set selectedIndex (index :int) :void
    {
        if (index != _selectedIndex) {
            if (index < numChildren && index >= -1) {
                _selectedIndex = index;
            } else {
                throw new ArgumentError("Invalid value for selectedIndex: " + index +
                                        " is not in range.");
            }
            updateDisplay();
        }
    }

    /** Retrieves the active child, or null of no active object was set. */
    public function get selectedChild () :DisplayObject
    {
        return _selectedIndex >= 0 ? getChildAt(_selectedIndex) : null;
    }

    /** Sets the specified child object as the active child. */
    public function set selectedChild (element :DisplayObject) :void
    {
        _selectedIndex = getChildIndex(element);
    }

    /**
     * Sets the current state of the ribbon. If true, the ribbon is 'collapsed' and only
     * the active child will be visible. If false, all children are visible.
     */
    public function get collapsed () :Boolean
    {
        return _collapsed;
    }

    /**
     * Retrieves true is the ribbon is currently collapsed, false otherwise.
     */
    public function set collapsed (value :Boolean) :void
    {
        if (value != _collapsed) {
            _collapsed = value;
            updateDisplay();
        }
    }

    // from Box
    override public function set visible (value :Boolean) :void
    {
        super.visible = value;
        updateDisplay();
    }
    
    // from Box
    override public function parentChanged (p :DisplayObjectContainer) :void
    {
        var handlers :Array =
            [ { event: MouseEvent.MOUSE_DOWN,  capture: true,   handler: handleMouseDown },
              { event: MouseEvent.MOUSE_UP,    capture: true,   handler: handleMouseUp }, 
              { event: MouseEvent.ROLL_OUT,                     handler: handleRollOut }
            ];

        // if we're added to display list, add event listeners, otherwise remove them
        var fn :Function = (p != null) ? addEventListener : removeEventListener;
        for each (var def :Object in handlers) {
            fn(def.event, def.handler, def.capture == true);
        }
    }

    // from UIComponent
    protected override function updateDisplayList (
        unscaledWidth:Number, unscaledHeight:Number) :void
    {
        super.updateDisplayList(unscaledWidth, unscaledHeight);
        recomputeDimensions();
        updateDisplay();
    }

    /**
     * Handles mouse down event, and starts a timer if necessary.
     */
    protected function handleMouseDown (event :MouseEvent) :void
    {
        if (collapsed) {
            startFoldoutTimer();
        } 
    }

    /**
     * Handles mouse up, and if the ribbon was expanded, causes it to activate the selected
     * child, and return to collapsed state.
     */
    protected function handleMouseUp (event :MouseEvent) :void
    {
        if (collapsed) {
            // mouse-up during a regular click - ignore
            stopFoldoutTimer();
        } else {
            // mouse-up when unfolded - pick a new child and collapse again
            var obj :DisplayObject = event.target as DisplayObject;
            if (contains(obj)) {
                selectedChild = obj;
                collapsed = true;
            }
        }
    }

    /**
     * When the mouse rolls out of the ribbon area, just collapse it.
     */
    protected function handleRollOut (event :MouseEvent) :void
    {
        stopFoldoutTimer();
        collapsed = true;
    }

    /**
     * Refreshes ribbon display based on its collapse state.
     */
    protected function updateDisplay() :void
    {
        if (! collapsed) {
            stretchToShowAllChildren();
        } else {
            showOneChild(selectedChild);
        }
    }

    /**
     * Called when the ribbon is collapsed, it restricts the ribbon's viewing area
     * to that of the active child object.
     */
    protected function showOneChild (selectedChild :DisplayObject) :void
    {
        if (selectedChild != null) {
            this.scrollRect = selectedChild.getBounds(this);
        }
    }

    /**
     * Called when the ribbon is expanded, it displays all child objects.
     */ 
    protected function stretchToShowAllChildren () :void
    {
        this.scrollRect = null;
    }

    /**
     * Updates the container's dimensions based on all of its children.
     */
    protected function recomputeDimensions () :void
    {
        var bounds :Rectangle = new Rectangle();
        if (numChildren > 0) {
            var child :DisplayObject = getChildAt(0);
            bounds.x = child.x;
            bounds.y = child.y;
            bounds.width = child.width;
            bounds.height = child.height;
            for (var i :int = 1; i < numChildren; i++) {
                child = getChildAt(i);
                bounds.width = Math.max(bounds.right, child.x + child.width) - bounds.x;
                bounds.height = Math.max(bounds.bottom, child.y + child.height) - bounds.y;
                bounds.x = Math.min(bounds.x, child.x);
                bounds.y = Math.min(bounds.y, child.y);
            }
        }
        this.width = bounds.width;
        this.height = bounds.height;
    }


    /** Timer helper: start counting the time since last mouse down. */
    protected function startFoldoutTimer () :void
    {
        _timer.reset();
        _timer.start();
    }

    /** Timer helper: stop counting the time since last mouse down. */
    protected function stopFoldoutTimer () :void
    {
        _timer.stop();
    }

    /**
     * Timer event handler: after the used had clicked and pressed on the mouse button
     * for a while, it expands the ribbon.
     */
    protected function handleFoldoutTimer (event :TimerEvent) :void
    {
        collapsed = false;
    }
        
    /** How long the mouse button needs to be pressed to expand the ribbon, in milliseconds. */
    protected var FOLDOUT_DELAY :int = 500;

    /** Index of the currently selected child. */
    protected var _selectedIndex :int = -1;

    /** Is the ribbon collapsed? */
    protected var _collapsed :Boolean = false;

    /** Ribbon rollout timer. */
    protected var _timer :Timer;

}
}
