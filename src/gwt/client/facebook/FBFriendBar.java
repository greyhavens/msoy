//
// $Id$

package client.facebook;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.facebook.gwt.FacebookFriendInfo;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookServiceAsync;

/**
 * Displays a row of {@link FacebookFriendInfo} to be shown across the bottom of the Whirled app
 * on Facebook.
 */
public class FBFriendBar extends FloatPanel
{
    public FBFriendBar ()
    {
        super("FriendBar");

        // TODO: scroll buttons
        _fbsvc.getFriends(new AsyncCallback<List<FacebookFriendInfo>>() {
            @Override public void onSuccess (List<FacebookFriendInfo> result) {
                init(result);
            }
            @Override public void onFailure (Throwable caught) {
                // TODO
            }
        });
    }

    public void init (List<FacebookFriendInfo> friends)
    {
        // countdown, lowest ranks on the left
        for (int ii = 5; ii >= 0; --ii) {
            if (ii >= friends.size()) {
                // TODO: empty box - call to action: invite somebody
                continue;
            }
            add(new FBFriendPanel(friends.get(ii), ii));
        }
        FBMLPanel.reparse(this);
    }

    protected static final FacebookServiceAsync _fbsvc = (FacebookServiceAsync)
        ServiceUtil.bind(GWT.create(FacebookService.class), FacebookService.ENTRY_POINT);
}
