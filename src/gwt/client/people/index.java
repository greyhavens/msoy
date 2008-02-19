//
// $Id$

package client.people;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.person.data.ProfileLayout;
import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.client.ProfileService;

import client.util.MsoyUI;
import client.msgs.MsgsEntryPoint;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;

/**
 * Displays a profile's "portal" page with their profile information, friends,
 * and whatever else they want showing on their page.
 */
public class index extends MsgsEntryPoint
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
            }
        };
    }

    // @Override // from Page
    public void onHistoryChanged (Args args)
    {
        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CPeople.ident == null) {
            setContent(MsoyUI.createLabel(CPeople.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        String action = args.get(0, "");
        if (action.equals("search")) {
            displaySearch(args);

        } else if (action.equals("f")) {
            setContent(new FriendsPanel(args.get(1, 0)));

        } else if (action.equals("invites")) {
            setContent(new SendInvitesPanel());

        } else if (args.get(0, 0) != 0) {
            displayMemberPage(args.get(0, 0));

        } else if (CPeople.getMemberId() != 0) {
            setContent(new FriendsPanel(CPeople.getMemberId()));

        } else {
            setContent(new PeoplePanel());
        }
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return PEOPLE;
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CPeople.msgs = (PeopleMessages)GWT.create(PeopleMessages.class);
    }

    protected void displayMemberPage (int memberId)
    {
        // issue a request for this member's profile page data
        CPeople.profilesvc.loadProfile(CPeople.ident, _memberId = memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                ProfileService.ProfileResult pdata = (ProfileService.ProfileResult)result;
                Frame.setTitle(CPeople.msgs.profileTitle(), pdata.name.toString());
                switch (pdata.layout.layout) {
                default:
                case ProfileLayout.ONE_COLUMN_LAYOUT:
                    setContent(new OneColumnLayout(pdata));
                    break;
                case ProfileLayout.TWO_COLUMN_LAYOUT:
                    setContent(new TwoColumnLayout(pdata));
                    break;
                }
            }

            public void onFailure (Throwable cause) {
                setContent(new Label(CPeople.serverError(cause)));
                CPeople.log("Failed to load blurbs", cause);
            }
        });
    }

    protected void displaySearch (Args args) 
    {
        if (_search == null) {
            _search = new SearchPanel();
        }
        _search.setArgs(args);
        setContent(_search);
    }

    protected int _memberId = -1;
    protected SearchPanel _search;
}
