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
    }

    public void displayPlayersInvitedBy (final int memberId) 
    {
        PlayerList playerList = null;
        for (int ii = 0; _playerLists != null && ii < _playerLists.size(); ii++) {
            playerList = (PlayerList) _playerLists.get(ii);
            if (playerList.highlight(memberId)) {
                break;
            } else {
                playerList = null;
            }
        }
        if (playerList == null) {
            // the given memberId was not found in any cached list - clear the display and 
            // start fresh
            clear();
            _playerLists = new ArrayList();
        } else {
            int ii = _playerLists.indexOf(playerList);
            if (ii < _playerLists.size() - 1 && 
                    ((PlayerList) _playerLists.get(ii + 1)).getInviterId() == memberId) {
                // we have what the caller wants cached... just display it
                clear();
                add(playerList);
                PlayerList next = (PlayerList) _playerLists.get(ii + 1);
                next.clearHighlight();
                add(next);
                return;
            } else {
                truncateList(playerList);
            }
        }

        CAdmin.adminsvc.getPlayerList(CAdmin.ident, memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                MemberInviteResult res = (MemberInviteResult) result;
                String title = res.name != null && !res.name.equals("") ? 
                    CAdmin.msgs.browserInvitedBy(res.name) : CAdmin.msgs.browserNoInviter();
                _playerLists.add(new PlayerList(title, memberId, 
                    res.invitees != null ? res.invitees : new ArrayList()));
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
                setWidget(row, NAME_COLUMN, nameLabel);
                setText(row, AVAILABLE_INVITES_COLUMN, "" + member.invitesGranted);
                setText(row, USED_INVITES_COLUMN, "" + member.invitesSent);
                _memberIds.put(new Integer(member.memberId), new Integer(row));
                setText(row++, TOTAL_INVITES_COLUMN, 
                    "" + (member.invitesGranted + member.invitesSent));
                getFlexCellFormatter().addStyleName(row-1, NUM_COLUMNS-1, "Last");
                _rows[ii++] = getRowFormatter().getElement(row-1);
            }
            getRowFormatter().addStyleName(row-1, "Bottom");
        }

        public boolean highlight (int memberId) 
        {
            Integer row = (Integer) _memberIds.get(new Integer(memberId));
            if (row == null) {
                return false;
            }

            clearHighlight();
            (_activeLabel = (Label) getWidget(row.intValue(), 0)).addStyleName("Highlighted");
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
                return other._column == _column && other._sortType == _sortType;
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
        // This array holds only Elements, but if it is declared as Element[], the the 
        // comparator's compare() method fails when checking if the objects it receives are
        // instanceof Element.  If the array is declared as Object[], and every time an 
        // element is accessed it is casted to Element, everything works fine.
        protected Object[] _rows;
        protected int _inviterId;
        protected Label _activeLabel, _activeHeader;
        protected Map _memberIds = new HashMap(); // Map<Integer, Integer>
    }

    // ArrayList<PlayerList>
    protected ArrayList _playerLists;
    protected PlayerList _primaryList;
    protected PlayerList _secondaryList;
}
