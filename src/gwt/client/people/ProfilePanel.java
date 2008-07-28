//
// $Id$

package client.people;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.person.gwt.ProfileService;

/**
 * Displays a member's profile.
 */
public class ProfilePanel extends VerticalPanel
{
    public ProfilePanel (int memberId)
    {
        setStyleName("profile");
        // issue a request for this member's profile page data
        CPeople.profilesvc.loadProfile(
            CPeople.ident, _memberId = memberId, new AsyncCallback<ProfileService.ProfileResult>() {
                public void onSuccess (ProfileService.ProfileResult result) {
                    init(result);
                }
                public void onFailure (Throwable cause) {
                    CPeople.log("Failed to load profile data [for=" + _memberId + "].", cause);
                    add(new Label(CPeople.serverError(cause)));
                }
            });
    }

    protected void init (ProfileService.ProfileResult pdata)
    {
        CPeople.frame.setTitle((_memberId == CPeople.getMemberId()) ?
                               CPeople.msgs.profileSelfTitle() :
                               CPeople.msgs.profileOtherTitle(pdata.name.toString()));

        for (int ii = 0; ii < _blurbs.length; ii++) {
            if (_blurbs[ii].shouldDisplay(pdata)) {
                _blurbs[ii].init(pdata);
                add(_blurbs[ii]);
            }
        }
    }

    /** The id of the memebr who's profile we're displaying. */
    protected int _memberId;

    /** The blurbs we'll display on our profile. */
    protected Blurb[] _blurbs = {
        new ProfileBlurb(), new InterestsBlurb(), new FriendsBlurb(), new TrophiesBlurb(),
        new RatingsBlurb(), new GroupsBlurb(), new FeedBlurb(), new CommentsBlurb(),
    };
}
