/*************************************************************************
 * 
 * ADOBE CONFIDENTIAL
 * __________________
 * 
 *  [2002] - [2007] Adobe Systems Incorporated 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 */
package mx.core
{

import flash.display.NativeWindow;

/**
 *  Documentation is not currently available.
 */
public interface IWindow
{
    //--------------------------------------------------------------------------
    //
    //  Properties
    //
    //--------------------------------------------------------------------------

    //----------------------------------
	//  maximizable
    //----------------------------------

	/**
	 *  Documentation is not currently available.
	 */
	function get maximizable():Boolean;
	
    //----------------------------------
	//  minimizable
    //----------------------------------

	/**
	 *  Documentation is not currently available.
	 */
	function get minimizable():Boolean;
	
    //----------------------------------
	//  nativeWindow
    //----------------------------------

	/**
	 *  Documentation is not currently available.
	 */
	function get nativeWindow():NativeWindow

    //----------------------------------
	//  resizable
    //----------------------------------

	/**
	 *  Documentation is not currently available.
	 */
	function get resizable():Boolean;
	
    //----------------------------------
	//  status
    //----------------------------------

	/**
	 *  Documentation is not currently available.
	 */
	function get status():String;
	
	/**
	 *  @private
	 */
	function set status(value:String):void;
	
    //----------------------------------
	//  systemChrome
    //----------------------------------

	/**
	 *  Documentation is not currently available.
	 */
	function get systemChrome():String;
	
    //----------------------------------
	//  title
    //----------------------------------

	/**
	 *  Documentation is not currently available.
	 */
	function get title():String;
	
	/**
	 *  @private
	 */
	function set title(value:String):void;
	
    //----------------------------------
	//  titleIcon
    //----------------------------------

	/**
	 *  Documentation is not currently available.
	 */
	function get titleIcon():Class;
	
	/**
	 *  @private
	 */
	function set titleIcon(value:Class):void;
	
    //----------------------------------
	//  transparent
    //----------------------------------

	/**
	 *  Documentation is not currently available.
	 */
	function get transparent():Boolean;
	
    //----------------------------------
	//  type
    //----------------------------------

	/**
	 *  Documentation is not currently available.
	 */
	function get type():String;
	
    //----------------------------------
	//  visible
    //----------------------------------

	/**
	 *  Documentation is not currently available.
	 */
	function get visible():Boolean;
	 
    //--------------------------------------------------------------------------
    //
    //  Methods
    //
    //--------------------------------------------------------------------------

	/**
	 *  Documentation is not currently available.
	 */
	function close():void; 
	 
	/**
	 *  Documentation is not currently available.
	 */
	function maximize():void
	
	/**
	 *  Documentation is not currently available.
	 */
	function minimize():void;
	
	/**
	 *  Documentation is not currently available.
	 */
	function restore():void;
}

}
