////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.automation.events
{

import flash.events.Event;
import flash.events.MouseEvent;
import mx.automation.IAutomationObject;
import mx.core.IUIComponent;

/**
 *  The AutomationDragEvent class represents event objects that are 
 *  dispatched as part of a drag-and-drop operation.
 *
 *  @see mx.managers.DragManager
 *  @see mx.core.UIComponent
 */
public class AutomationDragEvent extends MouseEvent
{
    include "../../core/Version.as";

    //--------------------------------------------------------------------------
    //
    //  Class constants
    //
    //--------------------------------------------------------------------------

    /**
     *  The <code>AutomationDragEvent.DRAG_COMPLETE</code> constant defines the value of the 
     *  <code>type</code> property of the event object for a <code>dragComplete</code> event.
     *
     *  <p>The properties of the event object have the following values:</p>
     *  <table class="innertable">
     *     <tr><th>Property</th><th>Value</th></tr>
     *     <tr><td><code>altKey</code></td>
     *         <td>Indicates whether the Alt key is down
     *            (<code>true</code>) or not (<code>false</code>).</td></tr>
     *     <tr><td><code>action</code></td><td>The action that caused the event: 
     *       <code>DragManager.COPY</code>, <code>DragManager.LINK</code>, 
     *       <code>DragManager.MOVE</code>, or <code>DragManager.NONE</code>.</td></tr>
     *     <tr><td><code>bubbles</code></td><td>false</td></tr>
     *     <tr><td><code>cancelable</code></td><td>true</td></tr>
     *     <tr><td><code>ctrlKey</code></td>
     *         <td>Indicates whether the Control key is down
     *            (<code>true</code>) or not (<code>false</code>).</td></tr>
     *     <tr><td><code>currentTarget</code></td><td>The Object that defines the 
     *       event listener that handles the event. For example, if you use 
     *       <code>myButton.addEventListener()</code> to register an event listener, 
     *       myButton is the value of the <code>currentTarget</code>. </td></tr>
     *     <tr><td><code>draggedItem</code></td><td>The item being dragged.</td></tr>
     *     <tr><td><code>dropParent</code></td><td>The object which will be
     *       parenting the item dropped.</td></tr>
     *     <tr><td><code>shiftKey</code></td>
     *         <td>Indicates whether the Shift key is down
     *            (<code>true</code>) or not (<code>false</code>).</td></tr>
     *     <tr><td><code>target</code></td><td>The Object that dispatched the event; 
     *       it is not always the Object listening for the event. 
     *       Use the <code>currentTarget</code> property to always access the 
     *       Object listening for the event.</td></tr>
     *  </table>
     *
     *  @eventType dragComplete
     */
    public static const DRAG_COMPLETE:String = "dragComplete";

    /**
     *  The <code>AutomationDragEvent.DRAG_DROP</code> constant defines the value of the 
     *  <code>type</code> property of the event object for a <code>dragDrop</code> event.
     *
     *  <p>The properties of the event object have the following values:</p>
     *  <table class="innertable">
     *     <tr><th>Property</th><th>Value</th></tr>
     *     <tr><td><code>altKey</code></td>
     *         <td>Indicates whether the Alt key is down
     *            (<code>true</code>) or not (<code>false</code>).</td></tr>
     *     <tr><td><code>action</code></td><td>The action that caused the event: 
     *       <code>DragManager.COPY</code>, <code>DragManager.LINK</code>, 
     *       <code>DragManager.MOVE</code>, or <code>DragManager.NONE</code>.</td></tr>
     *     <tr><td><code>bubbles</code></td><td>false</td></tr>
     *     <tr><td><code>cancelable</code></td><td>true</td></tr>
     *     <tr><td><code>ctrlKey</code></td>
     *         <td>Indicates whether the Control key is down
     *            (<code>true</code>) or not (<code>false</code>).</td></tr>
     *     <tr><td><code>currentTarget</code></td><td>The Object that defines the 
     *       event listener that handles the event. For example, if you use 
     *       <code>myButton.addEventListener()</code> to register an event listener, 
     *       myButton is the value of the <code>currentTarget</code>. </td></tr>
     *     <tr><td><code>draggedItem</code></td><td>The item being dragged.</td></tr>
     *     <tr><td><code>dropParent</code></td><td>The object which will be
     *       parenting the item dropped.</td></tr>
     *     <tr><td><code>shiftKey</code></td>
     *         <td>Indicates whether the Shift key is down
     *            (<code>true</code>) or not (<code>false</code>).</td></tr>
     *     <tr><td><code>target</code></td><td>The Object that dispatched the event; 
     *       it is not always the Object listening for the event. 
     *       Use the <code>currentTarget</code> property to always access the 
     *       Object listening for the event.</td></tr>
     *  </table>
     *
     *  @eventType dragDrop
     */
    public static const DRAG_DROP:String = "dragDrop";

    /**
     *  The AutomationDragEvent.DRAG_START constant defines the value of the 
     *  <code>type</code> property of the event object for a <code>dragStart</code> event.
     *
     *  <p>The properties of the event object have the following values:</p>
     *  <table class="innertable">
     *     <tr><th>Property</th><th>Value</th></tr>
     *     <tr><td><code>altKey</code></td>
     *         <td>Indicates whether the Alt key is down
     *            (<code>true</code>) or not (<code>false</code>).</td></tr>
     *     <tr><td><code>action</code></td><td>The action that caused the event: 
     *       <code>DragManager.COPY</code>, <code>DragManager.LINK</code>, 
     *       <code>DragManager.MOVE</code>, or <code>DragManager.NONE</code>.</td></tr>
     *     <tr><td><code>bubbles</code></td><td>false</td></tr>
     *     <tr><td><code>cancelable</code></td><td>true</td></tr>
     *     <tr><td><code>ctrlKey</code></td>
     *         <td>Indicates whether the Control key is down
     *            (<code>true</code>) or not (<code>false</code>).</td></tr>
     *     <tr><td><code>currentTarget</code></td><td>The Object that defines the 
     *       event listener that handles the event. For example, if you use 
     *       <code>myButton.addEventListener()</code> to register an event listener, 
     *       myButton is the value of the <code>currentTarget</code>. </td></tr>
     *     <tr><td><code>draggedItem</code></td><td>The item being dragged.</td></tr>
     *     <tr><td><code>dropParent</code></td><td>The object which will be
     *       parenting the item dropped.</td></tr>
     *     <tr><td><code>shiftKey</code></td>
     *         <td>Indicates whether the Shift key is down
     *            (<code>true</code>) or not (<code>false</code>).</td></tr>
     *     <tr><td><code>target</code></td><td>The Object that dispatched the event; 
     *       it is not always the Object listening for the event. 
     *       Use the <code>currentTarget</code> property to always access the 
     *       Object listening for the event.</td></tr>
     *  </table>
     *
     *  @eventType dragStart
     */
    public static const DRAG_START:String = "dragStart";

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     *  Normally called by the Flex control and not used in application code.
     *
     *  @param type The event type; indicates the action that caused the event.
     *
     *  @param bubbles Specifies whether the event can bubble up the display list hierarchy.
     *
     *  @param cancelable Specifies whether the behavior associated with the event can be prevented.
     *
     *  @param action The specified drop action, such as <code>DragManager.MOVE</code>.
     *
     *  @param ctrlKey Indicates whether the <code>Ctrl</code> key was pressed.
     *
     *  @param altKey Indicates whether the <code>Alt</code> key was pressed.
     *
     *  @param shiftKey Indicates whether the <code>Shift</code> key was pressed.
     */
    public function AutomationDragEvent(type:String, bubbles:Boolean = false,
                              cancelable:Boolean = true,
                              action:String = null,
                              ctrlKey:Boolean = false,
                              altKey:Boolean = false,
                              shiftKey:Boolean = false)
    {
        super(type, bubbles, cancelable);

        this.action = action;
        this.ctrlKey = ctrlKey;
        this.altKey = altKey;
        this.shiftKey = shiftKey;
    }

    //--------------------------------------------------------------------------
    //
    //  Properties
    //
    //--------------------------------------------------------------------------

    //----------------------------------
    //  action
    //----------------------------------

    /**
     *  The requested action.
     *  One of <code>DragManager.COPY</code>, <code>DragManager.LINK</code>,
     *  <code>DragManager.MOVE</code>, or <code>DragManager.NONE</code>.
     *
     *  @see mx.managers.DragManager
     */
    public var action:String;
    
    //----------------------------------
    //  draggedItem
    //----------------------------------

    /**
     *  Contains the child IAutomationObject object being dragged.
     */
    public var draggedItem:IAutomationObject;

    
    /**
     *  The IAutomationObject object which will be parenting the dropped item.
     */
    public var dropParent:IAutomationObject;

    //--------------------------------------------------------------------------
    //
    //  Overridden methods: Event
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override public function clone():Event
    {
        var cloneEvent:AutomationDragEvent = new AutomationDragEvent(type, bubbles, cancelable, 
                                                 action, ctrlKey,
                                                 altKey, shiftKey);

        // Set relevant MouseEvent properties.
        cloneEvent.relatedObject = this.relatedObject;
        cloneEvent.localX = this.localX;
        cloneEvent.localY = this.localY;

        return cloneEvent;
    }
}

}
