////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.events
{

/**
 *  The PropertyChangeEventKind class defines the constant values 
 *  for the <code>kind</code> property of the PropertyChangeEvent class.
 * 
 *  @see mx.events.PropertyChangeEvent
 */
public final class PropertyChangeEventKind
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class constants
	//
	//--------------------------------------------------------------------------

    /**
	 *  Indicates that the value of the property changed.
	 */
	public static const UPDATE:String = "update";

    /**
	 *  Indicates that the property was deleted from the object.
	 */
	public static const DELETE:String = "delete";
}

}
