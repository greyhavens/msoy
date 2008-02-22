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

/**
 *  The IProgrammaticSkin interface defines the interface that skin classes must implement 
 *  if they use the <code>name</code> property skin interface. 
 */
public interface IProgrammaticSkin
{
    /**
     *  @copy mx.skins.ProgrammaticSkin#validateNow()
     */
    function validateNow():void;

    /**
     *  @copy mx.skins.ProgrammaticSkin#validateDisplayList()
     */
    function validateDisplayList():void;
}

}