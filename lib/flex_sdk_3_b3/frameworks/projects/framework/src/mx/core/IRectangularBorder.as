////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.core
{
import flash.geom.Rectangle;

/**
 *  The IRectangularBorder interface defines the interface that all classes 
 *  used for rectangular border skins should implement.
 *
 */
public interface IRectangularBorder extends IBorder
{
    //--------------------------------------------------------------------------
    //
    //  Properties
    //
    //--------------------------------------------------------------------------
    
    //----------------------------------
    //  backgroundImageBounds
    //----------------------------------

    /**
     *  @copy mx.skins.RectangularBorder#backgroundImageBounds
     */
    function get backgroundImageBounds():Rectangle;
    function set backgroundImageBounds(value:Rectangle):void;

    //----------------------------------
    //  hasBackgroundImage
    //----------------------------------

    /**
     *  @copy mx.skins.RectangularBorder#hasBackgroundImage
     */
    function get hasBackgroundImage():Boolean;

    //----------------------------------
    //  adjustBackgroundImage
    //----------------------------------

    /**
     *  @copy mx.skins.RectangularBorder#layoutBackgroundImage()
     */
    function layoutBackgroundImage():void;
}

}
