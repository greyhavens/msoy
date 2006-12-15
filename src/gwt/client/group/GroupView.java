//
// $Id$

package client.group;

import java.util.Iterator;
import java.util.List;
import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupDetail;
import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.MemberName;

import client.shell.MsoyEntryPoint;
import client.util.WebContext;
import client.util.PromptPopup;

import client.group.GroupEdit.GroupSubmissionListener;

/**
 * Display the details of a group, including all its members, and let managers remove other members
 * (unless the group's policy is PUBLIC) and pop up the group editor.
 */
public class GroupView extends DockPanel
    implements GroupSubmissionListener
{
    public GroupView (WebContext ctx, int groupId)
    {
        super();
        _ctx = ctx;

        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("groupDetailErrors");

        loadGroup(groupId);
    }

    /**
     * Called by {@link GroupEdit}; reloads the group.
     */
    public void groupSubmitted (Group group)
    {
        loadGroup(group.groupId);
    }

    /**
     * Fetches the details of the group from the backend and trigger a UI rebuild.
     */
    protected void loadGroup (int groupId)
    {
        _ctx.groupsvc.getGroupDetail(_ctx.creds, groupId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _detail = (GroupDetail) result;
                _group = _detail.group;
                // in case this object is used more than once, make sure that _me is at least 
                // not stale
                _me = null;
                if (_ctx.creds != null) {
                    _me = GroupView.findMember(_detail.members, _ctx.creds.memberId);
                }
                buildUI();
            }
            public void onFailure (Throwable caught) {
                GWT.log("loadGroup failed", caught);
                // TODO: if ServiceException, translate
                addError("Failed to load group.");
            }
        });
    }

    /**
     * Rebuilds the UI from scratch.
     */
    protected void buildUI ()
    {
        clear();
        setStyleName("groupView");
        boolean amManager = _me != null && _me.rank == GroupMembership.RANK_MANAGER;

        VerticalPanel logoPanel = new VerticalPanel();
        logoPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        logoPanel.setStyleName("logoPanel");
        String path = _group.logo == null ? "/msoy/images/default_logo.png" : 
            MsoyEntryPoint.toMediaPath(_group.logo.getMediaPath());
        logoPanel.add(new Image(path));
        HorizontalPanel links = new HorizontalPanel();
        links.setStyleName("links");
        links.setSpacing(8);
        links.add(new Anchor("/world/index.html#g" +  _group.groupId, "Hall"));
        links.add(new Anchor("", "Forum"));
        if (_group.homepageUrl != null) {
            links.add(new Anchor(_group.homepageUrl, "Homepage"));
        }
        /*if (amManager) {
            Hyperlink edit = new Hyperlink();
            edit.setText("Edit Group");
            edit.addClickListener(new ClickListener() {
                public void onClick(Widget sender) {
                    new GroupEdit(_ctx, _group, GroupView.this).show();
                }
            });
            links.add(edit);
        }*/
        logoPanel.add(links);
        VerticalPanel established = new VerticalPanel();
        established.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        established.setStyleName("established");
        // Date.toLocaleString cannot be made to only print the date (sans time of day).
        // This has got to be the biggest waste of code ever.  Get with the Date formatting
        // program, GWT!
        established.add(new HTML("Est. " + getMonthName(_group.creationDate.getMonth()) + " " + 
            _group.creationDate.getDay() + ", " + (_group.creationDate.getYear() + 1900)));
        established.add(new HTML("by <a href='" + MsoyEntryPoint.memberViewPath(
            _detail.creator.getMemberId()) + "'>" + _detail.creator + "</a>"));
        logoPanel.add(established);
        HTML policy = new HTML(getPolicyName(_group.policy));
        policy.setStyleName("policy");
        logoPanel.add(policy);

        ScrollPanel description = new ScrollPanel();
        description.setStyleName("descriptionPanel");
        String descriptionHtml = "<span class='name'>" + _group.name + "</span><br />";
        if (_group.blurb != null) {
            descriptionHtml += "<span class='blurb'>" + _group.blurb + "</span><br />";
        }
        if (_group.charter != null) {
            descriptionHtml += "<p class='charter'>" + _group.charter + "</p>";
        }
        description.add(new HTML(descriptionHtml));
        add(description, DockPanel.CENTER);

        FlexTable people = new FlexTable();
        people.setStyleName("peoplePanel");
        people.setText(0, 0, "Managers:");
        people.setText(1, 0, "Members:");
        FlowPanel managers = new FlowPanel();
        FlowPanel members = new FlowPanel();
        Iterator i = _detail.members.iterator();
        boolean firstManager = true;
        boolean firstMember = true;
        while (i.hasNext()) {
            final GroupMembership membership = (GroupMembership) i.next();
            final MemberName name = (MemberName) membership.member;
            FlowPanel peoplePanel;
            if (membership.rank == GroupMembership.RANK_MANAGER) {
                if (firstManager) {
                    firstManager = false;
                } else {
                    managers.add(new InlineLabel(", "));
                }
                peoplePanel = managers;
            } else {
                if (firstMember) {
                    firstMember = false;
                } else {
                    members.add(new InlineLabel(", "));
                }
                peoplePanel = members;
            }
            if (amManager) {
                MenuBar menu = getManagerMenuBar(membership);
                final PopupPanel personMenuPanel = new PopupPanel(true);
                personMenuPanel.add(menu);
                final InlineLabel person = new InlineLabel(name.toString());
                person.addStyleName("labelLink");
                // use a MouseListener instead of ClickListener so we can get at the mouse (x,y)
                person.addMouseListener(new MouseListener() {
                    public void onMouseDown (Widget sender, int x, int y) { 
                        personMenuPanel.setPopupPosition(person.getAbsoluteLeft() + x, 
                            person.getAbsoluteTop() + y);
                        personMenuPanel.show();
                    }
                    public void onMouseLeave (Widget sender) { }
                    public void onMouseUp (Widget sender, int x, int y) { }
                    public void onMouseEnter (Widget sender) { }
                    public void onMouseMove (Widget sender, int x, int y) { }
                });
                peoplePanel.add(person);
            } else {
                peoplePanel.add(new Anchor(MsoyEntryPoint.memberViewPath(
                    name.getMemberId()), name.toString()));
            }
        }
        people.setWidget(0, 1, managers);
        people.setWidget(1, 1, members);

        // SOUTH must be added before WEST for the colspan to be set correctly... 
        add(_errorContainer, DockPanel.NORTH);
        add(people, DockPanel.SOUTH);
        add(logoPanel, DockPanel.WEST);
    }

    /**
     * performs a simple scan of the list of GroupMembership objects to find and return the 
     * first GroupMembership that refers to the requested memberId.
     */
    static protected GroupMembership findMember (List members, int memberId) 
    {
        Iterator i = members.iterator();
        GroupMembership member = null;
        while ((member == null || member.member.getMemberId() != memberId) && i.hasNext()) {
            member = (GroupMembership)i.next();
        }
        return (member != null && member.member.getMemberId() == memberId) ? member : null;
    }

    /**
     * returns the month name of the given month number, indexed from 0-11.
     */
    static protected String getMonthName (int month) 
    {
        // TODO: localize these strings
        String monthName;
        switch(month) {
        case 0:  monthName = "January"; break;
        case 1:  monthName = "February"; break;
        case 2:  monthName = "March"; break;
        case 3:  monthName = "April"; break;
        case 4:  monthName = "May"; break;
        case 5:  monthName = "June"; break;
        case 6:  monthName = "July"; break;
        case 7:  monthName = "August"; break;
        case 8:  monthName = "September"; break;
        case 9:  monthName = "October"; break;
        case 10: monthName = "November"; break;
        case 11: monthName = "December"; break;
        default: monthName = "ERROR";
        }
        return monthName;
    }

    /**
     * 100% Temporary!  This will be replaced with m.policy0, m.policy1, etc.
     */
    static protected String getPolicyName (int policy)
    {
        String policyName;
        switch(policy) {
        case Group.POLICY_PUBLIC: policyName = "Public"; break;
        case Group.POLICY_INVITE_ONLY: policyName = "Invitation Only"; break;
        case Group.POLICY_EXCLUSIVE: policyName = "Exclusive"; break;
        default: policyName = "Undefined";
        }
        return policyName;
    }

    /**
     * Get the menus for use by managers when perusing the members of their group.
     */
    protected MenuBar getManagerMenuBar(final GroupMembership membership) 
    {
        // MenuBar(true) creates a vertical menu
        MenuBar menu = new MenuBar(true);
        menu.addItem("<a href='" + MsoyEntryPoint.memberViewPath(
            membership.member.getMemberId()) + "'>View Profile</a>", true, (Command)null);
        menu.addItem("Promote", new Command() {
            public void execute() {
                (new PromptPopup("Are you sure you wish to promote " + 
                    membership.member.toString() + "?") {
                    public void onAffirmative () {
                        Window.alert("You clicked Yes!");
                    }
                    public void onNegative () {
                        Window.alert("You clicked No!");
                    }
                }).prompt();
            }
        });
        return menu;
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
    protected Group _group;
    protected GroupDetail _detail;
    protected GroupMembership _me;

    protected VerticalPanel _errorContainer;
}
