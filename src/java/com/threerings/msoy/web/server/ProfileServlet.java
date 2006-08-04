//
// $Id$

package com.threerings.msoy.web.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.threerings.msoy.item.data.Photo;
import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.Profile;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Provides the server implementation of {@link ProfileService}.
 */
public class ProfileServlet extends RemoteServiceServlet
    implements ProfileService
{
    // from interface ProfileService
    public Profile loadProfile (WebCreds creds, int memberId)
    {
        Profile stub = new Profile();
        stub.memberId = memberId;
        stub.displayName = "Captain Cleaver";
        stub.photo = new Photo();
        stub.photo.mediaHash = "816cd5aebc2d9d228bf66cff193b81eba1a6ac85";
        stub.photo.mimeType = Photo.IMAGE_JPEG;
        stub.headline = "Arr! Mateys, this here be me profile!";
        stub.homePageURL = "http://www.puzzlepirates.com/";
        stub.isMale = true;
        stub.location = "San Francisco, CA";
        stub.age = 36;
        return stub;
    }

    // from interface ProfileService
    public void updateProfileHeader (
        WebCreds creds, String displayName, String homePageURL, String headline)
    {
    }

    // from interface ProfileService
    public void updateProfileDetails (
        WebCreds creds, boolean isMale, long birthday, String location)
    {
    }
}
