//
// $Id$

package client.item;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.web.data.MemberName;

import com.threerings.gwt.ui.InlineLabel;

import client.shell.Application;

import client.util.PopupMenu;

/**
 * Displays a creator's name with "by Foozle" where Foozle is a link to the creator's profile page.
 */
public class CreatorLabel extends FlowPanel
{
    public void setMember (MemberName name)
    {
        setMember(name, null);
    }

    public void setMember (MemberName name, PopupMenu menu) 
    {
        while (getWidgetCount() > 0) {
            remove(0);
        }

        add(new InlineLabel(CItem.imsgs.creatorBy() + " "));
        if (menu == null) {
            add(Application.memberViewLink(name.toString(), name.getMemberId()));
        } else {
            InlineLabel text = new InlineLabel(name.toString());
            text.addStyleName("LabelLink");
            menu.setTrigger(text);
            add(text);
        }
    }
}
