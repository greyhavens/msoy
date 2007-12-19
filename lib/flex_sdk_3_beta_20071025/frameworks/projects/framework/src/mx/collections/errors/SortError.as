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
 *  This error is thrown when a Sort class is not configured properly;
 *  for example, if the find criteria are invalid.
 */
public class SortError extends Error
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
	 *  @param message A message providing information about the error cause.
     */
    public function SortError(message:String)
    {
        super(message);
    }
}

}
