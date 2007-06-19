//
// $Id$

package client.admin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.MemberInviteStatus;

/**
 * Displays the various services available to support and admin personnel.
 */
public class PlayerBrowserPanel extends HorizontalPanel
{
    public PlayerBrowserPanel ()
    {
        setStyleName("playerBrowser");
        setSpacing(10);
        _playerLists = new ArrayList();

        // first, load up the list of players that don't have an inviterId (defaults to 0)
        displayPlayersInvitedBy(CAdmin.msgs.browserNoInviter(), 0);
    }

    public void displayPlayersInvitedBy (final String title, int memberId) 
    {
        CAdmin.adminsvc.getPlayerList(CAdmin.ident, memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _playerLists.add(new PlayerList(title, (List) result, PlayerBrowserPanel.this));
                forward();
            }
            public void onFailure (Throwable cause) {
                add(new Label(CAdmin.serverError(cause)));
            }
        });
    }

    /**
     * Removes all the items after this one on the list, presumably because we're about to 
     * go down a new path.
     */
    public void truncateList (PlayerList lastItem) 
    {
        int ii = _playerLists.lastIndexOf(lastItem);
        if (ii != -1) {
            while (_playerLists.size() > ii + 1) {
                _playerLists.remove(ii + 1);
            }
        }
    }

    /**
     * Shifts the panels such that the second panel is shifted to the first position, and the
     * next panel on the list is shifted to the second position.
     */
    protected void forward ()
    {
        int size = _playerLists.size();
        int ii = size - 1;
        for (; ii > 0; ii--) {
            if (getWidgetIndex((Widget) _playerLists.get(ii)) != -1) {
                break;
            }
        }
        clear();
        add((Widget) _playerLists.get(ii));
        if (ii < size - 1) {
            add((Widget) _playerLists.get(ii+1));
        }
    }

    /**
     * Shifts the panels such that the first panel is shifted to the second position, and the
     * panel before it is in the list shifted to the first position.
     */
    protected void back ()
    {
        int ii = 0;
        for (; ii < _playerLists.size() - 1; ii++) {
            if (getWidgetIndex((Widget) _playerLists.get(ii)) != -1) {
                break;
            }
        }
        clear();
        if (ii > 0) {
            add((Widget) _playerLists.get(ii-1));
        }
        add((Widget) _playerLists.get(ii));
    }

    protected class PlayerList extends FlexTable
    {
        public PlayerList (String title, List players, final PlayerBrowserPanel panel)
        {
            setStyleName("PlayerList");
            int row = 0;
            getFlexCellFormatter().setColSpan(row, 0, NUM_COLUMNS);
            getFlexCellFormatter().addStyleName(row, 0, "Title");
            setText(row++, 0, title);

            getFlexCellFormatter().setColSpan(row, 1, 3);
            getFlexCellFormatter().addStyleName(row, 1, "Last");
            setText(row++, 1, CAdmin.msgs.browserInvites());
            for (int ii = 0; ii < NUM_COLUMNS; ii++) {
                getFlexCellFormatter().addStyleName(row, ii, "Separator");
            }
            setText(row, 0, CAdmin.msgs.browserName());
            setText(row, 1, CAdmin.msgs.browserAvailable());
            setText(row, 2, CAdmin.msgs.browserUsed());
            getFlexCellFormatter().addStyleName(row, 3, "Last");
            setText(row++, 3, CAdmin.msgs.browserTotal());

            Iterator iter = players.iterator();
            while (iter.hasNext()) {
                final MemberInviteStatus member = (MemberInviteStatus) iter.next();
                Label nameLabel = new Label(member.name);
                nameLabel.addClickListener(new ClickListener() {
                    public void onClick (final Widget sender) {
                        if (_activeLabel != null) {
                            _activeLabel.removeStyleName("Highlighted");
                        }
                        (_activeLabel = (Label) sender).addStyleName("Highlighted");
                        panel.truncateList(PlayerList.this);
                        panel.displayPlayersInvitedBy(
                            CAdmin.msgs.browserInvitedBy(member.name), member.memberId);
                    }
                });
                nameLabel.addStyleName("Clickable");
                setWidget(row, 0, nameLabel);
                setText(row, 1, "" + member.invitesGranted);
                setText(row, 2, "" + member.invitesSent);
                getFlexCellFormatter().addStyleName(row, 3, "Last");
                setText(row++, 3, "" + (member.invitesGranted + member.invitesSent));
            }
            for (int ii = 0; ii < NUM_COLUMNS; ii++) {
                getFlexCellFormatter().addStyleName(row-1, ii, "Bottom");
            }
        }

        protected static final int NUM_COLUMNS = 4;

        protected Label _activeLabel;
    }

    // ArrayList<PlayerList>
    protected ArrayList _playerLists;
    protected PlayerList _primaryList;
    protected PlayerList _secondaryList;
}
