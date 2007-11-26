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
public class ContentFooterPanel extends VerticalPanel
{
    public ContentFooterPanel ()
    {
        setStyleName("contentFooterPanel");

        _content = new FlexTable();
        _content.setStyleName("Content");
        _content.setCellPadding(0);
        _content.setCellSpacing(5);
        add(_content);

        _footer = new FlowPanel();
        _footer.setStyleName("Footer");
        add(_footer);
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
