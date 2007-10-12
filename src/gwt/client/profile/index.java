//
// $Id$

package client.profile;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.ProfileLayout;

import client.util.MsoyUI;
import client.msgs.MsgsEntryPoint;
import client.shell.Args;
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
        if (!DeploymentConfig.devDeployment && CProfile.ident == null) {
            setContent(MsoyUI.createLabel(CProfile.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        if (args.get(0, "").equals("search")) {
            displaySearch(args);
            return;
        }

        if (args.get(0, "").equals("f")) {
            setContent(new FriendsPanel(this, args.get(1, 0)));
            return;
        }

        int memberId = args.get(0, 0);
        if (memberId != 0) {
            displayMemberPage(memberId);
            return;
        }
        // #profile-me falls through

        if (CProfile.ident != null) {
            displayMemberPage(CProfile.getMemberId());
        } else {
            displaySearch(args);
        }
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return "profile";
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CProfile.msgs = (ProfileMessages)GWT.create(ProfileMessages.class);
    }

    protected void displayMemberPage (int memberId)
    {
        // issue a request for this member's profile page data
        CProfile.profilesvc.loadProfile(CProfile.ident, _memberId = memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                ProfileService.ProfileResult pdata = (ProfileService.ProfileResult)result;
                setPageTitle(CProfile.msgs.profileTitle(), pdata.name.toString());
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
                setContent(new Label(CProfile.serverError(cause)));
                CProfile.log("Failed to load blurbs", cause);
            }
        });
    }

    protected void displaySearch (Args args) 
    {
        setPageTitle(CProfile.msgs.profileSearchTitle());
        if (_search == null) {
            _search = new SearchPanel();
        }

        final String type = args.get(1, "name");
        final int page = args.get(2, 0);
        final String search = URL.decodeComponent(args.get(3, ""));

        if (search.length() == 0) {
            _search.clearResults();
            setContent(_search);
            return;
        }

        if (!_search.showingResultsFor(type, search)) {
            CProfile.profilesvc.findProfiles(type, search, new AsyncCallback() {
                public void onSuccess (Object result) {
                    _search.setResults((List) result, page, type, search);
                    setContent(_search);
                }
                public void onFailure (Throwable cause) {
                    setContent(new Label(CProfile.serverError(cause)));
                    CProfile.log("Failed to load search", cause);
                }
            });

        } else {
            _search.displayPage(page);
        }
    }

    protected int _memberId = -1;
    protected SearchPanel _search;
}
