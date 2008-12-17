//
// $Id$

package com.threerings.msoy.server;

import java.util.List;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.web.gwt.ExternalCreds;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.person.server.persist.ProfileRecord;

/**
 * Handles authentication with an external authentication source.
 */
public abstract class ExternalAuthHandler
{
    public static class Info
    {
        public String displayName;
        public ProfileRecord profile = new ProfileRecord();
        public List<String> friendIds;

        public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }

    /**
     * Validates the supplied external credentials, usually by computing some signature on the
     * credential information and comparing that to a supplied signature.
     *
     * @exception ServiceException thrown if the supplied credentials are not valid.
     */
    public abstract void validateCredentials (ExternalCreds creds)
        throws ServiceException;

    /**
     * Loads up whatever information that is available from the external authentication source
     * which the caller will use to create a Whirled account for the person identified by the
     * supplied credentials.
     */
    public abstract Info getInfo (ExternalCreds creds)
        throws ServiceException;
}
