//
// $Id$

package com.threerings.msoy.web.server;

import java.util.logging.Level;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.samskivert.io.PersistenceException;

import com.threerings.util.Name;

import com.threerings.msoy.item.data.Photo;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.Profile;
import com.threerings.msoy.web.data.WebCreds;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ProfileService}.
 */
public class ProfileServlet extends RemoteServiceServlet
    implements ProfileService
{
    // from interface ProfileService
    public void updateProfileHeader (
        WebCreds creds, String displayName, String homePageURL, String headline)
    {
        try {
            // TODO: this is super hack, we're on the servlet thread and not
            // doing anything properly
            log.info("Updating display name " + creds.memberId +
                " to '" + displayName + "'.");
            MsoyServer.memberRepo.configureMember(
                creds.memberId, new Name(displayName));
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to update member's display name " +
                "[mid=" + creds.memberId + ", name=" + displayName + "].", pe);
        }
    }

    // from interface ProfileService
    public void updateProfileDetails (
        WebCreds creds, boolean isMale, long birthday, String location)
    {
    }
}
