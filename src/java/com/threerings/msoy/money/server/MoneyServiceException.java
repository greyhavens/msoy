//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.web.gwt.ServiceException;

/**
 * This is here because I suppose there is some value to divorcing money logic
 * from GWT crud.
 */
public class MoneyServiceException extends MoneyException
{
    public MoneyServiceException (String msg)
    {
        super(msg);
    }

    public MoneyServiceException (String msg, Throwable cause)
    {
        super(msg, cause);
    }

    @Override
    public ServiceException toServiceException ()
    {
        return new ServiceException(getMessage());
    }
}
