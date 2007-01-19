//
// $Id$

package client.profile;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.threerings.msoy.web.data.PersonLayout;
import com.threerings.msoy.web.data.WebCreds;

import client.msgs.MsgsEntryPoint;
import client.shell.MsoyEntryPoint;
import client.shell.ShellContext;

/**
 * Displays a person's "portal" page with their profile information, friends,
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
    protected ShellContext createContext ()
    {
        return _ctx = new ProfileContext();
    }

    // @Override // from MsoyEntryPoint
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        _ctx.msgs = (ProfileMessages)GWT.create(ProfileMessages.class);
    }

    // @Override // from MsoyEntryPoint
    protected void onPageLoad ()
    {
        History.addHistoryListener(this);

        String initToken = History.getToken();
        if (initToken.length() > 0) {
            onHistoryChanged(initToken);

        } else if (_ctx.creds == null) {
            // TODO: display member search interface
            setContent(new Label(_ctx.msgs.indexLogon()));

        } else {
            // if we're logged on and not displaying someone elses member page, display our own
            displayMemberPage(_ctx.creds.getMemberId());
        }
    }

    // @Override // from MsoyEntryPoint
    protected void didLogon (WebCreds creds)
    {
        super.didLogon(creds);

        if (_memberId == -1) {
            displayMemberPage(creds.getMemberId());
        }
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
                    setContent(new OneColumnLayout(_ctx, _memberId, layout, data));
                    break;
                case PersonLayout.TWO_COLUMN_LAYOUT:
                    setContent(new TwoColumnLayout(_ctx, _memberId, layout, data));
                    break;
                }
            }

            public void onFailure (Throwable cause) {
                setContent(new Label(_ctx.serverError(cause)));
                _ctx.log("Failed to load blurbs", cause);
            }
        });
    }

    protected ProfileContext _ctx;
    protected int _memberId = -1;
}
