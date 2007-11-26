//
// $Id$

package client.msgs;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import client.util.MsoyUI;

/**
 * Displays a list of messages or threads things with a title and possibly a back button.
 */
public class TitledListPanel extends VerticalPanel
{
    public TitledListPanel ()
    {
        setStyleName("titledListPanel");

        _bheader = new FlexTable();
        _bheader.setCellSpacing(0);
        _bheader.setCellPadding(0);
        _bheader.setStyleName("Header");
        _bheader.setWidget(0, 0, MsoyUI.createActionLabel("", "Back", new ClickListener() {
            public void onClick (Widget sender) {
                History.back();
            }
        }));
        _bheader.getFlexCellFormatter().setStyleName(0, 1, "Title");
        _bheader.getFlexCellFormatter().setWidth(0, 1, "100%");

        _theader = new FlowPanel();
        _theader.setStyleName("Header");
    }

    public void setContents (String title, Widget contents, boolean backButton)
    {
        clear();
        if (backButton) {
            add(_bheader);
        } else {
            add(_theader);
        }
        updateTitle(title);
        add(contents);
    }

    public void setContents (Widget header, Widget contents)
    {
        clear();
        add(header);
        add(contents);
    }

    protected void updateTitle (String title)
    {
        if (_bheader.isAttached()) {
            _bheader.setText(0, 1, title);
        } else {
            _theader.clear();
            _theader.add(MsoyUI.createLabel(title, "Title"));
        }
    }

    protected FlexTable _bheader;
    protected FlowPanel _theader;
}
