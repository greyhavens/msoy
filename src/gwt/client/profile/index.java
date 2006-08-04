//
// $Id$

package client.profile;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;

import com.threerings.msoy.web.data.Profile;
import com.threerings.msoy.web.data.WebCreds;

import client.MsoyEntryPoint;

/**
 * Handles the MetaSOY profile page.
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

    // @Override // from MsoyEntryPoint
    public void onPageLoad ()
    {
        RootPanel.get("content").add(_header = new HeaderPanel());

        History.addHistoryListener(this);
        String initToken = History.getToken();
        if (initToken.length() > 0) {
            onHistoryChanged(initToken);
        } else if (_ctx.creds != null) {
            displayProfile(_ctx.creds.memberId);
        }
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        // parse the token as a memberId and display their profile
        try {
            displayProfile(Integer.parseInt(token));
        } catch (Exception e) {
            // TODO: display "no known profile for that user"
        }
    }

    // @Override // from MsoyEntryPoint
    protected void didLogon (WebCreds creds)
    {
        super.didLogon(creds);

        // if we just logged on and weren't looking at someone elses profile,
        // display ours
        if (_profile == null) {
            displayProfile(creds.memberId);
        }
    }

    protected void didLogoff ()
    {
        super.didLogoff();

        // TODO: disable any pending edit; disallow further editing
    }

    protected void displayProfile (int memberId)
    {
        _ctx.profilesvc.loadProfile(_ctx.creds, memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _profile = (Profile)result;
                _header.setProfile(_profile);
            }
            public void onFailure (Throwable cause) {
                // TODO: display friendly error
                GWT.log("Failed to load profile", cause);
            }
        });
    }

    protected Profile _profile;
    protected HeaderPanel _header;
}
