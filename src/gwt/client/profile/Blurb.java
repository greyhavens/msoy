//
// $Id$

package client.profile;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.data.BlurbData;
import com.threerings.msoy.web.client.ProfileService;

import client.util.HeaderBox;

/**
 * Contains a chunk of content that a user would want to display on their personal page.
 */
public abstract class Blurb extends HeaderBox
{
    /**
     * Creates the appropriate UI for the specified type of blurb.
     */
    public static Blurb createBlurb (int type)
    {
        switch (type) {
        case BlurbData.PROFILE:
            return new ProfileBlurb();
        case BlurbData.FRIENDS:
            return new FriendsBlurb();
        case BlurbData.GROUPS:
            return new GroupsBlurb();
        case BlurbData.RATINGS:
            return new RatingsBlurb();
        case BlurbData.TROPHIES:
            return new TrophiesBlurb();
        default:
            return null;
        }
    }

    /**
     * Configures this blurb with a context and the member id for whom it is displaying content.
     */
    public void init (int blurbId, ProfileService.ProfileResult pdata)
    {
        _blurbId = blurbId;
        _name = pdata.name;
        didInit(pdata);
    }

    /**
     * Returns true if we should display this blurb, false if we should skip it.
     */
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return true;
    }

    protected Blurb ()
    {
        addStyleName("blurb");
    }

    /**
     * Can be called by the derived class to set the header text of this blurb.
     */
    protected void setHeader (String header)
    {
        setTitle(header);
    }

    /**
     * Called once we have been configured with our context and member info.
     */
    protected abstract void didInit (ProfileService.ProfileResult pdata);

    protected MemberName _name;
    protected int _blurbId;
    protected Label _header;
}
