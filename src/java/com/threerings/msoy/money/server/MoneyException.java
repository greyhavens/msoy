//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.web.gwt.ServiceException;

/**
 * Base type for all exceptions in the MoneyService.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
public abstract class MoneyException extends Exception
{
    public MoneyException (final String message)
    {
        super(message);
    }

    public MoneyException (final String message, final Throwable cause)
    {
        super(message, cause);
    }

    public MoneyException (final Throwable cause)
    {
        super(cause);
    }

    // TODO: you know, why don't we just throw these fuckers directly,
    // and do away with MoneyException?
    public abstract ServiceException toServiceException ();
}
