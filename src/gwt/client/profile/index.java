//
// $Id$

package client.profile;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
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

        if (token != null && token.length() > 0) {
            try {
                displayMemberPage(Integer.parseInt(token));
            } catch (Exception e) {
                // TODO: display error
            }

        } else if (CProfile.ident != null) {
            // display our own profile if we're logged in
            displayMemberPage(CProfile.getMemberId());

        } else {
            // TODO: display member search interface
            setContent(MsoyUI.createLabel(CProfile.msgs.indexLogon(), "infoLabel"));
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
        CProfile.profilesvc.loadProfile(_memberId = memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                ArrayList data = (ArrayList)result;
                ProfileLayout layout = (ProfileLayout)data.remove(0);
                MemberName name = (MemberName)data.remove(0);
                setPageTitle(CProfile.msgs.profileTitle());
                switch (layout.layout) {
                default:
                case ProfileLayout.ONE_COLUMN_LAYOUT:
                    setContent(new OneColumnLayout(name, layout, data));
                    break;
                case ProfileLayout.TWO_COLUMN_LAYOUT:
                    setContent(new TwoColumnLayout(name, layout, data));
                    break;
                }
            }

            public void onFailure (Throwable cause) {
                setContent(new Label(CProfile.serverError(cause)));
                CProfile.log("Failed to load blurbs", cause);
            }
        });
    }

    protected int _memberId = -1;
}
