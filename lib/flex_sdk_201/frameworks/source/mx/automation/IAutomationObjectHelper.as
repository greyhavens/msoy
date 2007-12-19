////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.automation
{

import flash.events.IEventDispatcher;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;

/**
 * The IAutomationObjectHelper interface defines 
 * helper methods for IAutomationObjects.
 */
public interface IAutomationObjectHelper
{
    /**
     *  Creates an id for a given child within a parent.
     *
     *  @param parent Parent of object for which to create and id.
     *
     *  @param child Object for which to create an id.
     *
     *  @param automationNameCallback A user-supplied function used 
     *  to determine the child's <code>automationName</code>.
     *
     *  @param automationIndexCallback A user-supplied function used 
     *  to determine the child's <code>automationIndex</code>.
     *
     *  @return An AutomationIDPart object representing the child within the parent.
     * 
     */
    function helpCreateIDPart(parent:IAutomationObject,
                              child:IAutomationObject,
                              automationNameCallback:Function = null,
                              automationIndexCallback:Function = null):AutomationIDPart;

    /**
     * Returns an Array of children within a parent which match the id.
     *
     * @param parent Parent object under which the id needs to be resolved.
     *
     * @param part AutomationIDPart object representing the child.
     *
     * @return Array of children which match the id of <code>part</code>.
     */
    function helpResolveIDPart(parent:IAutomationObject,
                               part:Object):Array;

    /**
     * Dispatches a <code>KeyboardEvent.KEY_DOWN</code> and 
     * <code>KeyboardEvent.KEY_UP</code> event 
     * for the specified KeyboardEvent object.
     * 
     * @param to Event dispatcher.
     *
     * @param event Keyboard event.     
     *
     * @return <code>true</code> if the events were dispatched.
     */
    function replayKeyboardEvent(to:IEventDispatcher, event:KeyboardEvent):Boolean;

    /**
     * Dispatches a <code>KeyboardEvent.KEY_DOWN</code> and 
     * <code>KeyboardEvent.KEY_UP</code> event 
     * from the specified IInteractionReplayer, for the specified key, with the
     * specified modifiers.
     * 
     * @param keyCode Key code for key pressed.
     *
     * @param ctrlKey Boolean indicating whether Ctrl key pressed.
     *
     * @param ctrlKey Boolean indicating whether Shift key pressed.
     *
     * @param ctrlKey Boolean indicating whether Alt key pressed.
     *
     * @return <code>true</code> if the events were dispatched.
     */
    function replayKeyDownKeyUp(to:IEventDispatcher,
                                keyCode:uint,
                                ctrlKey:Boolean = false,
                                shiftKey:Boolean = false,
                                altKey:Boolean = false):Boolean;
        
    /**
     * Dispatches a MouseEvent while simulating mouse capture.
     *
     * @param target Event dispatcher.
     *
     * @param event Mouse event.
     *
     * @return <code>true</code> if the event was dispatched.
     */
    function replayMouseEvent(target:IEventDispatcher, event:MouseEvent):Boolean;

    /**
     * Dispatches a <code>MouseEvent.MOUSE_DOWN</code>, <code>MouseEvent.MOUSE_UP</code>, 
     * and <code>MouseEvent.CLICK</code> from the specified IInteractionReplayer with the 
     * specified modifiers.
     *
     * @param to Event dispatcher.
     *
     * @param sourceEvent Mouse event.
     *
     * @return <code>true</code> if the events were dispatched.
     */
    function replayClick(to:IEventDispatcher, sourceEvent:MouseEvent = null):Boolean;

    /**
     * Replays a <code>click</code> event somewhere off the edge of the stage. 
     * use this method to simulate the <code>mouseDownOutside</code> event.
     *
     * @return <code>true</code> if the event was dispatched.
     */
    function replayClickOffStage():Boolean;

    /**
     *  Indicates whether recording is taking place, <code>true</code>, 
     *  or not, <code>false</code>.
     */
    function get recording():Boolean;

    /**
     *  Indicates whether replay is taking place, <code>true</code>, 
     *  or not, <code>false</code>.
     */
    function get replaying():Boolean;

    /**
     *  Adds a synchronization object to the automation manager.
     *  The automation manager waits until the <code>isComplete</code> method
     *  returns <code>true</code>
     *  before proceeding with the next replay event.
     *  
     *  @param isComplete Function that indicates whether the synchronized
     *  operation is completed.
     * 
     *  @param target If null, all replay is stalled until  
     *  the <code>isComplete</code> method returns <code>true</code>, 
     *  otherwise the automation manager will only wait
     *  if the next operation is on the target.
     */
    function addSynchronization(isComplete:Function,
                                target:Object = null):void;

    /**
     *  Determines whether a object is a composite or not.
     *  If a object is not reachable through the automation APIs 
     *  from the top application then it is considered to be a composite.
     *
     *  @param obj Object whose compositeness is to be determined.
     *
     * @return <code>true</code> if the object is a composite.     
     */
    function isAutomationComposite(obj:IAutomationObject):Boolean;
    
    /**
     *  Returns the parent which is compositing the given object.
     *
     *  @param obj Object whose compositing parent is to be determined.
     *
     *  @return The parent IAutomationObject.
     */
    function getAutomationComposite(obj:IAutomationObject):IAutomationObject;

}

}
