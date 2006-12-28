//
// $Id$

package com.threerings.msoy.web.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.Profile;
import com.threerings.msoy.web.data.WebCreds;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ProfileService}.
 */
public class ProfileServlet extends MsoyServiceServlet
    implements ProfileService
{
    // from interface ProfileService
    public void updateProfile (WebCreds creds, Profile profile)
    {
        try {
            // TODO: this is super hack, we're on the servlet thread and not doing anything
            // properly
            log.info("Updating display name " + creds.memberId +
                " to '" + profile.displayName + "'.");
            MsoyServer.memberRepo.configureDisplayName(creds.memberId, profile.displayName);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to update member's display name " +
                "[mid=" + creds.memberId + ", name=" + profile.displayName + "].", pe);
        }
    }
}
