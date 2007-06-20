//
// $Id$

package client.admin;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.MemberInviteStatus;
import com.threerings.msoy.web.data.MemberInviteResult;

import client.shell.Application;

/**
 * Displays the various services available to support and admin personnel.
 */
public class PlayerBrowserPanel extends HorizontalPanel
{
    public PlayerBrowserPanel ()
    {
        setStyleName("playerBrowser");
        setSpacing(10);
        _upButton = new Image("/images/item/inventory_up.png");
        _upButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                Widget playerList = getWidget(1);
                if (playerList != null && playerList instanceof PlayerList) {
                    int inviterId = ((PlayerList) playerList).getInviterId();
                    if (_currentMember != null && inviterId == _currentMember.memberId) {
                        // we're not really going to a new page, just fetching new info
                        displayPlayersInvitedBy(inviterId);
                    } else {
                        History.newItem(Application.createLinkToken("admin", 
                            "browser_" + inviterId));
                    }
                }
            }
        });
        DOM.setStyleAttribute(_upButton.getElement(), "cursor", "pointer");
    }

    public void displayPlayersInvitedBy (final int memberId) 
    {
        PlayerList childList = null;
        PlayerList parentList = null;
        for (int ii = 0; _playerLists != null && ii < _playerLists.size(); ii++) {
            PlayerList list = (PlayerList) _playerLists.get(ii);
            if (parentList == null) {
                if (list.highlight(memberId)) {
                    parentList = list;
                }
            }
            if (childList == null) {
                if (list.getInviterId() == memberId) {
                    childList = list;
                }
            }
            if (childList != null && parentList != null) {
                break;
            }
        }

        int memberIdToFetch = memberId;
        if (childList != null && parentList != null) {
            // we have what the caller wants cached... just display it
            displayLists(parentList);
            return;
        } else if (childList == null && parentList == null) {
            // the given memberId was not found in any cached list - clear the display and 
            // start fresh
            clear();
            _playerLists = new ArrayList();
        } else if (childList == null && parentList != null) {
            // we're grabbing a new child list... truncate everything after the parent
            truncateList(parentList);
        } else if (childList != null && parentList == null) {
            if (_playerLists.indexOf(childList) != 0) {
                // wtf?  If we found a child with no parent, the child should be at the top.
                // Freak out and start over.
                CAdmin.log("_playerLists was not sane.  Found child with no parent at " + 
                    _playerLists.indexOf(childList));
                clear();
                _playerLists = new ArrayList();
            } else if (_currentMember != null && _currentMember.invitingFriendId >= 0) {
                // we're grabbing a new parent list
                memberIdToFetch = _currentMember.invitingFriendId;
            }
        }
        CAdmin.log("fetching for memberId: " + memberIdToFetch);
        CAdmin.adminsvc.getPlayerList(CAdmin.ident, memberIdToFetch, new AsyncCallback() {
            public void onSuccess (Object result) {
                MemberInviteResult res = (MemberInviteResult) result;
                String title = res.name != null && !res.name.equals("") ? 
                    CAdmin.msgs.browserInvitedBy(res.name) : CAdmin.msgs.browserNoInviter();
                PlayerList newList = new PlayerList(title, res.memberId,
                    res.invitees != null ? res.invitees : new ArrayList());
                if (res.memberId != memberId) {
                    // we're fetching a new parent.
                    _playerLists.add(0, newList);
                    displayLists(newList);
                    newList.highlight(_currentMember.memberId);
                } else {
                    _currentMember = res;
                    _playerLists.add(newList);
                    displayLists(null);
                }
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
    protected void truncateList (PlayerList lastItem) 
    {
        int ii = _playerLists.lastIndexOf(lastItem);
        if (ii != -1) {
            while (_playerLists.size() > ii + 1) {
                _playerLists.remove(ii + 1);
            }
        }
    }

    /**
     * Displays two lists, with the firstList being shown first, if not null.  Otherwise, it
     * simply displays the last two PlayerLists we have.
     */
    protected void displayLists (PlayerList firstList)
    {
        clear();
        int size = _playerLists.size();
        int ii = Math.max(size - 2, 0);
        if (firstList != null) {
            ii = _playerLists.indexOf(firstList);
        } else {
            firstList = (PlayerList) _playerLists.get(ii);
        }
        if (firstList.getInviterId() != 0) {
            add(_upButton);
        }
        add((Widget) _playerLists.get(ii));
        if (ii < size - 1) {
            PlayerList childList = (PlayerList) _playerLists.get(ii + 1);
            childList.clearHighlight();
            add(childList);
        }
    }

    protected class PlayerList extends FlexTable
    {
        public PlayerList (String title, int inviterId, List players)
        {
            _inviterId = inviterId;

            setStyleName("PlayerList");
            int row = 0;
            getFlexCellFormatter().setColSpan(row, 0, NUM_COLUMNS);
            getFlexCellFormatter().addStyleName(row, 0, "Title");
            setText(row++, 0, title);

            getFlexCellFormatter().setColSpan(row, 1, 3);
            getFlexCellFormatter().addStyleName(row, 1, "Last");
            getFlexCellFormatter().addStyleName(row, 1, "Header");
            setText(row++, 1, CAdmin.msgs.browserInvites());

            getRowFormatter().addStyleName(row, "Clickable");
            getRowFormatter().addStyleName(row, "Separator");
            // organized in the same order as the NNN_COLUMN constants
            String[] labelText = new String[] { CAdmin.msgs.browserName(), 
                CAdmin.msgs.browserAvailable(), CAdmin.msgs.browserUsed(), 
                CAdmin.msgs.browserTotal() };
            int[] sortType = new int[] { RowComparator.SORT_TYPE_STRING, 
                RowComparator.SORT_TYPE_INT, RowComparator.SORT_TYPE_INT, 
                RowComparator.SORT_TYPE_INT };
            int[] sortOrder = new int[] { RowComparator.SORT_ORDER_ASCENDING,
                RowComparator.SORT_ORDER_DESCENDING, RowComparator.SORT_ORDER_DESCENDING,
                RowComparator.SORT_ORDER_DESCENDING };
            for (int ii = 0; ii < NUM_COLUMNS; ii++) {
                Label headerLabel = new Label(labelText[ii]);
                headerLabel.addStyleName("Header");
                final int column = ii;
                final int type = sortType[ii];
                final int order = sortOrder[ii];
                headerLabel.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        sort(column, type, _sortOrder);
                        _sortOrder *= -1;
                        if (_activeHeader != null) {
                            _activeHeader.removeStyleName("HighlightedHeader");
                        }
                        (_activeHeader = (Label) sender).addStyleName("HighlightedHeader");
                    }
                    protected int _sortOrder = order;
                });
                setWidget(row, ii, headerLabel);
            }
            getFlexCellFormatter().addStyleName(row++, NUM_COLUMNS-1, "Last");

            _rows = new Object[players.size()];
            int ii = 0;
            Iterator iter = players.iterator();
            while (iter.hasNext()) {
                final MemberInviteStatus member = (MemberInviteStatus) iter.next();
                getRowFormatter().addStyleName(row, "DataRow");
                Label nameLabel = new Label(member.name);
                nameLabel.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        History.newItem(Application.createLinkToken("admin", 
                            "browser_" + member.memberId));
                    }
                });
                nameLabel.addStyleName("Clickable");
                _memberIds.put(new Integer(member.memberId), nameLabel);
                setWidget(row, NAME_COLUMN, nameLabel);
                setText(row, AVAILABLE_INVITES_COLUMN, "" + member.invitesGranted);
                setText(row, USED_INVITES_COLUMN, "" + member.invitesSent);
                setText(row++, TOTAL_INVITES_COLUMN, 
                    "" + (member.invitesGranted + member.invitesSent));
                getFlexCellFormatter().addStyleName(row-1, NUM_COLUMNS-1, "Last");
                _rows[ii++] = getRowFormatter().getElement(row-1);
            }
            getRowFormatter().addStyleName(row-1, "Bottom");
        }

        public boolean highlight (int memberId) 
        {
            Label label = (Label) _memberIds.get(new Integer(memberId));
            if (label == null) {
                return false;
            }

            clearHighlight();
            (_activeLabel = label).addStyleName("Highlighted");
            return true;
        }

        public void clearHighlight ()
        {
            if (_activeLabel != null) {
                _activeLabel.removeStyleName("Highlighted");
                _activeLabel = null;
            }
        }

        public int getInviterId () 
        {
            return _inviterId;
        }

        public void sort (int column, int type, int order)
        {
            int rowCount = getRowCount();
            getRowFormatter().removeStyleName(rowCount-1, "Bottom");
            Element table = getBodyElement();
            for (int ii = 0; ii < _rows.length; ii++) {
                DOM.removeChild(table, (Element) _rows[ii]);
            }
            Arrays.sort(_rows, new RowComparator(column, type, order));
            for (int ii = 0; ii < _rows.length; ii++) {
                DOM.appendChild(table, (Element) _rows[ii]);
            }
            getRowFormatter().addStyleName(rowCount-1, "Bottom");
        }

        protected class RowComparator implements Comparator
        {
            public static final int SORT_TYPE_STRING = 1;
            public static final int SORT_TYPE_INT = 2;

            public static final int SORT_ORDER_DESCENDING = -1;
            public static final int SORT_ORDER_ASCENDING = 1;

            public RowComparator (int column, int sortType, int sortOrder) 
            {
                _column = column;
                _sortType = sortType;
                _sortOrder = sortOrder;
            }

            public boolean equals (Object obj) {
                if (!(obj instanceof RowComparator)) {
                    return false;
                }
                RowComparator other = (RowComparator) obj;
                return other._column == _column && other._sortType == _sortType &&
                    other._sortOrder == _sortOrder;
            }

            public int compare (Object o1, Object o2) 
            {
                if (!(o1 instanceof Element) || !(o2 instanceof Element)) {
                    CAdmin.log("Received non-Element when sorting player list! " +
                        "|" + o1 + "|" + o2 + "|");
                    return 0; 
                }
                String s1 = getCellContents((Element) o1);
                String s2 = getCellContents((Element) o2);

                int result = 0;
                if (_sortType == SORT_TYPE_INT) {
                    try {
                        result = new Integer(s1).compareTo(new Integer(s2));
                    } catch (NumberFormatException nfe) {
                        CAdmin.log("NFE when sorting player list: " + nfe.getMessage());
                    }
                } else {
                    result = s1.compareTo(s2);
                }
                return result * _sortOrder;
            }

            protected String getCellContents (Element row) 
            {
                if (DOM.getChildCount(row) < _column) {
                    CAdmin.log("Element row does not contain " + _column + " children.");
                    return "";
                }
                return DOM.getInnerText(DOM.getChild(row, _column));
            }

            protected int _column;
            protected int _sortType;
            protected int _sortOrder;
        }

        protected static final int NUM_COLUMNS = 4;
        protected static final int NAME_COLUMN = 0;
        protected static final int AVAILABLE_INVITES_COLUMN = 1;
        protected static final int USED_INVITES_COLUMN = 2;
        protected static final int TOTAL_INVITES_COLUMN = 3;

        // Something super weird is going on here (possibly a bug with the GWT compiler).  
        // This array holds only Elements, but if it is declared as Element[], then the 
        // comparator's compare() method fails when checking if the objects it receives are
        // instanceof Element.  If the array is declared as Object[], and every time an 
        // element is accessed it is casted to Element, everything works fine.
        protected Object[] _rows;
        protected int _inviterId;
        protected Label _activeLabel, _activeHeader;
        protected Map _memberIds = new HashMap(); // Map<Integer, Label>
    }

    // ArrayList<PlayerList>
    protected ArrayList _playerLists;
    protected PlayerList _primaryList;
    protected PlayerList _secondaryList;
    protected Image _upButton;
    protected MemberInviteResult _currentMember;
}
