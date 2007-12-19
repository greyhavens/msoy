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
 *  The ContainerCreationPolicy class defines the constant values
 *  for the <code>cachePolicy</code> property of the UIComponent class.
 *
 *  @see mx.core.UIComponent#cachePolicy
 */
public final class UIComponentCachePolicy
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class constants
	//
	//--------------------------------------------------------------------------

    /**
     *  Specifies that the Flex framework should use heuristics
	 *  to decide whether to cache the object as a bitmap.
     */
    public static const AUTO:String = "auto";
    
	/**
     *  Specifies that the Flex framework should never attempt
	 *  to cache the object as a bitmap.
     */
    public static const OFF:String = "off";
    
	/**
     *  Specifies that the Flex framework should always cache
	 *  the object as a bitmap.
     */
    public static const ON:String = "on";
}

}
