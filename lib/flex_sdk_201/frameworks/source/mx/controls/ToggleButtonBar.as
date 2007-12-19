////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls
{

import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.ui.Keyboard;
import mx.core.IFlexDisplayObject;
import mx.core.mx_internal;
import mx.events.FlexEvent;

use namespace mx_internal;

//--------------------------------------
//  Styles
//-------------------------------------- 

/**
 *  Name of CSS style declaration that specifies styles for the text of the
 *  selected button.
 */
[Style(name="selectedButtonTextStyleName", type="String", inherit="no")]

//--------------------------------------
//  Excluded APIs
//--------------------------------------

[Exclude(name="horizontalLineScrollSize", kind="property")]
[Exclude(name="horizontalPageScrollSize", kind="property")]
[Exclude(name="horizontalScrollBar", kind="property")]
[Exclude(name="horizontalScrollPolicy", kind="property")]
[Exclude(name="horizontalScrollPosition", kind="property")]
[Exclude(name="maxHorizontalScrollPosition", kind="property")]
[Exclude(name="maxVerticalScrollPosition", kind="property")]
[Exclude(name="verticalLineScrollSize", kind="property")]
[Exclude(name="verticalPageScrollSize", kind="property")]
[Exclude(name="verticalScrollBar", kind="property")]
[Exclude(name="verticalScrollPolicy", kind="property")]
[Exclude(name="verticalScrollPosition", kind="property")]

[Exclude(name="scroll", kind="event")]
[Exclude(name="click", kind="event")]

[Exclude(name="backgroundAlpha", kind="style")]
[Exclude(name="backgroundAttachment", kind="style")]
[Exclude(name="backgroundColor", kind="style")]
[Exclude(name="backgroundImage", kind="style")]
[Exclude(name="backgroundSize", kind="style")]
[Exclude(name="borderColor", kind="style")]
[Exclude(name="borderSides", kind="style")]
[Exclude(name="borderSkin", kind="style")]
[Exclude(name="borderStyle", kind="style")]
[Exclude(name="borderThickness", kind="style")]
[Exclude(name="cornerRadius", kind="style")]
[Exclude(name="dropShadowColor", kind="style")]
[Exclude(name="dropShadowEnabled", kind="style")]
[Exclude(name="horizontalScrollBarStyleName", kind="style")]
[Exclude(name="shadowCapColor", kind="style")]
[Exclude(name="shadowColor", kind="style")]
[Exclude(name="shadowDirection", kind="style")]
[Exclude(name="shadowDistance", kind="style")]
[Exclude(name="verticalScrollBarStyleName", kind="style")]

//--------------------------------------
//  Other metadata
//-------------------------------------- 

[IconFile("TabBar.png")]

/**
 *  The ToggleButtonBar control defines a horizontal or vertical 
 *  group of buttons that maintain their selected or deselected state.
 *  Only one button in the ToggleButtonBar control
 *  can be in the selected state.
 *  This means that when a user selects a button in a ToggleButtonBar control,
 *  the button stays in the selected state until the user selects a different button.
 *
 *  <p>If you set the <code>toggleOnClick</code> property of the
 *  ToggleButtonBar container to <code>true</code>,
 *  selecting the currently selected button deselects it.
 *  By default the <code>toggleOnClick</code> property is set to
 *  <code>false</code>.</p>
 *
 *  <p>You can use the ButtonBar control to define a group
 *  of push buttons.</p>
 *
 *  <p>The typical use for a toggle button is for maintaining selection
 *  among a set of options, such as switching between views in a ViewStack
 *  container.</p>
 *
 *  <p>The ToggleButtonBar control creates Button controls based on the value of 
 *  its <code>dataProvider</code> property. 
 *  Even though ToggleButtonBar is a subclass of Container, do not use methods such as 
 *  <code>Container.addChild()</code> and <code>Container.removeChild()</code> 
 *  to add or remove Button controls. 
 *  Instead, use methods such as <code>addItem()</code> and <code>removeItem()</code> 
 *  to manipulate the <code>dataProvider</code> property. 
 *  The ToggleButtonBar control automatically adds or removes the necessary children based on 
 *  changes to the <code>dataProvider</code> property.</p>
 *
 *  <p>To control the styling of the buttons of the ToggleButtonBar control, 
 *  use the <code>buttonStyleName</code>, <code>firstButtonStyleName</code>, 
 *  and <code>lastButtonStyleName</code> style properties; 
 *  do not try to style the individual Button controls 
 *  that make up the ToggleButtonBar control.</p>
 *
 *  @mxml
 *
 *  <p>The <code>&lt;mx:ToggleButtonBar&gt;</code> tag inherits all of the tag attributes
 *  of its superclass, and adds the following tag attributes:</p>
 *
 *  <pre>
 *  &lt;mx:ToggleButtonBar
 *    <b>Properties</b>
 *    selectedIndex="0"
 *    toggleOnClick="false|true"
 * 
 *    <b>Styles</b>
 *    selectedButtonTextStyleName="<i>Name of CSS style declaration that specifies styles for the text of the selected button.</i>"&gt;
 *    ...
 *       <i>child tags</i>
 *    ...
 *  &lt;/mx:ToggleButtonBar&gt;
 *  </pre>
 *
 *  @includeExample examples/ToggleButtonBarExample.mxml
 *
 *  @see mx.controls.ButtonBar
 *  @see mx.controls.LinkBar
 */
public class ToggleButtonBar extends ButtonBar
{
    include "../core/Version.as";

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     */
    public function ToggleButtonBar()
    {
        super();
    }

    //--------------------------------------------------------------------------
    //
    //  Variables
    //
    //--------------------------------------------------------------------------

    /**
     *  @private.
     */
    private var initializeSelectedButton:Boolean = true;

    /**
     *  @private
     *  Name of style used to specify selectedButtonTextStyleName.
     *  Overridden by TabBar.
     */
    mx_internal var selectedButtonTextStyleNameProp:String =
        "selectedButtonTextStyleName";

    //--------------------------------------------------------------------------
    //
    //  Overridden properties
    //
    //--------------------------------------------------------------------------

    //----------------------------------
    //  selectedIndex
    //----------------------------------

    /**
     *  @private
     *  Storage for the selectedIndex property.
     */
    private var _selectedIndex:int = -1;

    /**
     *  @private.
     */
    private var selectedIndexChanged:Boolean = false;

    [Bindable("click")]
    [Bindable("valueCommit")]
    [Inspectable(category="General")]

    /**
     *  Index of the selected button.
     *  Indexes are in the range of 0, 1, 2, ... , n - 1,
     *  where <i>n</i> is the number of buttons.
     *
     *  @default 0
     */
    override public function get selectedIndex():int
    {
        return super.selectedIndex;
    }

    /**
     *  @private.
     */
    override public function set selectedIndex(value:int):void
    {
        if (value == selectedIndex)
            return;

        // If the buttons have not been created yet, store the selectedIndex.
        if (numChildren == 0)
        {
            _selectedIndex = value;
            selectedIndexChanged = true;
        }

        // Otherwise set the index locally and invalidate properties.
        if (value >= 0 && value < numChildren)
        {
            _selectedIndex = value;
            selectedIndexChanged = true;

            invalidateProperties();

            dispatchEvent(new FlexEvent(FlexEvent.VALUE_COMMIT));
        }
    }

    //--------------------------------------------------------------------------
    //
    //  Properties
    //
    //--------------------------------------------------------------------------

    //----------------------------------
    //  toggleOnClick
    //----------------------------------

    /**
     *  @private
     *  Storage for the toggleOnClick property.
     */
    private var _toggleOnClick:Boolean = false;

    [Inspectable(category="General", defaultValue="false")]

    /**
     *  Specifies whether the currently selected button can be deselected by
     *  the user.
     *
     *  By default, the currently selected button gets deselected
     *  automatically only when another button in the group is selected.
     *  Setting this property to <code>true</code> lets the user
     *  deselect it.
     *  When the currently selected button is deselected,
     *  the <code>selectedIndex</code> property is set to <code>-1</code>.
     *
     *  @default false
     */
    public function get toggleOnClick():Boolean
    {
        return _toggleOnClick;
    }

    /**
     *  @private
     */
    public function set toggleOnClick(value:Boolean):void
    {
        _toggleOnClick = value;
    }

    //--------------------------------------------------------------------------
    //
    //  Overridden methods: UIComponent
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override protected function commitProperties():void
    {
        super.commitProperties();

        if (selectedIndexChanged)
        {
            hiliteSelectedNavItem(_selectedIndex);

            // Update parent index.
            super.selectedIndex = _selectedIndex;

            selectedIndexChanged = false;
        }
    }

    /**
     *  @private
     */
    override protected function updateDisplayList(unscaledWidth:Number,
                                                  unscaledHeight:Number):void
    {
        super.updateDisplayList(unscaledWidth, unscaledHeight);

        // Select the first selectedIndex in the UI. If the user has defined it,
        // the value will be waiting in the selectedIndex property
        if (initializeSelectedButton)
        {
            initializeSelectedButton = false;

            var index:int = selectedIndex;
            if (index == -1 && numChildren > 0)
                index = 0;
            hiliteSelectedNavItem(index);
        }
    }

    /**
     *  @private
     */
    override public function styleChanged(styleProp:String):void
    {
        var allStyles:Boolean = styleProp == null || styleProp == "styleName";

        super.styleChanged(styleProp);

        if (allStyles ||
            styleProp == selectedButtonTextStyleNameProp)
        {
            if (selectedIndex != -1 && selectedIndex < numChildren)
            {
                var child:Button = Button(getChildAt(selectedIndex));
                if (child)
                {
                    var selectedButtonTextStyleName:String =
                        getStyle(selectedButtonTextStyleNameProp);

                    child.getTextField().styleName =
                        selectedButtonTextStyleName ?
                        selectedButtonTextStyleName :
                        "activeButtonStyle";
                }
            }
        }
    }

    //--------------------------------------------------------------------------
    //
    //  Overridden methods: NavBar
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override protected function createNavItem(
                                        label:String,
                                        icon:Class = null):IFlexDisplayObject
    {
        var b:Button = Button(super.createNavItem(label, icon));
        b.toggle = true;
        return b;
    }

    /**
     *  @private
     */
    override protected function hiliteSelectedNavItem(index:int):void
    {
        var child:Button;

        // Un-hilite the current selection
        if (selectedIndex != -1 && selectedIndex < numChildren)
        {
            child = Button(getChildAt(selectedIndex));

            child.selected = false;
            child.getTextField().styleName = child;

            child.invalidateDisplayList()
        }

        // Set new index
        super.selectedIndex = index;

        if (index != -1)
        {
            // Hilite the new selection
            child = Button(getChildAt(selectedIndex));

            child.selected = true;

            var selectedButtonTextStyleName:String =
                getStyle(selectedButtonTextStyleNameProp);

            child.getTextField().styleName =
                selectedButtonTextStyleName ?
                selectedButtonTextStyleName :
                "activeButtonStyle";

            child.invalidateDisplayList();
        }
    }

    /**
     *  @private
     */
    override protected function resetNavItems():void
    {
        var selectedButtonTextStyleName:String =
            getStyle(selectedButtonTextStyleNameProp);

        // Need to reset the index values, selection state,
        // and selected text style...
        var n:int = numChildren;
        for (var i:int = 0; i < n; i++)
        {
            var child:Button = Button(getChildAt(i));

            if (i == selectedIndex)
            {
                child.selected = true;
                child.getTextField().styleName =
                    selectedButtonTextStyleName ?
                    selectedButtonTextStyleName :
                    "activeButtonStyle";
            }
            else
            {
                child.selected = false;
                child.getTextField().styleName = child;
            }
        }

        super.resetNavItems();
    }

    //--------------------------------------------------------------------------
    //
    //  Methods
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     *  Select the button at the specified index.
     */
    mx_internal function selectButton(index:int,
                                      updateFocusIndex:Boolean = false,
                                      trigger:Event = null):void
    {
        // 143958
        _selectedIndex = index;

        if (updateFocusIndex)
        {
            drawButtonFocus(focusedIndex, false);
            focusedIndex = index;
            drawButtonFocus(focusedIndex, false);
        }

        var child:Button = Button(getChildAt(index));
        simulatedClickTriggerEvent = trigger;
        child.dispatchEvent(new MouseEvent(MouseEvent.CLICK));
        simulatedClickTriggerEvent = null;
    }

    //--------------------------------------------------------------------------
    //
    //  Overridden event handlers: UIComponent
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override protected function keyDownHandler(event:KeyboardEvent):void
    {
        var targetIndex:int = -1;
        var updateFocusIndex:Boolean = true;
        var n:int = numChildren;

        switch (event.keyCode)
        {
            case Keyboard.PAGE_DOWN:
            {
                targetIndex = nextIndex(selectedIndex);
                break;
            }

            case Keyboard.PAGE_UP:
            {
                if (selectedIndex != -1)
                    targetIndex = prevIndex(selectedIndex);
                else if (n > 0)
                    targetIndex = 0;
                break;
            }

            case Keyboard.HOME:
            {
                if (n > 0)
                    targetIndex = 0;
                break;
            }

            case Keyboard.END:
            {
                if (n > 0)
                    targetIndex = n - 1;
                break;
            }

            case Keyboard.SPACE:
            case Keyboard.ENTER:
            {
                if (focusedIndex != -1)
                {
                    targetIndex = focusedIndex;
                    updateFocusIndex = false;
                }
                break;
            }

            default:
            {
                super.keyDownHandler(event);
            }
        }

        if (targetIndex != -1)
        {
            selectButton(targetIndex, updateFocusIndex, event);
        }

        event.stopPropagation();
    }

    /**
     *  @private
     */
    override protected function keyUpHandler(event:KeyboardEvent):void
    {
        // Override superclass's keyUpHandler() but do nothing.
    }

    //--------------------------------------------------------------------------
    //
    //  Overridden event handlers: NavBar
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override protected function clickHandler(event:MouseEvent):void
    {
        var index:int = getChildIndex(Button(event.currentTarget));

        // 143958
        _selectedIndex = index;

        if (_toggleOnClick && index == selectedIndex)
            hiliteSelectedNavItem(-1);
        else
            hiliteSelectedNavItem(index);

        super.clickHandler(event);
    }
}

}
