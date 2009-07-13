//
// $Id$

package client.facebook;

import java.util.ArrayList;
import java.util.List;

import client.ui.MsoyUI;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.ui.AbsoluteCSSPanel;

import com.threerings.msoy.facebook.gwt.FacebookFriendInfo;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookServiceAsync;

/**
 * Displays a row of {@link FacebookFriendInfo} to be shown across the bottom of the Whirled app
 * on Facebook.
 */
public class FBFriendBar extends AbsoluteCSSPanel
{
    public FBFriendBar ()
    {
        super("friendBar", "fixed");

        add(MsoyUI.createLabel(_msgs.friendBarTitle(), "title"));
        add(MsoyUI.createImageButton("fbscrollLeft", new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                scroll(1); // left
            }
        }));
        add(MsoyUI.createImageButton("fbscrollRight", new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                scroll(-1); // right
            }
        }));

        _fbsvc.getFriends(new AsyncCallback<List<FacebookFriendInfo>>() {
            @Override public void onSuccess (List<FacebookFriendInfo> result) {
                init(result);
            }
            @Override public void onFailure (Throwable caught) {
                // TODO
            }
        });
    }

    protected void init (List<FacebookFriendInfo> friends)
    {
        _friendPanels = new ArrayList<FBFriendPanel>(friends.size());
        for (int ii = 0, ll = friends.size(); ii < ll; ++ii) {
            _friendPanels.add(new FBFriendPanel(friends.get(ii), ii + 1));
        }
        while (_friendPanels.size() < FRIEND_COUNT) {
            _friendPanels.add(new FBFriendPanel(null, _friendPanels.size() + 1));
        }
        _offset = 0;
        update();
    }

    protected void update ()
    {
        for (FBFriendPanel panel : _friendPanels) {
            if (panel.getParent() == this) {
                remove(panel);
            }
        }
        // countdown, lowest ranks on the left
        for (int ii =_offset, col = FRIEND_COUNT; col >= 1; ++ii, --col) {
            if (ii < _friendPanels.size()) {
                FBFriendPanel panel = _friendPanels.get(ii);
                add(panel);
                panel.getElement().setAttribute("column", String.valueOf(col));
            }
        }

        // TODO: if slow, maybe play with adding extra panels to DOM as hidden to get FB to do
        // larger batches
        FBMLPanel.reparse(this);
    }

    public void scroll (int delta)
    {
        int offset = _offset + delta;
        if (offset < 0 || offset + FRIEND_COUNT >= _friendPanels.size()) {
            return;
        }
        _offset = offset;
        update();
    }

    int _offset;
    // store the entire panel since the FBMPLPanel.reparse is very expensive
    List<FBFriendPanel> _friendPanels;

    protected static final int FRIEND_COUNT = 6;

    protected static final FacebookMessages _msgs = GWT.create(FacebookMessages.class);
    protected static final FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
}
