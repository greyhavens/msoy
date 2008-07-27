//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import client.ui.MsoyUI;

/**
 * Displays a list of messages or threads things with a title and possibly a back button.
 */
public class TitledListPanel extends SmartTable
{
    public TitledListPanel ()
    {
        super("titledListPanel", 0, 0);
        getFlexCellFormatter().setStyleName(0, 0, "Header");

        _bheader = new SmartTable(0, 0);
        _bheader.setWidget(0, 0, MsoyUI.createBackArrow(), 1, "Back");
        _bheader.getFlexCellFormatter().setStyleName(0, 1, "Title");
        _bheader.getFlexCellFormatter().setWidth(0, 1, "100%");
    }

    public void setContents (String title, Widget contents)
    {
        setContents(title, contents, false);
    }

    public void setContents (String title, Widget contents, boolean backButton)
    {
        if (backButton) {
            setWidget(0, 0, _bheader);
        }
        updateTitle(title);
        setWidget(1, 0, contents);
    }

    public void setContents (Widget header, Widget contents)
    {
        setWidget(0, 0, header);
        setWidget(1, 0, contents);
    }

    protected void updateTitle (String title)
    {
        if (_bheader.getParent() != null) {
            _bheader.setText(0, 1, title);
        } else {
            setWidget(0, 0, MsoyUI.createLabel(title, "Title"));
        }
    }

    protected SmartTable _bheader;
}
