////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls.videoClasses 
{

import mx.managers.ISystemManager;
import mx.managers.SystemManager;
import mx.resources.ResourceBundle;

/**
 *  The VideoError class represents the error codes for errors 
 *  thrown by the VideoDisplay control.
 *
 *  @see mx.controls.VideoDisplay
 */ 
public class VideoError extends Error 
{
	include "../../core/Version.as";
    
    //--------------------------------------------------------------------------
    //
    //  Class constants
    //
    //--------------------------------------------------------------------------
    
	/**
	 *  @private
	 *  Base error code
	 */
	private static const BASE_ERROR_CODE:uint = 1000;

	/**
	 *  Unable to make connection to server or to find FLV on server.
	 */
	public static const NO_CONNECTION:uint = 1000;

	/**
	 *  No matching cue point found.
	 */
	public static const NO_CUE_POINT_MATCH:uint = 1001;

	/**
	 *  Illegal cue point.
	 */
	public static const ILLEGAL_CUE_POINT:uint = 1002;

	/**
	 *  Invalid seek.
	 */
	public static const INVALID_SEEK:uint = 1003;

	/**
	 *  Invalid content path.
	 */
	public static const INVALID_CONTENT_PATH:uint = 1004;

	/**
	 *  Invalid XML.
	 */
	public static const INVALID_XML:uint = 1005;

	/**
	 *  No bitrate match.
	 */
	public static const NO_BITRATE_MATCH:uint = 1006;

	/**
	 *  Cannot delete default VideoPlayer
	 */
	public static const DELETE_DEFAULT_PLAYER:uint = 1007;
	
	//--------------------------------------------------------------------------
	//
	//  Class initialization
	//
	//--------------------------------------------------------------------------

	loadResources();

	//--------------------------------------------------------------------------
	//
	//  Class resources
	//
	//--------------------------------------------------------------------------

	[ResourceBundle("controls")]

    /**
	 *  @private
     */	
	private static var packageResources:ResourceBundle;
	
    /**
	 *  @private
     */	
	private static var resourceErrorMessages:Array;

    //--------------------------------------------------------------------------
    //
    //  Class variables
    //
    //--------------------------------------------------------------------------

    /**
	 *  @private
     *  Error messages.
     */    
	private static var ERROR_MSG:Array;
	   	
	//--------------------------------------------------------------------------
	//
	//  Class methods
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private    
     *  Loads resources for this class.
     */
	private static function loadResources():void
	{
		resourceErrorMessages = packageResources.getStringArray("errorMessages");
		bundleChanged();
	}
	
    /**
	 *  @private    
     *  Populates localizable properties from the loaded 
	 *  bundle for this class.
     */
	private static function bundleChanged():void
	{
		ERROR_MSG = resourceErrorMessages;
	}
	
    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

	/**
	 *  Constructor
	 *
	 *  @param The error code.
	 *
	 *  @param msg The error message.
	 */
	public function VideoError(errCode:uint, msg:String = null) 
	{
		super();

		_code = errCode;
		
		message = "" + errCode + ": " +
				  ERROR_MSG[errCode - BASE_ERROR_CODE] +
				  ((msg == null) ? "" : (": " + msg));
		
		//name = "VideoError";
	}
	
    //--------------------------------------------------------------------------
    //
    //  Properties
    //
    //--------------------------------------------------------------------------

    //----------------------------------
	//  code
    //----------------------------------

    /**
	 *  @private
	 *  Storage for the code property.
     */      
	private var _code:uint;

	/**
	 *  Contains the error code.
	 */
	public function get code():uint 
	{ 
		return _code; 
	}
}

}
