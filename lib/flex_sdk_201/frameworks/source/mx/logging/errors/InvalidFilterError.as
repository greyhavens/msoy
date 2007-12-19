////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.logging.errors
{

/**
 *  This error is thrown when a filter specified for a target
 *  contains invalid characters or is malformed.
 *  This error is thrown by the following methods/properties:
 *  <ul>
 *    <li><code>ILoggerTarget.filters</code> if a filter expression
 *    in this listis malformed.</li>
 *  </ul>
 */
public class InvalidFilterError extends Error
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
	 *  @param message The message that describes this error.
     */
    public function InvalidFilterError(message:String)
    {
        super(message);
    }

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

         /**
	 *  Returns the messge as a String.
	 *  
	 *  @return The message.
	 */
    public function toString():String
    {
        return String(message);
    }
}

}
