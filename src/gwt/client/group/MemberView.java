//
// $Id$

package client.group;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.DockPanel;

import client.util.HeaderValueTable;

import com.threerings.msoy.web.client.WebContext;

/**
 * Display the details of a group member.  Right now this means a link to their profile, and their
 * rank.  Managers are allowed to promote members to Manager in all groups, and boot members in 
 * non-public groups.  Managers can also demote any Manager that is their Junior.
 */
public class MemberView extends PopupPanel
{
    public MemberView (WebContext ctx, int memberId)
    {
        super(true);
        _ctx = ctx;
        _memberId = memberId;
        setStyleName("memberPopup");

        _content = new DockPanel();
        setWidget(_content);

        _table = new HeaderValueTable();
        _content.add(_table, DockPanel.CENTER);

        _table.addHeader("Viewing Member " + memberId);
    }

    protected WebContext _ctx;
    protected int _memberId;

    protected DockPanel _content;
    protected HeaderValueTable _table;
}
