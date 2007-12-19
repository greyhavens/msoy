////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.logging
{

import flash.events.EventDispatcher;
import mx.managers.ISystemManager;
import mx.managers.SystemManager;
import mx.resources.ResourceBundle;

/**
 *  The logger that is used within the logging framework.
 *  This class dispatches events for each message logged using the <code>log()</code> method.
 */
public class LogLogger extends EventDispatcher implements ILogger
{
	include "../core/Version.as";

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

	[ResourceBundle("logging")]

	/**
	 *  @private
	 */	
	private static var packageResources:ResourceBundle;

	/**
	 *  @private
	 */	
	private static var resourceLevelLimit:String;

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
		resourceLevelLimit = packageResources.getString("levelLimit");
	}

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function LogLogger(category:String)
	{
		super();

		_category = category;
	}

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  category
	//----------------------------------

	/**
	 *  @private
	 *  Storage for the category property.
	 */
	private var _category:String;

	/**
	 *  The category this logger send messages for.
	 */	
	public function get category():String
	{
		return _category;
	}
	
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @inheritDoc
	 */	
	public function log(level:int, msg:String, ... rest):void
	{
		// we don't want to allow people to log messages at the 
		// Log.Level.ALL level, so throw a RTE if they do
		if (level < LogEventLevel.DEBUG)
        	throw new ArgumentError(resourceLevelLimit);
        	
		if (hasEventListener(LogEvent.LOG))
		{
			// replace all of the parameters in the msg string
			for(var i:uint = 0; i<rest.length; i++)
				msg = msg.replace(new RegExp("\\{"+i+"\\}", "g"), rest[i]);

			dispatchEvent(new LogEvent(msg, level));
		}
	}

	/**
	 *  @inheritDoc
	 */	
	public function debug(msg:String, ... rest):void
	{
		if(hasEventListener(LogEvent.LOG))
		{
			// replace all of the parameters in the msg string
			for(var i:uint = 0; i<rest.length; i++)
				msg = msg.replace(new RegExp("\\{"+i+"\\}", "g"), rest[i]);

			dispatchEvent(new LogEvent(msg, LogEventLevel.DEBUG));
		}
	}

	/**
	 *  @inheritDoc
	 */	
	public function error(msg:String, ... rest):void
	{
		if(hasEventListener(LogEvent.LOG))
		{
			// replace all of the parameters in the msg string
			for(var i:uint = 0; i<rest.length; i++)
				msg = msg.replace(new RegExp("\\{"+i+"\\}", "g"), rest[i]);

			dispatchEvent(new LogEvent(msg, LogEventLevel.ERROR));
		}
	}

	/**
	 *  @inheritDoc
	 */	
	public function fatal(msg:String, ... rest):void
	{
		if(hasEventListener(LogEvent.LOG))
		{
			// replace all of the parameters in the msg string
			for(var i:uint = 0; i<rest.length; i++)
				msg = msg.replace(new RegExp("\\{"+i+"\\}", "g"), rest[i]);

			dispatchEvent(new LogEvent(msg, LogEventLevel.FATAL));
		}
	}

	/**
	 *  @inheritDoc
	 */	
	public function info(msg:String, ... rest):void
	{
		if(hasEventListener(LogEvent.LOG))
		{
			// replace all of the parameters in the msg string
			for(var i:uint = 0; i<rest.length; i++)
				msg = msg.replace(new RegExp("\\{"+i+"\\}", "g"), rest[i]);

			dispatchEvent(new LogEvent(msg, LogEventLevel.INFO));
		}
	}

	/**
	 *  @inheritDoc
	 */	
	public function warn(msg:String, ... rest):void
	{
		if(hasEventListener(LogEvent.LOG))
		{
			// replace all of the parameters in the msg string
			for(var i:uint = 0; i<rest.length; i++)
				msg = msg.replace(new RegExp("\\{"+i+"\\}", "g"), rest[i]);

			dispatchEvent(new LogEvent(msg, LogEventLevel.WARN));
		}
	}
}

}
