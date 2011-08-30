//
// $Id$

package com.threerings.msoy.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.server.ClientManager;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.data.all.VisitorInfo;

import static com.threerings.msoy.Log.log;

/**
 * Provides some common methods for authentication with an msoy server.
 */
@Singleton
public class AuthLogic
{
    /**
     * Verifies that an ident is valid.
     */
    public static boolean isValidIdent (String ident)
    {
        if (ident == null || ident.length() != 48) {
            return false;
        }
        return ident.substring(40, 48).equals(generateIdentChecksum(ident.substring(0, 40)));
    }

    /**
     * Checks whether this authentication failure is due to a purged permaguest account. If true,
     * magicks up a new visitorId for the authenticator because they're going to need it when we
     * subsequently create them a new permaguest account.
     */
    public static boolean fixPurgedPermaguest (ServiceException cause, MsoyCredentials creds)
    {
        final String aname = creds.getUsername().toString().toLowerCase();
        boolean userNotFound = cause.getMessage().equals(MsoyAuthCodes.NO_SUCH_USER) ||
            cause.getMessage().equals(MsoyAuthCodes.INVALID_LOGON);
        if (userNotFound && MemberMailUtil.isPermaguest(aname)) {
            log.info("Coping with expired permaguest", "oldacct", aname);
            // we need to fake up a new visitor id since the old one is now long gone
            if (creds.visitorId == null) {
                creds.visitorId = new VisitorInfo().id;
            }
            return true;
        }
        return false;
    }

    /**
     * Generate a new unique ident for this flash client.
     */
    public static String generateIdent (String accountName, int offset)
    {
        String seed = StringUtil.sha1hex(
            Long.toHexString(System.currentTimeMillis() + offset*1000L) + accountName);
        return seed + generateIdentChecksum(seed);
    }

    /**
     * Generates a checksum for an ident.
     */
    protected static String generateIdentChecksum (final String seed)
    {
        return StringUtil.sha1hex(seed.substring(10, 20) + seed.substring(30, 40) +
            seed.substring(20, 30) + seed.substring(0, 10)).substring(0, 8);
    }

    // dependencies
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected ClientManager _clmgr;
    @Inject protected RuntimeConfig _runtime;
}
