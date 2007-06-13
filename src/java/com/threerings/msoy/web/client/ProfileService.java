//
// $Id$

package com.threerings.msoy.web.client;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.Profile;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Defines profile-related services available to the GWT/AJAX web client.
 */
public interface ProfileService extends RemoteService
{
    /**
     * Requests that this user's profile be updated.
     */
    public void updateProfile (WebIdent ident, String displayName, Profile profile)
        throws ServiceException;

    /**
     * Loads the blurbs for the specified member's profile page. The first entry in the list will
     * be information on the page layout and subsequent entries will be data for each of the blurbs
     * on the page.
     */
    public ArrayList loadProfile (int memberId) throws ServiceException;

    /**
     * Looks for profiles that match the specified search term. We'll aim to be smart about what we
     * search. Returns a (possibly empty) list of {@link MemberCard} records.
     */
    public ArrayList findProfiles (String type, String search) throws ServiceException;
}
