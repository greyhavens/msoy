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
import flash.display.DisplayObject;

/**
 * The IAutomationMouseSimulator interface describes an object 
 * that simulates mouse movement so that components
 * capturing the mouse use the simulated versions of the mouse
 * cursor instead of the live Flash Player version. Implementors of
 * the IUIComponent interface should override the 
 * <code>mouseX</code> and <code>mouseY</code> properties and
 * call the active simulator's version if a simulator is present.
 *
 *  @see mx.core.IUIComponent
 */
public interface IAutomationMouseSimulator
{
    /**
     *  Called when a DisplayObject retrieves the <code>mouseX</code> property.
     *
     *  @param item DisplayObject that simulates mouse movement.
     *
     *  @return The x coordinate of the mouse position relative to item.
     */
    function getMouseX(item:DisplayObject):Number;

    /**
     *  Called when a DisplayObject retrieves <code>mouseY</code> property.
     *
     *  @param item DisplayObject that simulates mouse movement.
     *
     *  @return The y coordinate of the mouse position relative to item.
     */
    function getMouseY(item:DisplayObject):Number;
}

}
