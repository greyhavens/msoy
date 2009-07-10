//
// $Id$

package client.facebook;

import java.util.ArrayList;
import java.util.List;

import client.ui.MsoyUI;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.facebook.gwt.FacebookFriendInfo;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookServiceAsync;

/**
 * Displays a row of {@link FacebookFriendInfo} to be shown across the bottom of the Whirled app
 * on Facebook.
 */
public class FBFriendBar extends FlowPanel
{
    public FBFriendBar ()
    {
        setStyleName("friendBar");
        // allow children to be positioned relatively
        DOM.setStyleAttribute(getElement(), "position", "fixed");

        add(MsoyUI.createLabel(_msgs.friendBarTitle(), "title"));
        add(_content);

        _content.setWidget(0, 0, MsoyUI.createActionImage("/images/facebook/scroller_left.png",
            new ClickHandler() {
                @Override public void onClick (ClickEvent event) {
                    scroll(-1); // left
                }
            }), 1, "NavCell");

        for (int i = 1; i <= FRIEND_COUNT; ++i) {
            _content.setText(0, i, "...", 1, "FriendCell");
        }

        _content.setWidget(0, FRIEND_COUNT + 1, MsoyUI.createActionImage(
            "/images/facebook/scroller_right.png", new ClickHandler() {
                @Override public void onClick (ClickEvent event) {
                    scroll(1); // left
                }
            }), 1, "NavCell");

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
            _friendPanels.add(new FBFriendPanel(friends.get(ii), ii));
        }
        while (_friendPanels.size() < FRIEND_COUNT) {
            _friendPanels.add(new FBFriendPanel(null, 0));
        }
        _offset = 0;
        update();
    }

    protected void update ()
    {
        // countdown, lowest ranks on the left
        for (int ii =_offset, col = FRIEND_COUNT; col >= 1; ++ii, --col) {
            if (ii < _friendPanels.size()) {
                _content.setWidget(0, col, _friendPanels.get(ii));
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
    SmartTable _content = new SmartTable("FriendBarTable", 0, 1);

    protected static final int FRIEND_COUNT = 6;

    protected static final FacebookMessages _msgs = GWT.create(FacebookMessages.class);
    protected static final FacebookServiceAsync _fbsvc = (FacebookServiceAsync)
        ServiceUtil.bind(GWT.create(FacebookService.class), FacebookService.ENTRY_POINT);
}
