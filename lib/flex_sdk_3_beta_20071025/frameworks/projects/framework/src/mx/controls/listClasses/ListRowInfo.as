////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls.listClasses
{

/**
 *  Used by the list-based classes to store information about their IListItemRenderers.
 *
 *  @see mx.controls.listClasses.ListBase#rowInfo
 */
public class ListRowInfo
{
	include "../../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 *
	 *  @param y The y-position value for the row.
	 *
	 *  @param height The height of the row including margins.
	 *
	 *  @param uid The unique identifier of the item in the dataProvider
	 *
	 *  @param data The item in the dataprovider.
	 */
	public function ListRowInfo(y:Number, height:Number,
								uid:String, data:Object = null)
	{
		super();

		this.y = y;
		this.height = height;
		this.uid = uid;
		this.data = data;
	}

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  data
	//----------------------------------

	/**
	 *  The item in the dataprovider. 
	 */
	public var data:Object; 

	//----------------------------------
	//  height
	//----------------------------------

	/**
	 *  The height of the row including margins.
	 */
	public var height:Number; 

	//----------------------------------
	//  itemOldY
	//----------------------------------

	/**
	 *  The last Y value for the renderer.
	 *  Used in Tree's open/close effects.
	 */
	public var itemOldY:Number; 

	//----------------------------------
	//  oldY
	//----------------------------------

	/**
	 *  The last Y value for the row.
	 *  Used in Tree's open/close effects.
	 */
	public var oldY:Number; 

	//----------------------------------
	//  uid
	//----------------------------------

	/**
	 *  The unique identifier of the item in the dataProvider
	 */
	public var uid:String; 

	//----------------------------------
	//  y
	//----------------------------------

	/**
	 *  The y-position value for the row.
	 */
	public var y:Number; 
}

}
