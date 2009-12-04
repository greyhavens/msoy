//
// $Id$

package client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.web.gwt.Pages;

import client.util.Link;

/**
 * Displays content with a tongue label header and an optional right-aligned footer widget.
 */
public class TongueBox extends SmartTable
    implements ClickHandler
{
    public TongueBox ()
    {
        super("tongueBox", 0, 0);
    }

    public TongueBox (String title, Widget content)
    {
        this(null, title, content, null);
    }

    public TongueBox (String title, String content, boolean isHTML)
    {
        this();
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

    public TongueBox (Image icon, String title, Widget content, ClickHandler clicker)
    {
        this();
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
            header.cell(0, 0).widget(MsoyUI.createHTML(title, null)).styles("Base");
            header.addClickHandler(this);
        } else if (title == null && icon != null) {
            header.cell(0, 0).widget(icon).styles("Base");
            header.addClickHandler(this);
        } else {
            SmartTable base = new SmartTable("BaseContents", 0, 0);
            base.cell(0, 0).widget(icon);
            base.cell(0, 1).widget(MsoyUI.createHTML(title, null)).styles("BaseText");
            base.addClickHandler(this);
            header.cell(0, 0).widget(base).styles("Base");
        }

        Image line = new Image("/images/ui/grey_line.png");
        line.setWidth("100%");
        line.setHeight("1px");
        header.cell(0, 1).widget(line).styles("Line");
        cell(0, 0).widget(header);
    }

    public void setContent (Widget content)
    {
        cell(1, 0).widget(content).styles("TContent");
    }

    public void setContent (String content, boolean isHTML)
    {
        if (isHTML) {
            cell(1, 0).widget(MsoyUI.createHTML(content, null));
        } else {
            cell(1, 0).text(content);
        }
        cell(1, 0).styles("TContent");
    }

    public void setFooterLink (String text, Pages page, Object... args)
    {
        setFooter(Link.create(text, page, args));
    }

    public Label setFooterLabel (String text, ClickHandler onClick)
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
            cell(2, 0).widget(widget).styles("TFooter").alignRight();
        } else if (getRowCount() > 2) {
            cell(2, 0).clear();
        }
    }

    public void onClick (ClickEvent event)
    {
        if (_clicker == null) {
            return;
        }
        Cell cell = getCellForEvent(event);
        String style = getCellFormatter().getStyleName(cell.getRowIndex(), cell.getCellIndex());
        if (!"Line".equals(style)) {
            _clicker.onClick(null);
        }
    }

    protected ClickHandler _clicker;
}
