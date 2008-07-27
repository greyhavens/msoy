//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import client.ui.ContentFooterPanel;
import client.ui.MsoyUI;

/**
 * A content footer panel customized for use by our forum bits.
 */
public class TableFooterPanel extends ContentFooterPanel
{
    public TableFooterPanel ()
    {
        super(new FlexTable(), new FlowPanel());
        _content = (FlexTable)getWidget(0, 0);
        _content.setCellPadding(0);
        _content.setCellSpacing(5);
        _content.setWidth("100%");
        _footer = (FlowPanel)getWidget(1, 0);
    }

    protected int addRow (String label, Widget widget)
    {
        int row = _content.getRowCount();
        _content.setText(row, 0, label);
        _content.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        _content.setWidget(row, 1, widget);
        return row;
    }

    protected int addRow (String text)
    {
        int row = _content.getRowCount();
        _content.setText(row, 0, text);
        _content.getFlexCellFormatter().setColSpan(row, 0, 2);
        return row;
    }

    protected int addRow (Widget widget)
    {
        int row = _content.getRowCount();
        _content.setWidget(row, 0, widget);
        _content.getFlexCellFormatter().setColSpan(row, 0, 2);
        return row;
    }

    protected void addFooterButton (Button button)
    {
        if (_footer.getWidgetCount() > 0) {
            // I'd love to use a <div> here with a width but that doesn't work, awesome!
            _footer.add(MsoyUI.createHTML("&nbsp;", "inline"));
        }
        _footer.add(button);
    }

    protected FlexTable _content;
    protected FlowPanel _footer;
}
