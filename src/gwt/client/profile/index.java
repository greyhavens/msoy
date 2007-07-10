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
    public void onHistoryChanged (String token)
    {
        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CProfile.ident == null) {
            setContent(MsoyUI.createLabel(CProfile.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        if (token != null && token.length() > 0 && !"me".equals(token) && 
                !token.startsWith("search")) {
            try {
                displayMemberPage(Integer.parseInt(token));
            } catch (Exception e) {
                // TODO: display error
            }

        } else if (token == null || token.equals("") || token.startsWith("search")) {
            displaySearch((token == null || token.equals("")) ? "" : token.substring(7));

        } else if (CProfile.ident != null) {
            // #profile-me falls to here
            displayMemberPage(CProfile.getMemberId());

        } else {
            setContent(new Label(CProfile.msgs.profileLogin()));
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
                setPageTitle(CProfile.msgs.profileTitle());
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

    protected void displaySearch (String args) 
    {
        setPageTitle(CProfile.msgs.profileSearchTitle());
        if (_search == null) {
            _search = new SearchPanel();
        }

        if (args == null || "".equals(args)) {
            _search.clearResults();
            setContent(_search);
        } else {
            String[] argArray = args.split("_", 3);
            if (argArray.length < 3) {
                setContent(new Label(CProfile.msgs.searchParseParamsError()));
                return;
            }

            try {
                final String type = argArray[0];
                final int page = Integer.parseInt(argArray[1]);
                final String search = URL.decodeComponent(argArray[2]);
    
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
            } catch (NumberFormatException nfe) {
                setContent(new Label(CProfile.msgs.searchParseParamsError()));
            }
        }
    }

    protected int _memberId = -1;
    protected SearchPanel _search;
}
