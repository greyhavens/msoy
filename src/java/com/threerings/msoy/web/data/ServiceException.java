//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * An exception thrown by a remote service when it wishes to communicate a
 * particular error message to a user.
 */
public class ServiceException extends Exception
    implements IsSerializable
{
    /** The standard failure message thrown when the shit hit the fan and we
     * have nothing useful to report to the user. */
    public static final String INTERNAL_ERROR = "m.internal_error";

    /**
     * Creates a service exception with the supplied fully qualified
     * translation message. The qualified message will be decoded into a
     * separate bundle and message because we cannot use the MessageUtil class
     * in JavaScript where this exception will be propagated.
     */
    public ServiceException (String fqmsg)
    {
        // there's no way to set a message in a Throwable; thanks Java!
        super(fqmsg.substring(fqmsg.indexOf(":")+1));

        // do our best to cope if we are not actually provided with a qualified
        // message (which would look like %bundle:message)
        int cidx = fqmsg.indexOf(":");
        _bundle = (cidx == -1) ? "" : fqmsg.substring(1, cidx);
    }

    /**
     * Creatse a service exception with the supplied translatable error code
     * which is defined in the specified message bundle.
     */
    public ServiceException (String bundle, String message)
    {
        super(message);
        _bundle = bundle;
    }

    /**
     * Default constructor for use when unserializing.
     */
    public ServiceException ()
    {
    }

    /**
     * Returns the message bundle in which to look up our error message.
     */
    public String getBundle ()
    {
        return _bundle;
    }

    protected String _bundle;
}
