////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.containers
{

import flash.display.DisplayObject;
import flash.events.Event;
import flash.text.TextFieldAutoSize;
import mx.containers.FormItemDirection;
import mx.containers.utilityClasses.Flex;
import mx.controls.FormItemLabel;
import mx.core.Container;
import mx.core.EdgeMetrics;
import mx.core.IFlexDisplayObject;
import mx.core.IUIComponent;
import mx.core.ScrollPolicy;
import mx.core.mx_internal;

use namespace mx_internal;

//--------------------------------------
//  Styles
//--------------------------------------

include "../styles/metadata/GapStyles.as";

/**
 *  Horizontal alignment of children in the container.
 *  Possible values are <code>"left"</code>, <code>"center"</code>,
 *  and <code>"right"</code>.
 *
 *  @default "left"
 */
[Style(name="horizontalAlign", type="String", enumeration="left,center,right", inherit="no")]

/**
 *  Number of pixels between the label and child components of the form item.
 *
 *  @default 14
 */
[Style(name="indicatorGap", type="Number", format="Length", inherit="yes")]

/**
 *  Specifies the skin to use for the required field indicator. 
 *
 *  @default mx.containers.FormItem.Required
 */
[Style(name="indicatorSkin", type="Class", inherit="no")]

/**
 *  Width of the form labels.
 *  The default is the length of the longest label in the form.
 */
[Style(name="labelWidth", type="Number", format="Length", inherit="yes")]

/**
 *  Number of pixels between the container's bottom border
 *  and the bottom edge of its content area.
 *  
 *  @default 0
 */
[Style(name="paddingBottom", type="Number", format="Length", inherit="no")]

/**
 *  Number of pixels between the container's right border
 *  and the right edge of its content area.
 *  
 *  @default 0
 */
[Style(name="paddingRight", type="Number", format="Length", inherit="no")]

/**
 *  Number of pixels between the container's top border
 *  and the top edge of its content area.
 *  
 *  @default 0
 */
[Style(name="paddingTop", type="Number", format="Length", inherit="no")]

//--------------------------------------
//  Excluded APIs
//--------------------------------------

[Exclude(name="focusIn", kind="event")]
[Exclude(name="focusOut", kind="event")]

[Exclude(name="focusBlendMode", kind="style")]
[Exclude(name="focusSkin", kind="style")]
[Exclude(name="focusThickness", kind="style")]

[Exclude(name="focusInEffect", kind="effect")]
[Exclude(name="focusOutEffect", kind="effect")]

//--------------------------------------
//  Other metadata
//--------------------------------------

[IconFile("FormItem.png")]

/**
 *  The FormItem container defines a label and one or more children
 *  arranged horizontally or vertically.
 *  Children can be controls or other containers.
 *  A single Form container can hold multiple FormItem containers.
 *
 *  @mxml
 *
 *  <p>The <code>&lt;mx:FormItem&gt;</code> tag inherits all of the tag 
 *  attributes of its superclass, except <code>paddingLeft</code>,
 *  and adds the following tag attributes:</p>
 *
 *  <pre>
 *  &lt;mx:FormItem
 *    <strong>Properties</strong>
 *    direction="vertical|horizontal"
 *    label=""
 *    required="false|true"
 *    <strong>Styles</strong>
 *    horizontalAlign="left|center|right"
 *    horizontalGap="8"
 *    indicatorGap="14"
 *    indicatorSkin="Class; mx.container.FormItem.Required"
 *    labelWidth="<i>Calculated</i>"
 *    paddingBottom="0"
 *    paddingRight="0"
 *    paddingTop="0"
 *    verticalGap="6"
 *    &gt;
 *    ...
 *      <i>child tags</i>
 *    ...
 *  &lt;/mx:FormItem&gt;
 *  </pre>
 *
 *  @see mx.containers.Form
 *  @see mx.containers.FormItemDirection
 *  @see mx.containers.FormHeading
 *  @see mx.controls.FormItemLabel
 *
 *  @includeExample examples/FormExample.mxml
 */
public class FormItem extends Container
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
    public function FormItem()
    {
        super();

        _horizontalScrollPolicy = ScrollPolicy.OFF;
        _verticalScrollPolicy = ScrollPolicy.OFF;
    }

    //--------------------------------------------------------------------------
    //
    //  Variables
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     *  A reference to the FormItemLabel subcomponent.
     */
    private var labelObj:FormItemLabel;

    /**
     *  @private
     *  A reference to the "required" indicator.
     */
    private var indicatorObj:IFlexDisplayObject;

    /**
     *  @private
     */
    private var guessedRowWidth:Number;

    /**
     *  @private
     */
    private var guessedNumColumns:int;

    /**
     *  @private
     */
    private var alreadyGuessedAgain:Boolean = false;

    //--------------------------------------------------------------------------
    //
    //  Overridden properties
    //
    //-------------------------------------------------------------------------

    //----------------------------------
    //  label
    //----------------------------------

    /**
     *  @private
     *  Storage for the label property.
     */
    private var _label:String = "";

    [Bindable("labelChanged")]
    [Inspectable(category="General", defaultValue="")]

    /**
     *  Text label for the FormItem. This label appears to the left of the 
     *  child components of the form item.
     *  The default value is the empty String ("").
     */
    override public function get label():String
    {
        return _label;
    }

    /**
     *  @private
     */
    override public function set label(value:String):void
    {
        _label = value;

        invalidateProperties();

        dispatchEvent(new Event("labelChanged"));
    }

    //--------------------------------------------------------------------------
    //
    //  Properties
    //
    //--------------------------------------------------------------------------

    //----------------------------------
    //  direction
    //----------------------------------

    /**
     *  @private
     *  Storage for the direction property.
     */
    private var _direction:String = FormItemDirection.VERTICAL;

    [Bindable("directionChanged")]
    [Inspectable(category="General", enumeration="vertical,horizontal", defaultValue="vertical")]

    /**
     *  Direction of the FormItem subcomponents.
     *  Possible MXML values are <code>"vertical"</code>
     *  or <code>"horizontal"</code>.
     *  The default MXML value is <code>"vertical"</code>.
     *  Possible ActionScript values are <code>FormItemDirection.VERTICAL</code>
     *  or <code>FormItemDirection.HORIZONTAL</code>.
     *
     *  <p>When <code>direction</code> is <code>"vertical"</code>,
     *  the children of the  FormItem are stacked vertically
     *  to the right of the FormItem label.
     *  When <code>direction</code> is <code>"horizontal"</code>,
     *  the children are placed in a single row (if they fit),
     *  or in two equally-sized columns.</p>
     *
     *  <p>If you need more control over the layout of FormItem children,
     *  you can use a container such as Grid or Tile as the direct child
     *  of the FormItem and put the desired controls inside it.</p>
     *
     *  @default FormItemDirection.VERTICAL
     *  @see mx.containers.FormItemDirection
     */
    public function get direction():String
    {
        return _direction;
    }

    /**
     *  @private
     */
    public function set direction(value:String):void
    {
        _direction = value;

        invalidateSize();
        invalidateDisplayList();

        dispatchEvent(new Event("directionChanged"));
    }

    //----------------------------------
    //  labelObject
    //----------------------------------

    /**
     *  @private
     *  A read-only reference to the FormItemLabel subcomponent
     *  displaying the label of the FormItem.
     *  
     */
    mx_internal function get labelObject():Object
    {
        return labelObj;
    }

    //----------------------------------
    //  required
    //----------------------------------

    /**
     *  @private
     *  Storage for the required property.
     */
    private var _required:Boolean = false;

    [Bindable("requiredChanged")]
    [Inspectable(category="General", defaultValue="false")]

    /**
     *  If <code>true</code>, display an indicator
     *  that the FormItem children require user input.
     *  If <code>false</code>, indicator is not displayed.
     *
     *  <p>This property controls the indicator display only.
     *  You must attach a validator to the children
     *  if you require input validation.</p>
     *
     *  @default false
     */
    public function get required():Boolean
    {
        return _required;
    }

    /**
     *  @private
     */
    public function set required(value:Boolean):void
    {
        if (value != _required)
        {
            _required = value;
 
            invalidateDisplayList();

            dispatchEvent(new Event("requiredChanged"));
        }
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

        createItemLabel();
    }

    /**
     *  Calculates the preferred, minimum and maximum sizes of the FormItem.
     *  See the <code>UIComponent.measure()</code> method for more information
     *  about the <code>measure()</code> method.
     *
     *  <p>The <code>FormItem.measure()</code> method first determines
     *  the number of columns to use.
     *  If the <code>direction</code> property is
     *  <code>FormItemDirection.HORIZONTAL</code>,
     *  all controls will be placed in a single row if possible.
     *  If the controls cannot fit in a single row, they are split
     *  into two columns.
     *  If <code>direction</code> is <code>FormItemDirection.VERTICAL</code>,
     *  the controls are placed in a single column.</p>
     *
     *  <p>A FormItem contains two areas: the label area
     *  and the controls area.
     *  The size of the label is the same
     *  regardless of the direction of the controls.
     *  The size of the control area depends on how many rows 
     *  and columns are used.</p>
     *
     *  <p>The width of the label area is determined by the
     *  <code>labelWidth</code> style property.
     *  If this property is <code>undefined</code> (which is the default),
     *  the width of the largest label in the parent Form container
     *  is used.</p>
     *
     *  <p>If all children are on a single row, the width of the
     *  control area is the sum of the widths of all the children
     *  plus <code>horizontalGap</code> space between the children.</p>
     *
     *  <p>If the children are on a single column,
     *  the width of the control area is the width of the widest child.</p>
     *
     *  <p>If the children are on multiple rows and columns,
     *  the width of the widest child is the column width,
     *  and the width of the control area is the column width
     *  multiplied by the number of columns plus the
     *  <code>horizontalGap</code> space between each column.</p>
     *
     *  <p><code>measuredWidth</code> is set to the
     *  width of the label area plus the width of the control area
     *  plus the value of the <code>indicatorGap</code> style property.
     *  The values of the <code>paddingLeft</code> and
     *  <code>paddingRight</code> style properties
     *  and the width of the border are also added.</p>
     *
     *  <p><code>measuredHeight</code> is set to the
     *  sum of the preferred heights of all rows of children,
     *  plus <code>verticalGap</code> space between each child.
     *  The <code>paddingTop</code> and <code>paddingBottom</code>
     *  style properties and the height of the border are also added.</p>
     *
     *  <p><code>measuredMinWidth</code> is set to the width of the
     *  label area plus the minimum width of the control area
     *  plus the value of the <code>indicatorGap</code> style property.
     *  The values of the <code>paddingLeft</code> and
     *  <code>paddingRight</code> style properties
     *  and the width of the border are also added.</p>
     *
     *  <p><code>measuredMinHeight</code> is set to the
     *  sum of the minimum heights of all rows of children,
     *  plus <code>verticalGap</code> space between each child.
     *  The <code>paddingTop</code> and <code>paddingBottom</code>
     *  style properties and the height of the border are also added.</p>
     */
    override protected function measure():void
    {
        super.measure();

        var numColumns:int = guessedNumColumns =
            calcNumColumns(guessedRowWidth);

        var horizontalGap:Number = getStyle("horizontalGap");
        var verticalGap:Number = getStyle("verticalGap");
        var indicatorGap:Number = getStyle("indicatorGap");

        var col:int = 0;

        var tempMinWidth:Number = 0;
        var tempWidth:Number = 0;
        var tempMinHeight:Number = 0;
        var tempHeight:Number = 0;

        var minWidth:Number = 0;
        var minHeight:Number = 0;
        var preferredWidth:Number = 0;
        var preferredHeight:Number = 0;
        var maxPreferredWidth:Number = 0;

        var n:int = numChildren;
        var i:int;
        var child:IUIComponent;

        // If direction == FormItemDirection.HORIZONTAL
        // and the children span multiple rows,
        // then updateDisplayList() (below) sets each child's width to the
        // preferredWidth of the largest child.
        if (direction == FormItemDirection.HORIZONTAL && numColumns < n)
        {
            for (i = 0; i < n; i++)
            {
                child = IUIComponent(getChildAt(i));
                maxPreferredWidth = Math.max(
                    maxPreferredWidth, child.getExplicitOrMeasuredWidth());
            }
        }

        for (i = 0; i < n; i++)
        {
            child = IUIComponent(getChildAt(i));

            if (col < numColumns)
            {
                tempMinWidth += !isNaN(child.percentWidth) ?
                                child.minWidth :
                                child.getExplicitOrMeasuredWidth();

                tempWidth += (maxPreferredWidth > 0) ?
                             maxPreferredWidth :
                             child.getExplicitOrMeasuredWidth();

                if (col > 0)
                {
                    tempMinWidth += horizontalGap;
                    tempWidth += horizontalGap;
                }

                tempMinHeight = Math.max(tempMinHeight,
                                         !isNaN(child.percentWidth) ?
                                         child.minHeight :
                                         child.getExplicitOrMeasuredHeight());

                tempHeight = Math.max(tempHeight,
                                      child.getExplicitOrMeasuredHeight());
            }

            col++;

            if (col >= numColumns || i == n - 1)
            {
                minWidth = Math.max(minWidth, tempMinWidth);
                preferredWidth = Math.max(preferredWidth, tempWidth);

                minHeight += tempMinHeight;
                preferredHeight += tempHeight;

                if (i > 0)
                {
                    minHeight += verticalGap;
                    preferredHeight += verticalGap;
                }

                col = 0;

                tempMinWidth = 0;
                tempWidth = 0;

                tempMinHeight = 0;
                tempHeight = 0;
            }
        }

        var labelWidth:Number = getLabelWidth() + indicatorGap;
        minWidth += labelWidth;
        preferredWidth += labelWidth;

        if (labelObj)
        {
            minHeight = Math.max(minHeight,
                                 labelObj.getExplicitOrMeasuredHeight());

            preferredHeight = Math.max(preferredHeight,
                                       labelObj.getExplicitOrMeasuredHeight());
        }

        var vm:EdgeMetrics = viewMetricsAndPadding;

        minHeight += vm.top + vm.bottom;
        minWidth += vm.left + vm.right;

        preferredHeight += vm.top + vm.bottom;
        preferredWidth += vm.left + vm.right;

        measuredMinWidth = minWidth;
        measuredMinHeight = minHeight;

        measuredWidth = preferredWidth;
        measuredHeight = preferredHeight;

    }

    /**
     *  Responds to size changes by setting the positions and sizes
     *  of this container's children.
     *  See the <code>UIComponent.updateDisplayList()</code> method
     *  for more information about the <code>updateDisplayList()</code> method.
     *
     *  <p>See the <code>FormItem.measure()</code> method for more
     *  information on how the FormItem controls are positioned.</p>
     *
     *  <p>The label is right-aligned in the label area.
     *  All labels in a form are aligned with each other.</p>
     *
     *  <p>If the <code>required</code> property is <code>true</code>,
     *  a symbol indicating the field is required is placed between
     *  the label and the controls.</p>
     *
     *  <p>The controls are positioned in columns, as described in the
     *  documentation for the <code>measure()</code> method.
     *  The  <code>horizontalAlign</code> style property
     *  determines where the controls are placed horizontally.</p>
     *
     *  <p>When the <code>direction</code> property is
     *  <code>"vertical"</code>, any child that has no <code>width</code>
     *  specified uses the <code>measuredWidth</code> rounded up
     *  to the nearest 1/4 width of the control area.
     *  This is done to avoid jagged right edges of controls.</p>
     *
     *  <p> This method calls the <code>super.updateDisplayList()</code>
     *  method before doing anything else.</p>
     *
     *  @param unscaledWidth Specifies the width of the component, in pixels,
     *  in the component's coordinates, regardless of the value of the
     *  <code>scaleX</code> property of the component.
     *
     *  @param unscaledHeight Specifies the height of the component, in pixels,
     *  in the component's coordinates, regardless of the value of the
     *  <code>scaleY</code> property of the component.   
     */
    override protected function updateDisplayList(unscaledWidth:Number,
                                                  unscaledHeight:Number):void
    {
        super.updateDisplayList(unscaledWidth, unscaledHeight);

        var vm:EdgeMetrics = viewMetricsAndPadding;
        var left:Number = vm.left;
        var top:Number = vm.top;
        
        var y:Number = top;
        var labelWidth:Number = getLabelWidth();
        var indicatorGap:Number = getStyle("indicatorGap");
        var horizontalAlign:String = getStyle("horizontalAlign");
        var i:int;
        var child:IUIComponent;
        var childBaseline:Number;
        var childWidth:Number;
        var horizontalGap:Number;
        var verticalGap:Number;

        var horizontalAlignValue:Number;
        if (horizontalAlign == "right")
            horizontalAlignValue = 1;
        else if (horizontalAlign == "center")
            horizontalAlignValue = 0.5;
        else
            horizontalAlignValue = 0;

        var n:int = numChildren;

        // Position our label.
        if (labelObj)
        {
            if (n > 0)
            {
                // Center label with first child
                child = IUIComponent(getChildAt(0));
                childBaseline = child.baselinePosition;
                if (!isNaN(childBaseline))
                    y += childBaseline - labelObj.baselinePosition;
            }

            // Set label size.
            childWidth = Math.min(labelObj.getExplicitOrMeasuredWidth(),
                                  labelWidth);
            labelObj.setActualSize(childWidth,
                                   labelObj.getExplicitOrMeasuredHeight());
            labelObj.move(left + labelWidth - childWidth, y);
        }
        left += labelWidth;

        // Position the "required" indicator.
        displayIndicator(left, y);
        left += indicatorGap;

        var controlWidth:Number = unscaledWidth - vm.right - left;
        if (controlWidth < 0) 
            controlWidth = 0;

        // Position our children.
        if (direction == FormItemDirection.HORIZONTAL)
        {
            var maxWidth:Number = 0;
            var numColumns:int = calcNumColumns(controlWidth);
            var x:Number;
            var col:int = 0;

            horizontalGap = getStyle("horizontalGap");
            verticalGap = getStyle("verticalGap");

            // Earlier, the measure function took a guess at the number
            // of columns, but that function didn't know the width of this
            // FormItem.  Now that we know the width, we may discover that the
            // guess was wrong.  In that case, call invalidateSize(), so that
            // we loop back and repeat the measurement phase again.
            //
            // It's possible that we might introduce an infinite loop - the
            // new guess might change the layout, so that the guess once again
            // is found to be wrong.  If we get back here a second time and
            // discover that the guess is still wrong, we'll just live with it.
            if (numColumns != guessedNumColumns && !alreadyGuessedAgain)
            {
                guessedRowWidth = controlWidth;
                alreadyGuessedAgain = true;
                invalidateSize();
            }
            else
            {
                alreadyGuessedAgain = false;
            }

            // Special case for single row - use the HBox layout algorithm.
            if (numColumns == n)
            {
                var h:Number = height - (top + vm.bottom);
                var excessSpace:Number = Flex.flexChildWidthsProportionally(
                    this, controlWidth - (n - 1) * horizontalGap, h);

                left += (excessSpace * horizontalAlignValue);

                for (i = 0; i < n; i++)
                {
                    child = IUIComponent(getChildAt(i));
                    child.move(Math.floor(left), top);
                    left += child.width + horizontalGap;
                }
            }
            else
            {
                // Determine the widest child.
                for (i = 0; i < n; i++)
                {
                    child = IUIComponent(getChildAt(i));
                    maxWidth = Math.max(maxWidth,
                                        child.getExplicitOrMeasuredWidth());
                }

                // Determine the left side for the columns.
                var widthSlop:Number = controlWidth -
                    (numColumns * maxWidth + (numColumns - 1) * horizontalGap);
                if (widthSlop < 0)
                    widthSlop = 0;

                left += (widthSlop * horizontalAlignValue);
                x = left;

                // Place the children in columns
                for (i = 0; i < n; i++)
                {
                    child = IUIComponent(getChildAt(i));
                    
                    childWidth = Math.min(maxWidth,
                                          child.getExplicitOrMeasuredWidth());

                    child.setActualSize(childWidth,
                                        child.getExplicitOrMeasuredHeight());
                    child.move(x, top);

                    if (++col >= numColumns)
                    {
                        x = left;
                        col = 0;
                        top += child.height + verticalGap;
                    }
                    else
                    {
                        x += maxWidth + horizontalGap;
                    }
                }
            }
        }
        else
        {
            verticalGap = getStyle("verticalGap");

            for (i = 0; i < n; i++)
            {
                child = IUIComponent(getChildAt(i));

                // Round up to nearest 1/4 of controlWidth
                if (!isNaN(child.percentWidth))
                {
                    childWidth = Math.floor(controlWidth *
                        Math.min(child.percentWidth, 100) / 100);
                }
                else
                {
                    childWidth = child.getExplicitOrMeasuredWidth();

                    // Only do modular sizing if an explicit width isn't set.
                    if (isNaN(child.explicitWidth))
                    {
                        if (childWidth < Math.floor(controlWidth * 0.25)) 
                            childWidth = Math.floor(controlWidth * 0.25);
                        else if (childWidth < Math.floor(controlWidth * 0.5)) 
                            childWidth = Math.floor(controlWidth * 0.5);
                        else if (childWidth < Math.floor(controlWidth * 0.75))
                            childWidth = Math.floor(controlWidth * 0.75);
                        else if (childWidth < Math.floor(controlWidth)) 
                            childWidth = Math.floor(controlWidth);
                    }
                }

                child.setActualSize(childWidth,
                                    child.getExplicitOrMeasuredHeight());

                var xOffset:Number = (controlWidth - childWidth) * horizontalAlignValue;
                child.move(left + xOffset, top);

                top += child.height;
                top += verticalGap;
            }
        }

        // Position our label again, now that our children have been positioned.
        // Moving our children can affect the baselinePosition. (Bug 86725)
        if (labelObj)
        {
            y = vm.top;
            if (n > 0)
            {
                // Center label with first child
                child = IUIComponent(getChildAt(0));
                childBaseline = child.baselinePosition;
                if (!isNaN(childBaseline))
                    y += childBaseline - labelObj.baselinePosition;
            }
            labelObj.move(labelObj.x, y);
        }
    }

    //--------------------------------------------------------------------------
    //
    //  Methods
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    private function createItemLabel():void
    {
        // See if we need to create our label mc
        if (label.length > 0)
        {
            if (!labelObj)
            {
                labelObj = new FormItemLabel();
                labelObj.styleName = this;
                rawChildren.addChild(labelObj);
            }

            if (labelObj.text != label)
            {
                labelObj.text = label;

                labelObj.validateSize();

                invalidateSize();
                invalidateDisplayList();

                // Changing the label could affect the overall form label width
                // so we need to invalidate our parent's size here too
                if (parent is Form)
                    Form(parent).invalidateLabelWidth();
            }
        }

        // See if we need to destroy our label mc
        if (label.length == 0 && labelObj)
        {
            rawChildren.removeChild(labelObj);
            labelObj = null;

            invalidateSize();
            invalidateDisplayList();
        }
    }

    /**
     *  @private
     */
    internal function getPreferredLabelWidth():Number
    {
        createItemLabel();

        if (labelObj == null)
            return 0;

        if (isNaN(labelObj.measuredWidth))
            labelObj.validateSize();
        var labelWidth:Number = labelObj.measuredWidth;

        if (isNaN(labelWidth))
            return 0;

        return labelWidth;
    }

    /**
     *  @private
     */
    private function getLabelWidth():Number
    {
        var labelWidth:Number = getStyle("labelWidth");

        // labelWidth of 0 is the same as NaN
        if (labelWidth == 0)
            labelWidth = NaN;

        if (isNaN(labelWidth) && parent is Form)
            labelWidth = Form(parent).calculateLabelWidth();

        if (isNaN(labelWidth))
            labelWidth = getPreferredLabelWidth();

        return labelWidth;
    }

    /**
     *  @private
     */
    private function calcNumColumns(w:Number):int
    {
        var totalWidth:Number = 0;
        var maxChildWidth:Number = 0;
        var horizontalGap:Number = getStyle("horizontalGap");

        if (direction != FormItemDirection.HORIZONTAL)
            return 1;

        var n:int = numChildren;
        for (var i:int = 0; i < n; i++)
        {
            var child:IUIComponent = IUIComponent(getChildAt(i));
            var childWidth:Number = child.getExplicitOrMeasuredWidth();

            maxChildWidth = Math.max(maxChildWidth, childWidth);
            totalWidth += childWidth;
            if (i > 0)
                totalWidth += horizontalGap;
        }

        // See if everything can fit in a single row
        if (isNaN(w) || totalWidth <= w)
            return n;

        // if the width is enough to contain two children use two columns
        if (maxChildWidth*2 <= w)
            return 2;

        // Default is single column
        return 1;
    }

    /**
     *  @private
     */
    private function displayIndicator(xPos:Number, yPos:Number):void
    {
        if (required)
        {
            if (!indicatorObj)
            {
                var indicatorClass:Class = getStyle("indicatorSkin") as Class;
                indicatorObj = IFlexDisplayObject(new indicatorClass());
                rawChildren.addChild(DisplayObject(indicatorObj));
            }

            indicatorObj.x =
                xPos + ((getStyle("indicatorGap") - indicatorObj.width) / 2);
            
            if (labelObj)
            {
                indicatorObj.y = yPos +
                    (labelObj.getExplicitOrMeasuredHeight() -
                     indicatorObj.measuredHeight) / 2;
            }
        }
        else
        {
            if (indicatorObj)
            {
                rawChildren.removeChild(DisplayObject(indicatorObj));
                indicatorObj = null;
            }
        }
    }

	/**
	 *  @private
	 */
    mx_internal function get itemLabel():FormItemLabel
    {
        return labelObj;
    }

}

}
