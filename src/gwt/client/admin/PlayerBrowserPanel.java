//
// $Id$

package client.admin;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.MemberInviteResult;
import com.threerings.msoy.admin.gwt.MemberInviteStatus;

import com.threerings.gwt.ui.WidgetUtil;

import client.shell.Args;
import client.shell.Pages;

import client.ui.MsoyUI;
import client.ui.NumberTextBox;
import client.util.ClickCallback;
import client.util.Link;
import client.util.ServiceUtil;
import client.util.MsoyCallback;

/**
 * Displays the various services available to support and admin personnel.
 */
public class PlayerBrowserPanel extends HorizontalPanel
{
    public PlayerBrowserPanel ()
    {
        setStyleName("playerBrowser");
        setSpacing(10);

        add(_backButton = new Button("<--", new ClickListener() {
            public void onClick (Widget sender) {
                if (_childList == null) {
                    // nothing to do
                    return;
                }
                if (_parentList == null) {
                    // we're keeping the same history token, just need to grab parent info.
                    displayPlayersInvitedBy(_childList.getResult().memberId);
                } else {
                    int memberId = _parentList.getResult().memberId;
                    Link.go(Pages.ADMIN, Args.compose("browser", memberId));
                }
            }
        }));
        _backButton.setEnabled(false);
        add(_forwardButton = new Button("-->", new ClickListener() {
            public void onClick (Widget sender) {
                if (_childList == null) {
                    // nothing to do
                    return;
                }
                int index = _playerLists.indexOf(_childList);
                if (index == _playerLists.size() - 1) {
                    // nothing to do
                    return;
                }
                int memberId = _playerLists.get(index +1).getResult().memberId;
                Link.go(Pages.ADMIN, Args.compose("browser", memberId));
            }
        }));
        _forwardButton.setEnabled(false);
    }

    public void displayPlayersInvitedBy (final int memberId)
    {
        int childList = -1;
        int parentList = -1;
        for (int ii = 0; _playerLists != null && ii < _playerLists.size(); ii++) {
            PlayerList list = _playerLists.get(ii);
            if (list.highlight(memberId)) {
                parentList = ii;
            } else if (list.getResult().memberId == memberId) {
                childList = ii;
                break;
            }
        }

        int memberIdToFetch = memberId;
        if (childList > 0 ||
                // special case only invoked if we have a populated list, and the person goes back
                // to the root admin console and clicks "player browser" again.
                (childList == 0 && _playerLists.get(childList).getResult().memberId == 0)) {
            // we already have everything the caller wants, just display it
            displayLists(childList);
            return;
        } else if (childList == 0) {
            // we need to fetch the parent list
            PlayerList list = _parentList != null ? _parentList : _childList;
            memberIdToFetch = list.getResult().invitingFriendId;
        } else if (parentList > -1) {
            // we have the parent, but the child is not there, so we're on a new branch... truncate
            // the list at the parent
            while (_playerLists.size() > parentList + 1) {
                _playerLists.remove(parentList + 1);
            }
        } else {
            clearLists();
            _playerLists = new ArrayList<PlayerList>();
        }

        _adminsvc.getPlayerList(memberIdToFetch, new MsoyCallback<MemberInviteResult>() {
            public void onSuccess (MemberInviteResult res) {
                PlayerList newList = new PlayerList(res);
                if (res.memberId != memberId) {
                    // we're fetching a new parent.
                    _playerLists.add(0, newList);
                    PlayerList list = _parentList != null ? _parentList : _childList;
                    newList.highlight(list.getResult().memberId);
                    displayLists(1);
                } else {
                    _playerLists.add(newList);
                    displayLists(_playerLists.size() - 1);
                }
            }
        });
    }

    protected void clearLists ()
    {
        if (_parentList != null) {
            remove(_parentList);
            _parentList = null;
        }
        if (_childList != null) {
            remove(_childList);
            _childList = null;
        }
    }

    /**
     * Displays two lists, with the firstList being shown first, if not null.  Otherwise, it
     * simply displays the last two PlayerLists we have.
     */
    protected void displayLists (int childIndex)
    {
        clearLists();
        insert(_childList = _playerLists.get(childIndex), 1);
        _forwardButton.setEnabled(childIndex != _playerLists.size() - 1);
        if (childIndex != 0) {
            insert(_parentList = _playerLists.get(childIndex - 1), 1);
            _backButton.setEnabled(_parentList.getResult().memberId != 0);
        } else {
            _backButton.setEnabled(_childList.getResult().memberId != 0);
        }
    }

    protected void addToAvailable (int memberId, int amount)
    {
        for (PlayerList list : _playerLists) {
            if (list.addToAvailable(memberId, amount)) {
                break;
            }
        }
    }

    protected class PlayerList extends FlexTable
    {
        public PlayerList (MemberInviteResult result)
        {
            _result = result;
            String title = _result.name != null && !_result.name.equals("") ?
                CAdmin.msgs.browserInvitedBy(_result.name) : CAdmin.msgs.browserNoInviter();
            setStyleName("PlayerList");
            int row = 0;
            getFlexCellFormatter().setColSpan(row, 0, NUM_COLUMNS);
            getFlexCellFormatter().addStyleName(row, 0, "Title");
            setText(row++, 0, title);

            if (_result.name != null && !_result.name.equals("")) {
                getFlexCellFormatter().setColSpan(row, 0, NUM_COLUMNS);
                getFlexCellFormatter().addStyleName(row, 0, "Title");
                HorizontalPanel buttons = new HorizontalPanel();
                // wtf?  Even if you don't set this property, the default gets applied directly to
                // the element, so I can't override it in the css file.  Thanks a ton, GWT team.
                buttons.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
                buttons.addStyleName("Buttons");
                buttons.add(new Button("View Profile", new ClickListener() {
                    public void onClick (Widget sender) {
                        Link.go(Pages.PEOPLE, "" + _result.memberId);
                    }
                }));
                Widget shim = WidgetUtil.makeShim(1, 25);
                shim.addStyleName("Shim");
                buttons.add(shim);
                final NumberTextBox numInvites = new NumberTextBox(false, 2);
                buttons.add(numInvites);
                Button grantInvites = new Button(CAdmin.msgs.browserGrantInv());
                new ClickCallback<Void>(grantInvites) {
                    public boolean callService () {
                        _adminsvc.grantInvitations(
                            numInvites.getValue().intValue(), _result.memberId, this);
                        return true;
                    }
                    public boolean gotResult (Void result) {
                        MsoyUI.info(CAdmin.msgs.browserAddInvites("" + numInvites.getValue(),
                                    _result.name));
                        PlayerBrowserPanel.this.addToAvailable(
                            _result.memberId, numInvites.getValue().intValue());
                        return true;
                    }
                };
                buttons.add(grantInvites);
                setWidget(row++, 0, buttons);
            }

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

            if (_result.invitees == null) {
                return;
            }
            _rows = new Element[_result.invitees.size()];
            int ii = 0;
            for (MemberInviteStatus member : _result.invitees) {
                getRowFormatter().addStyleName(row, "DataRow");
                Label nameLabel = new Label(member.name);
                nameLabel.addClickListener(
                    Link.createListener(Pages.ADMIN, Args.compose("browser", member.memberId)));
                nameLabel.addStyleName("Clickable");
                _memberIds.put(member.memberId, nameLabel);
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

        public boolean addToAvailable (int memberId, int amount)
        {
            Label label = _memberIds.get(memberId);
            if (label == null) {
                return false;
            }

            Element row = DOM.getParent(DOM.getParent(label.getElement()));
            Element cell = DOM.getChild(row, AVAILABLE_INVITES_COLUMN);
            String text = DOM.getInnerText(cell);
            try {
                text = "" + (Integer.parseInt(text) + amount);
            } catch (NumberFormatException nfe) {
                CAdmin.log("NFE attempting to add to available invites: " + nfe.getMessage());
            }
            DOM.setInnerText(cell, text);
            return true;
        }

        public boolean highlight (int memberId)
        {
            Label label = _memberIds.get(memberId);
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

        public MemberInviteResult getResult ()
        {
            return _result;
        }

        public void sort (int column, int type, int order)
        {
            int rowCount = getRowCount();
            getRowFormatter().removeStyleName(rowCount-1, "Bottom");
            Element table = getBodyElement();
            for (int ii = 0; ii < _rows.length; ii++) {
                DOM.removeChild(table, _rows[ii]);
            }
            Arrays.sort(_rows, new RowComparator(column, type, order));
            for (int ii = 0; ii < _rows.length; ii++) {
                DOM.appendChild(table, _rows[ii]);
            }
            getRowFormatter().addStyleName(rowCount-1, "Bottom");
        }

        protected class RowComparator implements Comparator<Element>
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

            public int compare (Element o1, Element o2)
            {
                String s1 = getCellContents(o1);
                String s2 = getCellContents(o2);

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

        protected Element[] _rows;
        protected MemberInviteResult _result;
        protected Label _activeLabel, _activeHeader;
        protected Map<Integer, Label> _memberIds = new HashMap<Integer, Label>();
    }

    protected ArrayList<PlayerList> _playerLists;
    protected PlayerList _parentList, _childList;
    protected Button _backButton, _forwardButton;

    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
