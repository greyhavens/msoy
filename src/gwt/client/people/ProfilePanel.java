//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.profile.gwt.ProfileServiceAsync;

import client.shell.CShell;
import client.util.ServiceUtil;

/**
 * Displays a member's profile.
 */
public class ProfilePanel extends VerticalPanel
{
    public ProfilePanel (int memberId)
    {
        // TODO - make public - add galleries to the list of blurbs in DEV only
        if (DeploymentConfig.devDeployment) {
            _blurbs = new Blurb[] { new ProfileBlurb(), new InterestsBlurb(), new FriendsBlurb(),
                new StampsBlurb(), new GalleriesBlurb(), new TrophiesBlurb(), new RatingsBlurb(),
                new GroupsBlurb(), new FavoritesBlurb(), new FeedBlurb(), new CommentsBlurb() };
        }

        setStyleName("profile");
        _memberId = memberId;
        // issue a request for this member's profile page data
        _profilesvc.loadProfile(memberId, new AsyncCallback<ProfileService.ProfileResult>() {
            public void onSuccess (ProfileService.ProfileResult result) {
                init(result);
            }
            public void onFailure (Throwable cause) {
                CShell.log("Failed to load profile data [for=" + _memberId + "].", cause);
                add(new Label(CShell.serverError(cause)));
            }
        });
    }

    protected void init (ProfileService.ProfileResult pdata)
    {
        CShell.frame.setTitle((_memberId == CShell.getMemberId()) ?
                              _msgs.profileSelfTitle() :
                              _msgs.profileOtherTitle(pdata.name.toString()));

        for (Blurb _blurb : _blurbs) {
            if (_blurb.shouldDisplay(pdata)) {
                _blurb.init(pdata);
                add(_blurb);
            }
        }
    }

    /** The id of the member who's profile we're displaying. */
    protected int _memberId;

    /** The blurbs we'll display on our profile. */
    protected Blurb[] _blurbs = {
        new ProfileBlurb(), new InterestsBlurb(), new FriendsBlurb(), new StampsBlurb(),
        new TrophiesBlurb(), new RatingsBlurb(), new GroupsBlurb(),
        new FavoritesBlurb(),
        new FeedBlurb(), new CommentsBlurb()
    };

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final ProfileServiceAsync _profilesvc = (ProfileServiceAsync)
        ServiceUtil.bind(GWT.create(ProfileService.class), ProfileService.ENTRY_POINT);
}
