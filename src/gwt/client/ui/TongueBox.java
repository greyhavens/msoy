//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.web.gwt.Pages;

import client.util.Link;

/**
 * Displays content with a tongue label header and an optional right-aligned footer widget.
 */
public class TongueBox extends SmartTable
    implements TableListener
{
    public TongueBox ()
    {
        super("tongueBox", 0, 0);
    }

    public TongueBox (String title, Widget content)
    {
        this(); // not sure if zero argument constructor is automatically called
        if (title != null) {
            setHeader(title);
        }
        if (content != null) {
            setContent(content);
        }
    }

    public TongueBox (String title, String content, boolean isHTML)
    {
        this(); // not sure if zero argument constructor is automatically called
        if (title != null) {
            setHeader(title);
        }
        if (content != null) {
            setContent(content, isHTML);
        }
    }

    public TongueBox (Image icon, String title, Widget content)
    {
        this(icon, title, content, null);
    }

    public TongueBox (Image icon, String title, Widget content, ClickListener clicker)
    {
        this(); // not sure if zero argument constructor is automatically called
        _clicker = clicker;
        if (title != null || icon != null) {
            setHeader(title, icon);
        }
        if (content != null) {
            setContent(content);
        }
    }

    public void setHeader (String title)
    {
        setHeader(title, null);
    }

    public void setHeader (String title, Image icon)
    {
        SmartTable header = new SmartTable("THeader", 0, 0);
        if (title != null && icon == null) {
            header.setText(0, 0, title, 1, "Base");
            header.addTableListener(this);
        } else if (title == null && icon != null) {
            header.setWidget(0, 0, icon, 1, "Base");
            header.addTableListener(this);
        } else {
            SmartTable base = new SmartTable("BaseContents", 0, 0);
            base.setWidget(0, 0, icon);
            base.setText(0, 1, title, 1, "BaseText");
            base.addTableListener(this);
            header.setWidget(0, 0, base, 1, "Base");
        }

        Image line = new Image("/images/ui/grey_line.png");
        line.setWidth("100%");
        line.setHeight("1px");
        header.setWidget(0, 1, line, 1, "Line");
        setWidget(0, 0, header);
    }

    public void setContent (Widget content)
    {
        setWidget(1, 0, content, 1, "TContent");
    }

    public void setContent (String content, boolean isHTML)
    {
        if (isHTML) {
            setHTML(1, 0, content);
            getFlexCellFormatter().setStyleName(1, 0, "TContent");
            SafeHTML.fixAnchors(getBodyElement()); // we can't call getCellElement(), yay!
        } else {
            setText(1, 0, content, 1, "TContent");
        }
    }

    public void setFooterLink (String text, Pages page, String args)
    {
        setFooter(Link.create(text, page, args));
    }

    public Label setFooterLabel (String text, ClickListener onClick)
    {
        // annoyingly we have to stick our label into a table otherwise the Label (a div) will
        // actually be the entire width of the page; if it's clickable, that is very weird
        HorizontalPanel box = new HorizontalPanel();
        Label label = MsoyUI.createActionLabel(text, onClick);
        box.add(label);
        setFooter(box);
        return label;
    }

    public void setFooter (Widget widget)
    {
        if (widget != null) {
            setWidget(2, 0, widget, 1, "TFooter");
            getFlexCellFormatter().setHorizontalAlignment(2, 0, HasAlignment.ALIGN_RIGHT);
        } else if (getRowCount() > 2) {
            clearCell(2, 0);
        }
    }

    public void onCellClicked (SourcesTableEvents sender, int row, int cell)
    {
        if (_clicker != null && !"Line".equals(getCellFormatter().getStyleName(row, cell))) {
            _clicker.onClick(this);
        }
    }

    protected ClickListener _clicker;
}
