////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.logging.targets
{

import mx.core.mx_internal;
import mx.logging.AbstractTarget;
import mx.logging.ILogger;
import mx.logging.LogEvent;

use namespace mx_internal;

/**
 *  All logger target implementations that have a formatted line style output
 *  should extend this class.
 *  It provides default behavior for including date, time, category, and level
 *  within the output.
 *
 */
public class LineFormattedTarget extends AbstractTarget
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
	 *  <p>Constructs an instance of a logger target that will format
	 *  the message data on a single line and pass that line
	 *  to the <code>internalLog()</code> method.</p>
     */
	public function LineFormattedTarget()
	{
		super();

        includeTime = false;
        includeDate = false;
        includeCategory = false;
        includeLevel = false;
	}

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  fieldSeparator
	//----------------------------------

    [Inspectable(category="General", defaultValue=" ")]
    
	/**
     *  The separator string to use between fields (the default is " ")
     */
    public var fieldSeparator:String = " ";

	//----------------------------------
	//  includeCategory
	//----------------------------------

    [Inspectable(category="General", defaultValue="false")]
    
	/**
     *  Indicates if the category for this target should added to the trace.
     */
    public var includeCategory:Boolean;

	//----------------------------------
	//  includeDate
	//----------------------------------

    [Inspectable(category="General", defaultValue="false")]
    
	/**
     *  Indicates if the date should be added to the trace.
     */
    public var includeDate:Boolean;

	//----------------------------------
	//  includeLevel
	//----------------------------------

    [Inspectable(category="General", defaultValue="false")]
    
	/**
     *  Indicates if the level for the event should added to the trace.
     */
    public var includeLevel:Boolean;

	//----------------------------------
	//  includeTime
	//----------------------------------

    [Inspectable(category="General", defaultValue="false")]
    
	/**
     *  Indicates if the time should be added to the trace.
     */
    public var includeTime:Boolean;

	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

    /**
     *  This method handles a <code>LogEvent</code> from an associated logger.
     *  A target uses this method to translate the event into the appropriate
     *  format for transmission, storage, or display.
     *  This method will be called only if the event's level is in range of the
     *  target's level.
     */
    override public function logEvent(event:LogEvent):void
    {
    	var date:String = ""
    	if (includeDate || includeTime)
    	{
    		var d:Date = new Date();
    		if (includeDate)
    		{
    			date = Number(d.getUTCMonth() + 1).toString() + "/" +
					   d.getUTCDate().toString() + "/" + 
					   d.getUTCFullYear() + fieldSeparator;
    		}	
    		if (includeTime)
    		{
    			date = pad(d.getUTCHours()) + ":" +
					   pad(d.getUTCMinutes()) + ":" +
					   pad(d.getUTCSeconds()) + "." +
					   pad(d.getUTCMilliseconds()) + fieldSeparator;
    		}
    	}
    	
        var level:String = "";
        if (includeLevel)
        {
        	level = "[" + LogEvent.getLevelString(event.level) +
				    "]" + fieldSeparator;
        }

 		var category:String = includeCategory ?
							  ILogger(event.target).category + fieldSeparator :
							  "";

        internalLog(date + level + category + event.message);
    }
    
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
	 */
	private function pad(num:Number):String
    {
        return num > 9 ? num.toString() : "0" + num.toString();
    }

	/**
	 *  Descendants of this class should override this method to direct the 
	 *  specified message to the desired output.
	 *
	 *  @param	message String containing preprocessed log message which may
	 *  			include time, date, category, etc. based on property settings,
	 *  			such as <code>includeDate</code>, <code>includeCategory</code>,
	 *			etc.
	 */
	mx_internal function internalLog(message:String):void
	{
		// override this method to perform the redirection to the desired output
	}
}

}
