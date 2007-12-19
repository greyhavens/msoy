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

import flash.text.TextField;

[ExcludeClass]

/**
 *  @private
 *  Interface to create text fields.
 *  Text fields are re-used so there are no more than one per module factory.
 */
public interface ITextFieldFactory
{
	/**
	 *  Creates a TextField object in the context
	 *  of a specified module factory.
	 * 
	 *  @param moduleFactory May not be null.
	 *
	 *  @return A TextField created in the context of the module factory.
	 */
	function createTextField(moduleFactory:IFlexModuleFactory):TextField;
}

}
