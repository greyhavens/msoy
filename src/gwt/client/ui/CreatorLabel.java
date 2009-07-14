//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.gwt.ui.InlineLabel;

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
        clear();
        add(new InlineLabel(_cmsgs.creatorBy() + " "));
        add(Link.memberView(name.toString(), name.getMemberId()));
    }

    public void setBrand (GroupName name)
    {
        clear();
        add(new InlineLabel(_cmsgs.creatorBy() + " "));
        add(Link.groupView(name.toString(), name.getGroupId()));
    }

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
