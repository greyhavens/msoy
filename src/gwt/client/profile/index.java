//
// $Id$

package client.profile;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import com.threerings.msoy.web.data.PersonLayout;
import com.threerings.msoy.web.data.WebCreds;

import client.shell.MsoyEntryPoint;

/**
 * Displays a person's "portal" page with their profile information, friends,
 * and whatever else they want showing on their page.
 */
public class index extends MsoyEntryPoint
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
    protected void onPageLoad ()
    {
        History.addHistoryListener(this);
        String initToken = History.getToken();
        if (initToken.length() > 0) {
            onHistoryChanged(initToken);
        } else {
            setContent(new Label("Log in to view your member page."));
        }
    }

    // @Override // from MsoyEntryPoint
    protected void didLogon (WebCreds creds)
    {
        super.didLogon(creds);

        // if we're not displaying someone elses member page, display our own
        displayMemberPage(_memberId != -1 ? _memberId : _ctx.creds.memberId);
    }

    protected void displayMemberPage (int memberId)
    {
        // issue a request for this member's person page data
        _ctx.personsvc.loadBlurbs(_memberId = memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                ArrayList data = (ArrayList)result;
                PersonLayout layout = (PersonLayout)data.remove(0);
                switch (layout.layout) {
                default:
                case PersonLayout.ONE_COLUMN_LAYOUT:
                    setContent(
                        new OneColumnLayout(_ctx, _memberId, layout, data));
                    break;
                case PersonLayout.TWO_COLUMN_LAYOUT:
                    setContent(
                        new TwoColumnLayout(_ctx, _memberId, layout, data));
                    break;
                }
            }

            public void onFailure (Throwable cause) {
                // TODO: display friendly error
                GWT.log("Failed to load blurbs", cause);
            }
        });
    }

    protected int _memberId = -1;
}
