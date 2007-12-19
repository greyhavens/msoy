////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.collections.errors
{

/**
 *  The <code>CollectionViewError</code> class represents general errors
 *  within a collection that are not related to specific activities
 *  such as Cursor seeking.
 *  Errors of this class are thrown by the ListCollectionView class.
 */
public class CollectionViewError extends Error
{
    include "../../core/Version.as";

    //--------------------------------------------------------------------------
    //
    // Constructor.
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
	 *
	 *  @param message A message providing information about the error cause.
     */
    public function CollectionViewError(message:String)
    {
        super(message);
    }
}

}
