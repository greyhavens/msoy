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
	
/**
 *  Static class containing constants for use in the <code>level</code>
 *  property.
 */
public final class LogEventLevel
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class constants
	//
	//--------------------------------------------------------------------------

    /**
     *  Designates events that are very
     *  harmful and will eventually lead to application failure.
     */
    public static const FATAL:int = 1000;

    /**
     *  Designates error events that might
     *  still allow the application to continue running.
     */
    public static const ERROR:int = 8;
    
    /**
     *  Designates events that could be
     *  harmful to the application operation.
     */
    public static const WARN:int = 6;
    
    /**
     *  Designates informational messages that
     *  highlight the progress of the application at coarse-grained level.
     */
    public static const INFO:int = 4;
    
    /**
     *  Designates informational level
     *  messages that are fine grained and most helpful when debugging an
     *  application.
     */
    public static const DEBUG:int = 2;
    
    /**
     *  Tells a target to process all messages.
     */
    public static const ALL:int = 0;
}

}
