////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.collections
{

/**
 *  @private
 * 
 *  The ItemWrapper class is a simple envelope for an item in a collection.
 *  Its purpose is to provide a way of distinguishing between duplicate items
 *  in a collection -- i.e., giving them unique IDs. It is used by data change
 *  effects for classes derived by ListBase. Distinguishing between duplicate
 *  elements is particularly important for data change effects because it is
 *  necessary to assign common item renderers to common items in a collection
 */
public class ItemWrapper
{
    include "../core/Version.as";
    
	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructs an instance of the wrapper with the specified data.
	 * 
	 *  @param data The data element to be wrapped.
	 */
	public function ItemWrapper(data:Object)
	{
		super();
		this.data = data;
	}
	
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------
	
	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	/**
	 *  The data item being wrapped.
	 */ 
    public var data:Object;

}


}