////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.resources
{

[ExcludeClass]

/**
 *  @private
 *  When the MXML compiler compiles a resource module, the class
 *  that it autogenerates to represent the module implements this interface.
 */
public interface IResourceModule
{
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  An Array of ResourceBundle instances, containing one for each
	 *  of the resource bundle classes in this resource module.
	 *  
	 *  <p>The order of ResourceBundle instances in this Array
	 *  is not specified.</p>
	 */
	function get resourceBundles():Array /* of ResourceBundle */;
}

}
