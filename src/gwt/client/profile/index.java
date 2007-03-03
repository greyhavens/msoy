//
// $Id$

package client.profile;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.ProfileLayout;
import com.threerings.msoy.web.data.WebCreds;

import client.util.MsoyUI;
import client.msgs.MsgsEntryPoint;
import client.shell.MsoyEntryPoint;

/**
 * Displays a profile's "portal" page with their profile information, friends,
 * and whatever else they want showing on their page.
 */
public class index extends MsgsEntryPoint
    implements HistoryListener
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public MsoyEntryPoint createEntryPoint () {
                return new index();
            }
        };
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        try {
            displayMemberPage(Integer.parseInt(token));
        } catch (Exception e) {
            // TODO: display error
        }
    }

    // @Override // from MsoyEntryPoint
    protected String getPageId ()
    {
        return "profile";
    }

    // @Override // from MsoyEntryPoint
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CProfile.msgs = (ProfileMessages)GWT.create(ProfileMessages.class);
    }

    // @Override // from MsoyEntryPoint
    protected void onPageLoad ()
    {
        History.addHistoryListener(this);
    }

    // @Override // from MsoyEntryPoint
    protected boolean didLogon (WebCreds creds)
    {
        boolean header = super.didLogon(creds);
        if (_memberId == -1) {
            String initToken = History.getToken();
            if (initToken.length() > 0) {
                onHistoryChanged(initToken);
            } else {
                displayMemberPage(creds.getMemberId());
            }
        }
        return header;
    }

    // @Override // from MsoyEntryPoint
    protected void didLogoff ()
    {
        super.didLogoff();

        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CProfile.creds == null) {
            setContent(MsoyUI.createLabel(CProfile.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        if (_memberId == -1) {
            String initToken = History.getToken();
            if (initToken.length() > 0) {
                onHistoryChanged(initToken);
            } else {
                // TODO: display member search interface
                setContent(new Label(CProfile.msgs.indexLogon()));
            }
        }
    }

    protected void displayMemberPage (int memberId)
    {
        // issue a request for this member's profile page data
        CProfile.profilesvc.loadProfile(_memberId = memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                ArrayList data = (ArrayList)result;
                ProfileLayout layout = (ProfileLayout)data.remove(0);
                switch (layout.layout) {
                default:
                case ProfileLayout.ONE_COLUMN_LAYOUT:
                    setContent(new OneColumnLayout(_memberId, layout, data));
                    break;
                case ProfileLayout.TWO_COLUMN_LAYOUT:
                    setContent(new TwoColumnLayout(_memberId, layout, data));
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
