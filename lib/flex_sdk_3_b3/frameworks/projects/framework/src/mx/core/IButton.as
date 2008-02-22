////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.core
{

/**
 *  The IButton interface is a marker interface that indicates that a component
 *  acts as a button.
 */
public interface IButton extends IUIComponent
{
    /**
     *  @copy mx.controls.Button#emphasized
     */
    function get emphasized():Boolean;
    function set emphasized(value:Boolean):void;

    /**
     *  @copy mx.core.UIComponent#callLater()
     */
    function callLater(method:Function,
                              args:Array /* of Object */ = null):void
}

}
