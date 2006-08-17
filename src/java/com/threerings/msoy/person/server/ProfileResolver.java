//
// $Id$

package com.threerings.msoy.person.server;

import com.threerings.msoy.item.data.Photo;
import com.threerings.msoy.web.data.Profile;

/**
 * Resolves a person's profile information.
 */
public class ProfileResolver extends BlurbResolver
{
    @Override // from BlurbResolver
    protected void resolve ()
    {
        Profile stub = new Profile();
        stub.memberId = _memberId;
        stub.displayName = "Captain Cleaver";
        stub.photo = new Photo();
        stub.photo.mediaHash = "816cd5aebc2d9d228bf66cff193b81eba1a6ac85";
        stub.photo.mimeType = Photo.IMAGE_JPEG;
        stub.headline = "Arr! Mateys, this here be me profile!";
        stub.homePageURL = "http://www.puzzlepirates.com/";
        stub.isMale = true;
        stub.location = "San Francisco, CA";
        stub.age = 36;
        resolutionCompleted(stub);
    }
}
