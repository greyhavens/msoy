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

import flash.net.LocalConnection;
import mx.core.mx_internal;

use namespace mx_internal;

/**
 *  Provides a logger target that outputs to a <code>LocalConnection</code>,
 *  connected to the MiniDebug application.
 */
public class MiniDebugTarget extends LineFormattedTarget
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
	 *  <p>Constructs an instance of a logger target that will send
	 *  the log data to the MiniDebug application.</p>
	 *
     *  @param connection Specifies where to send the logging information.
     *  This value is the name of the connection specified in the
     *  <code>LocalConnection.connect()</code> method call in the remote SWF,
     *  that can receive calls to a <code>log()</code> method with the
     *  following signature: 
     *  <pre>
     *    log(... args:Array)
     *  </pre> 
     *  Each value specified in the <code>args</code> Array is a String.
     *
     *  @param method Specifies what method to call on the remote connection.
     */
    public function MiniDebugTarget(connection:String = "_mdbtrace",
									method:String = "trace")
    {
        super();

        _lc = new LocalConnection();
        _connection = connection;
        _method = method;
    }

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

    /**
     *  @private
     */
    private var _lc:LocalConnection;
    
    /**
     *  @private
     *  The name of the method that we should call on the remote connection.
     */
    private var _method:String;

    /**
     *  @private
     *  The name of the connection that we should send to.
     */
    private var _connection:String;

	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
     *  This method outputs the specified message directly to the method
     *  specified (passed to the constructor) for the local connection.
     *
	 *  @param message String containing preprocessed log message which may
	 *  include time, date, category, etc. based on property settings,
	 *  such as <code>includeDate</code>, <code>includeCategory</code>, etc.
	 */
	override mx_internal function internalLog(message:String):void
	{
        _lc.send(_connection, _method, message);
    }
}

}
