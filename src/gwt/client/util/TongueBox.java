//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import client.shell.Application;

/**
 * Displays content with a tongue label header and an optional right-aligned footer widget.
 */
public class TongueBox extends SmartTable
{
    public TongueBox ()
    {
        super("tongueBox", 0, 0);
    }

    public void setHeader (String title)
    {
        SmartTable header = new SmartTable("Header", 0, 0);
        header.setText(0, 0, title, 1, "Base");
        Image line = new Image("/images/ui/grey_line.png");
        line.setWidth("100%");
        line.setHeight("1px");
        header.setWidget(0, 1, line, 1, "Line");
        setWidget(0, 0, header);
    }

    public void setContent (Widget content)
    {
        setWidget(1, 0, content, 1, "Content");
    }

    public void setFooterLink (String label, String page, String args)
    {
        setFooter(Application.createLink(label, page, args));
    }

    public void setFooterLabel (String label, ClickListener onClick)
    {
        setFooter(MsoyUI.createActionLabel(label, onClick));
    }

    public void setFooter (Widget widget)
    {
        if (widget == null) {
            clearCell(2, 0);
        } else {
            setWidget(2, 0, widget, 1, "Footer");
        }
    }
}
