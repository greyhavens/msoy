//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.Profile;

import com.threerings.gwt.ui.PagedGrid;

import client.profile.CProfile;

/**
 * A Grid of profiles.  This is useful for profile searching, or displaying collections of profiles
 * on other pages, such as group page or friends lists.
 */
public class ProfileGrid extends PagedGrid
{
    public ProfileGrid (int height, int width) 
    {
        super(height, width);
    }

    protected Widget createWidget (Object item) {
        return new ProfileWidget((Profile)item);
    }
    protected String getEmptyMessage () {
        return CProfile.msgs.gridNoProfiles();
    }

    protected class ProfileWidget extends FlexTable
    {
        public ProfileWidget (Profile profile) 
        {
        }
    }
}
