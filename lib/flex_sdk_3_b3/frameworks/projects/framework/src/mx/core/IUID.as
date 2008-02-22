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
 *  The IUID interface defines the interface for objects that must have 
 *  Unique Identifiers (UIDs) to uniquely identify the object.
 *  UIDs do not need to be universally unique for most uses in Flex.
 *  One exception is for messages send by data services.
 */
public interface IUID
{
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  uid
	//----------------------------------
	
	/**
	 *  The unique identifier for this object.
     */
    function get uid():String;
    
    /**
     *  @private
     */
    function set uid(value:String):void;
}

}
