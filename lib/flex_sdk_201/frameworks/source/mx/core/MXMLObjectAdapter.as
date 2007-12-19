////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.core
{

/**
 *  The MXMLObjectAdapter class is a stub implementation
 *  of the IMXMLObject interface, so that you can implement
 *  the interface without defining all of the methods.
 *  All implementations are the equivalent of no-ops.
 *  If the method is supposed to return something, it is null, 0, or false.
 */
public class MXMLObjectAdapter implements IMXMLObject
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------
	
	/**
	 *  Constructor.
	 */
    public function MXMLObjectAdapter()
    {
		super();
	}

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

    /**
     *  @inheritDoc
     */
    public function initialized(document:Object, id:String):void
	{
	}
}

}
