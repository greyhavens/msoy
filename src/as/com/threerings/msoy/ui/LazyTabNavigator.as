package com.threerings.msoy.ui {

import mx.core.ContainerCreationPolicy;

import mx.core.UIComponent;

import mx.containers.TabNavigator;
import mx.containers.VBox;

/**
 * A tab navigator that is specially set up for lazy-creating the
 * content in each tab.
 */
public class LazyTabNavigator extends TabNavigator
{
    public function LazyTabNavigator ()
    {
        super();
    }

    /**
     * Add a tab to the container. The creation function takes no args and
     * returns a UIComponent.
     */
    public function addTab (label :String, creation :Function) :void
    {
        addTabAt(label, creation, numChildren);
    }

    /**
     * Add a tab to the container at the specified index.
     * The creation function takes no args and returns a UIComponent.
     */
    public function addTabAt (
        label :String, creation :Function, index :int) :void
    {
        var box :WeeBox = new WeeBox(creation);
        box.label = label;
        addChildAt(box, index);
    }
}
}

import mx.core.ContainerCreationPolicy;
import mx.core.UIComponent;
import mx.containers.VBox;

class WeeBox extends VBox
{
    public function WeeBox (creation :Function)
    {
        _creation = creation;
        creationPolicy = ContainerCreationPolicy.NONE;
    }

    override public function createComponentsFromDescriptors (
        recurse :Boolean = true) :void
    {
        super.createComponentsFromDescriptors(recurse);

        if (_creation != null) {
            addChild(_creation() as UIComponent);
            _creation = null; // assist gc
        }
    }

    protected var _creation :Function;
}
