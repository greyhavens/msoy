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
 *  The IIMESupport interface defines the interface for any component that supports IME 
 *  (input method editor).
 *  IME is used for entering characters in Chinese, Japanese, and Korean.
 * 
 *  @see flash.system.IME
 */
public interface IIMESupport
{
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  imeMode
	//----------------------------------

	/**
	 *  The IME mode of the component.
	 */
	function get imeMode():String;

	/**
	 *  @private
	 */
	function set imeMode(value:String):void;
}

}
