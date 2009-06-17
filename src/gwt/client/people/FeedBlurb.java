//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;

import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.profile.gwt.ProfileServiceAsync;

import client.person.PlayerFeedPanel;

/**
 * Displays a member feed on the member's profile
 */
public class FeedBlurb extends Blurb
{
    @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return true;
    }

    @Override // from Blurb
    public void init (final ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(_msgs.feedTitle());
        setContent(new PlayerFeedPanel(_msgs.emptySelfFeed(""+pdata.name), pdata.feed));
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final ProfileServiceAsync _profilesvc = (ProfileServiceAsync)
        ServiceUtil.bind(GWT.create(ProfileService.class), ProfileService.ENTRY_POINT);
}
