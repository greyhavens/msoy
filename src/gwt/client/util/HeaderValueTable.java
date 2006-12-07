//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class HeaderValueTable extends FlexTable
{
    public HeaderValueTable ()
    {
        super();
        // default style
        setStyleName("headerValueTable");
        setBorderWidth(0);
        setCellSpacing(0);
        setCellPadding(3);
    }
    
    public void addRow (String lhead, String lval, String rhead, String rval)
    {
        addRow(lhead, new Label(lval), rhead, new Label(rval));
    }

    public void addRow (String lhead, Widget lval, String rhead, String rval)
    {
        addRow(lhead, lval, rhead, new Label(rval));
    }

    public void addRow (String lhead, String lval, String rhead, Widget rval)
    {
        addRow(lhead, new Label(lval), rhead, rval);
    }

    public void addRow (String lhead, Widget lval, String rhead, Widget rval)
    {
        int row = getRowCount();
        FlexCellFormatter formatter = getFlexCellFormatter();
        setText(row, 0, lhead + ":");
        setWidget(row, 1, lval);
        setText(row, 2, rhead + ":");
        setWidget(row, 3, rval);
        getRowFormatter().setStyleName(row, "dataRow");
        formatter.setAlignment(row, 0, HasAlignment.ALIGN_RIGHT, HasAlignment.ALIGN_MIDDLE);
        formatter.setAlignment(row, 1, HasAlignment.ALIGN_LEFT, HasAlignment.ALIGN_MIDDLE);
        formatter.setAlignment(row, 2, HasAlignment.ALIGN_RIGHT, HasAlignment.ALIGN_MIDDLE);
        formatter.setAlignment(row, 3, HasAlignment.ALIGN_LEFT, HasAlignment.ALIGN_MIDDLE);
    }

    public void addRow (String head, String val)
    {
        addRow(head, new Label(val));
    }

    public void addRow (String head, Widget val)
    {
        int row = getRowCount();
        FlexCellFormatter formatter = getFlexCellFormatter();
        setText(row, 0, head + ":");
        setWidget(row, 1, val);
        getFlexCellFormatter().setColSpan(row, 1, 3);
        getRowFormatter().setStyleName(row, "dataRow");
        formatter.setAlignment(row, 0, HasAlignment.ALIGN_RIGHT, HasAlignment.ALIGN_MIDDLE);
        formatter.setAlignment(row, 1, HasAlignment.ALIGN_LEFT, HasAlignment.ALIGN_MIDDLE);
    }

    public void addRow (Widget val)
    {
        int row = getRowCount();
        FlexCellFormatter formatter = getFlexCellFormatter();
        setWidget(row, 0, val);
        getFlexCellFormatter().setColSpan(row, 1, 4);
        getRowFormatter().setStyleName(row, "dataRow");
        formatter.setAlignment(row, 0, HasAlignment.ALIGN_CENTER, HasAlignment.ALIGN_MIDDLE);
    }

    public void addHeader (String header)
    {
        int row = getRowCount();
        setText(row, 0, header);
        getFlexCellFormatter().setColSpan(row, 0, 4);
        getFlexCellFormatter().setAlignment(
            row, 0, HasAlignment.ALIGN_CENTER, HasAlignment.ALIGN_MIDDLE);
        getRowFormatter().setStyleName(row, "headerRow");
    }
}
