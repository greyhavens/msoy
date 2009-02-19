//
// $Id$

package client.people;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.web.gwt.EmailContact;

import client.ui.MsoyUI;

/**
 * A table list of names and email addresses, with a button to remove them. Intended to be used
 * for sending invitations to Whirled.
 */
public class InviteList extends FlexTable
{
    /**
     * Creates a new list.
     */
    public InviteList ()
    {
        setStyleName("InviteList");
        setCellSpacing(0);
        setText(0, 0, InvitePanel._msgs.inviteListName());
        getFlexCellFormatter().setWidth(0, 0, "190px");
        getFlexCellFormatter().setStyleName(0, 0, "Header");
        setText(0, 1, InvitePanel._msgs.inviteListRemove());
        getFlexCellFormatter().setWidth(0, 1, "40px");
        getFlexCellFormatter().setStyleName(0, 1, "Header");
        setText(0, 2, InvitePanel._msgs.inviteListEmail());
        getFlexCellFormatter().setWidth(0, 2, "364px");
        getFlexCellFormatter().setStyleName(0, 2, "Header");

        _listTable = new SmartTable("InviteListTable", 0, 0);

        ScrollPanel scroll = new ScrollPanel(_listTable);
        scroll.setStyleName("Scroll");
        setWidget(1, 0, scroll);
        getFlexCellFormatter().setColSpan(1, 0, 3);
    }

    /**
     * Gets the list of email items in the list.
     */
    public List<EmailContact> getItems ()
    {
        return _items;
    }

    /**
     * Removes all emails from the list.
     */
    public void clear ()
    {
        _items.clear();
        for (int ii = _listTable.getRowCount() - 1; ii >= 0; ii--) {
            _listTable.removeRow(ii);
        }
    }

    /**
     * Adds a new item to the list.
     */
    public boolean addItem (String name, String email)
    {
        EmailContact ec = new EmailContact();
        ec.name = name.trim();
        ec.email = email.trim();
        if (_items.contains(ec)) {
            return false;
        }
        _items.add(ec);
        final int row = _listTable.getRowCount();
        _listTable.setText(row, 0, name);
        _listTable.getFlexCellFormatter().setWidth(row, 0, "190px");
        setRemove(row);
        _listTable.setText(row, 2, email);
        return true;
    }

    protected void removeItem (int row)
    {
        _items.remove(row);
        _listTable.removeRow(row);
        for (int ii = row; ii < _listTable.getRowCount(); ii++) {
            setRemove(ii);
        }
    }

    protected void setRemove (final int row)
    {
        _listTable.setWidget(row, 1, MsoyUI.createActionImage(
                    "/images/profile/remove.png", new ClickListener() {
            public void onClick (Widget widget) {
                removeItem(row);
            }
        }));
        _listTable.getFlexCellFormatter().setWidth(row, 1, "40px");
    }

    protected List<EmailContact> _items = new ArrayList<EmailContact>();
    protected FlexTable _listTable;
}
