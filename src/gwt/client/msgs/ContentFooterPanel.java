//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays contents with a footer bar at the bottom containing buttons.
 */
public class ContentFooterPanel extends FlexTable
{
    public ContentFooterPanel ()
    {
        setStyleName("contentFooterPanel");
        setCellSpacing(0);
        setCellPadding(0);

        _content = new FlexTable();
        _content.setCellPadding(0);
        _content.setCellSpacing(5);
        setWidget(0, 0, _content);
        getFlexCellFormatter().setStyleName(0, 0, "Content");

        setWidget(1, 0, _footer = new FlowPanel());
        getFlexCellFormatter().setStyleName(1, 0, "Footer");
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

    protected FlexTable _content;
    protected FlowPanel _footer;
}
