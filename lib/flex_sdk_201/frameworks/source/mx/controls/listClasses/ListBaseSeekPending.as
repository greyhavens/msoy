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

import mx.collections.CursorBookmark;

/**
 *  An object that stores data about a seek operation
 *  that was interrupted by an ItemPendingError error.
 *
 *  @see mx.collections.errors.ItemPendingError
 *  @see mx.controls.listClasses.ListBase#lastSeekPending
 */
public class ListBaseSeekPending
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
	 *  @param bookmark The bookmark that was being used in the 
	 *                  seek operation.
	 *  @param offset The offset from the bookmark that was the target of
	 *                  the seek operation.
	 */
	public function ListBaseSeekPending(bookmark:CursorBookmark, offset:int)
	{
		super();

		this.bookmark = bookmark;
		this.offset = offset;
	}

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  bookmark
	//----------------------------------

	/**
	 *  The bookmark that was being used in the seek operation.
	 */
	public var bookmark:CursorBookmark;

	//----------------------------------
	//  offset
	//----------------------------------

	/**
	 *  The offset from the bookmark that was the target of the seek operation.
	 */
	public var offset:int;
}

}