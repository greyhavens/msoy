////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.styles
{

/**
 * Simple interface that defines an <code>unload()</code> method.
 * You can cast an object to an IStyleModule type so that there is no dependency on the StyleModule
 * type in the loading application.
 */
public interface IStyleModule
{
    //--------------------------------------------------------------------------
    //
    //  Methods
    //
    //--------------------------------------------------------------------------

    /**
     * Unloads the style module.
     */
    function unload():void;
}

}
