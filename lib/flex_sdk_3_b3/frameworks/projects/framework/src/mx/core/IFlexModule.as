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
 *  The IFlexModule interface is used as an optional contract with IFlexModuleFactory.
 *  When an IFlexModule instance is created with the IFlexModuleFactory, the factory
 *  stores a reference to itself after creation.
 */
public interface IFlexModule
{
    /**
     *  @private
     */
    function set moduleFactory(factory:IFlexModuleFactory):void;

    /**
     * @private
     */
    function get moduleFactory():IFlexModuleFactory;

}

}
