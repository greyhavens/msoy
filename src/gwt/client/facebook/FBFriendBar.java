//
// $Id$

package client.facebook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import client.ui.MsoyUI;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.PushButton;

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
        add(_left = MsoyUI.createImageButton("fbscrollLeft", new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                scroll(1); // left
            }
        }));
        add(_right = MsoyUI.createImageButton("fbscrollRight", new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                scroll(-1); // right
            }
        }));
        _left.setVisible(false);
        _right.setVisible(false);
        _fbsvc.getAppFriendsInfo(new AsyncCallback<List<FacebookFriendInfo>>() {
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
            FBFriendPanel panel = new FBFriendPanel(friends.get(ii), ii + 1);
            String id = HTMLPanel.createUniqueId();
            panel.getElement().setId(id);
            _parsed.put(id, 0L); // never parsed
            _friendPanels.add(panel);
        }

        // pad out for testing
        boolean testScrolling = false;
        if (testScrolling) {
            while (_friendPanels.size() < FRIEND_COUNT * 2) {
                _friendPanels.add(new FBFriendPanel(friends.get(0), _friendPanels.size() + 1));
            }
        }

        // add empty slots (w/ invite button)
        while (_friendPanels.size() < FRIEND_COUNT) {
            _friendPanels.add(new FBFriendPanel(null, _friendPanels.size() + 1));
        }

        for (FBFriendPanel panel : _friendPanels) {
            panel.setVisible(false);
            add(panel);
        }
        _offset = 0;
        update();
    }

    protected void update ()
    {
        for (FBFriendPanel panel : _friendPanels) {
            panel.setVisible(false);
        }
        // countdown, lowest ranks on the left
        for (int ii =_offset, col = FRIEND_COUNT; col >= 1; ++ii, --col) {
            FBFriendPanel panel = get(ii);
            panel.setVisible(true);
            panel.getElement().setAttribute("column", String.valueOf(col));

            // workaround for IE: attribute selectors do not appear to update dynamically
            // ... so just remove and readd a style attribute here
            DOM.setStyleAttribute(panel.getElement(), "left", "0px");
            DOM.setStyleAttribute(panel.getElement(), "left", "");
        }

        _left.setVisible(_friendPanels.size() > FRIEND_COUNT);
        _right.setVisible(_friendPanels.size() > FRIEND_COUNT);

        // reparse 3 sections of panels, 1 now and 2 later
        reparseRange(_offset, _offset + FRIEND_COUNT - 1);
        if (_timer != null) {
            _timer.cancel();
        }
        _timer = new Timer() {
            @Override public void run () {
                reparseRange(_offset + FRIEND_COUNT, _offset + FRIEND_COUNT * 2 - 1);
                reparseRange(_offset - FRIEND_COUNT, _offset - 1);
            }
        };
        _timer.schedule(2000);
    }

    public void scroll (int delta)
    {
        _offset += delta;
        update();
    }

    protected FBFriendPanel get (int offset)
    {
        int size = _friendPanels.size();
        while (offset < 0) {
            offset += size;
        }
        return _friendPanels.get(offset % size);
    }

    protected void reparseRange (int start, int end)
    {
        long now = System.currentTimeMillis();
        for (int ii = start; ii <= end; ++ii) {
            FBFriendPanel panel = get(ii);
            String id = panel.getElement().getId();
            Long time = _parsed.get(id);
            if (time == null || time != 0) {
                continue;
            }
            FBMLPanel.reparse(panel);
            _parsed.put(id, now);
        }
    }

    // we are experiencing this bug: http://bugs.developers.facebook.com/show_bug.cgi?id=4852
    // the last comment was from May so workaround by hacking a bit
    protected void checkForFailedParsings ()
    {
        // TODO
    }

    protected int _offset;
    // store the entire panel since the FBMPLPanel.reparse is very expensive
    protected List<FBFriendPanel> _friendPanels;
    protected PushButton _left, _right;
    protected HashMap<String, Long> _parsed = new HashMap<String, Long>();
    protected Timer _timer;

    protected static final int FRIEND_COUNT = 6;

    protected static final FacebookMessages _msgs = GWT.create(FacebookMessages.class);
    protected static final FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
}
