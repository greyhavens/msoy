//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Provides services used by all remote service servlets.
 */
public class MsoyServiceServlet extends RemoteServiceServlet
{
    /**
     * Looks up the member information associated with the supplied session authentication
     * information.
     *
     * @exception ServiceException thrown if the session has expired or is otherwise invalid.
     */
    protected MemberRecord requireAuthedUser (WebCreds creds)
        throws ServiceException
    {
        if (creds != null) {
            MemberRecord member = _members.get(creds.token);
            if (member != null && member.memberId == creds.getMemberId()) {
                return member;
            }
        }
        throw new ServiceException(MsoyAuthCodes.SESSION_EXPIRED);
    }

    /**
     * Called when a user logs on or refreshes their credentials to map the user's record by their
     * session token.
     */
    protected void mapUser (WebCreds creds, MemberRecord record)
    {
        _members.put(creds.token, record);
    }

    /** Contains a mapping of authenticated members. TODO: expire records. */
    protected static Map<String,MemberRecord> _members =
        Collections.synchronizedMap(new HashMap<String,MemberRecord>());
}
