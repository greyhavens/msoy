//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.MemberCard;

import client.shell.ShellMessages;
import client.util.Link;

/**
 * Displays a creator's name with "by Foozle" where Foozle is a link to the creator's profile page
 * if it's a player, or its group page if it's a brand.
 */
public class CreatorLabel extends FlowPanel
{
    public CreatorLabel ()
    {
        addStyleName("creator");
    }

    public CreatorLabel (MemberName name)
    {
        this();
        setMember(name);
    }

    public CreatorLabel (GroupName name)
    {
        this();
        setBrand(name);
    }

    public void setMember (MemberName name)
    {
        setBy(Link.memberView(name));
    }

    public void setMember (MemberCard card)
    {
        setBy(Link.memberView(card));
    }

    public void setBrand (GroupName group)
    {
        setBy(Link.groupView(group));
    }

    protected void setBy (Widget link)
    {
        clear();
        add(new InlineLabel(_cmsgs.creatorBy() + " "));
        add(link);
    }

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
