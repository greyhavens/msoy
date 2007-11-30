//
// $Id$

package client.msgs;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import client.util.MsoyUI;

/**
 * Displays a list of messages or threads things with a title and possibly a back button.
 */
public class TitledListPanel extends FlexTable
{
    public TitledListPanel ()
    {
        setStyleName("titledListPanel");
        setCellPadding(0);
        setCellSpacing(0);
        getFlexCellFormatter().setStyleName(0, 0, "Header");

        _bheader = new FlexTable();
        _bheader.setCellSpacing(0);
        _bheader.setCellPadding(0);
        _bheader.setWidget(0, 0, MsoyUI.createActionLabel("", "Back", new ClickListener() {
            public void onClick (Widget sender) {
                History.back();
            }
        }));
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
            if (_bheader.getCellCount(0) > 1) {
                _bheader.setText(0, 2, "");
            }
        }
        updateTitle(title);
        setWidget(1, 0, contents);
    }

    public void setContents (Widget header, Widget contents)
    {
        setWidget(0, 0, header);
        setWidget(1, 0, contents);
    }

    protected void setRightBits (Widget rightBox)
    {
        _bheader.setWidget(0, 2, rightBox);
        _bheader.getFlexCellFormatter().setStyleName(0, 2, "RightBits");
    }

    protected void updateTitle (String title)
    {
        if (_bheader.getParent() != null) {
            _bheader.setText(0, 1, title);
        } else {
            setWidget(0, 0, MsoyUI.createLabel(title, "Title"));
        }
    }

    protected FlexTable _bheader;
}
