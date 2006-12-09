//
// $Id$

package client.group;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.HTML;

import client.util.HeaderValueTable;

import client.shell.MsoyEntryPoint;

import com.threerings.msoy.web.client.WebContext;

import com.threerings.msoy.web.data.GroupMembership;

/**
 * Display the details of a group member.  Right now this means a link to their profile, and their
 * rank.  Managers are allowed to promote members to Manager in all groups, and boot members in 
 * non-public groups.  Managers can also demote any Manager that is their Junior.
 */
public class MemberView extends PopupPanel
{
    public MemberView (WebContext ctx, GroupMembership membership, boolean amAdmin)
    {
        super(true);
        _ctx = ctx;
        _membership = membership;
        // TODO: a boolean is not sufficient, because it will need to be known if this is a Manager
        // that is superior to this member (who might also be a manager).
        _amAdmin = amAdmin;
        setStyleName("memberPopup");

        _content = new DockPanel();
        setWidget(_content);

        _table = new HeaderValueTable();
        _content.add(_table, DockPanel.CENTER);

        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("memberViewErrors");
        _content.add(_errorContainer, DockPanel.NORTH);

        _table.addHeader("" + _membership.member);

        _table.addRow(new HTML("<a href='" + MsoyEntryPoint.memberViewPath(
            _membership.member.getMemberId()) + "'>Profile</a>"));
        
        // TODO: bring in enough info to resolve this correctly
        /*if (_amAdmin && rank != GroupMembership.RANK_MANAGER &&
            _group.policy != Group.POLICY_PUBLIC) {
            Label removeLabel = new InlineLabel("Remove Member");
            removeLabel.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    removeMember(name.getMemberId());
                }
            });
        }*/
    }

    protected void addError (String error)
    {
        _errorContainer.add(new Label(error));
    }

    protected void clearErrors ()
    {
        _errorContainer.clear();
    }

    protected WebContext _ctx;
    protected GroupMembership _membership;
    protected boolean _amAdmin;

    protected DockPanel _content;
    protected HeaderValueTable _table;
    protected VerticalPanel _errorContainer;
}
