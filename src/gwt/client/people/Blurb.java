//
// $Id$

package client.people;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.client.ProfileService;

import client.shell.Application;
import client.util.MsoyUI;
import client.util.TongueLabel;

/**
 * Contains a chunk of content that a user would want to display on their personal page.
 */
public abstract class Blurb extends SmartTable
{
    /**
     * Returns true if we should display this blurb, false if we should skip it.
     */
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return true;
    }

    /**
     * Configures this blurb with a context and the member id for whom it is displaying content.
     */
    public void init (ProfileService.ProfileResult pdata)
    {
        _name = pdata.name;
    }

    protected Blurb ()
    {
        super("Blurb", 0, 0);
    }

    protected void setBlurbTitle (String title)
    {
        setWidget(0, 0, new TongueLabel(title));
    }

    public void setContent (Widget content)
    {
        setWidget(1, 0, content, 1, "BlurbContent");
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
            setWidget(2, 0, widget, 1, "BlurbFooter");
        }
    }

    protected MemberName _name;
}
