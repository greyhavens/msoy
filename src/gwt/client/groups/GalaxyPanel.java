//
// $Id$

package client.groups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.ui.InlinePanel;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MediaDescSize;
import com.threerings.msoy.group.gwt.GalaxyData;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.InfoCallback;
import client.util.Link;

/**
 * Display the public groups in a sensical manner, including a sorted list of characters that
 * start the groups, allowing people to select a subset of the public groups to view.
 */
public class GalaxyPanel extends FlowPanel
{
    public GalaxyPanel ()
    {
        setStyleName("galaxyPanel");
        add(MsoyUI.createNowLoading());
        _groupsvc.getGalaxyData(new InfoCallback<GalaxyData>() {
            public void onSuccess (GalaxyData galaxy) {
                init(galaxy);
            }
        });
    }

    /**
     * Called when my groups & tags data is retrieved after this panel is created; populate the
     * page with data.
     */
    protected void init (final GalaxyData data)
    {
        clear();

        // search box floats on far right
        FloatPanel search = new FloatPanel("Search");
        search.add(MsoyUI.createLabel(_msgs.galaxySearchTitle(), "SearchTitle"));
        final TextBox searchInput = MsoyUI.createTextBox("", 255, 15);
        search.add(searchInput);
        ClickHandler doSearch = new ClickHandler() {
            public void onClick (ClickEvent event) {
                Link.go(Pages.GROUPS, ACTION_SEARCH, 0, searchInput.getText().trim());
            }
        };
        EnterClickAdapter.bind(searchInput, doSearch);
        search.add(MsoyUI.createImageButton("GoButton", doSearch));
        add(search);

        // blurb about groups on the left
        InlinePanel introBlurb = new InlinePanel("IntroBlurb");
        add(introBlurb);
        introBlurb.add(MsoyUI.createHTML(_msgs.galaxyIntroBlurb(), null));
        if (!CShell.isGuest()) {
            introBlurb.add(MsoyUI.createHTML(_msgs.galaxyIntroCreate(), null));
        }

        // category tag links
        add(new CategoryLinks());

        FloatPanel content = new FloatPanel("Content");
        add(content);

        FlowPanel leftColumn = MsoyUI.createFlowPanel("LeftColumn");
        content.add(leftColumn);

        // add the My Groups display
        leftColumn.add(MsoyUI.createLabel(_msgs.galaxyMyGroupsTitle(), "MyGroupsHeader"));
        FlowPanel myGroups = MsoyUI.createFlowPanel("QuickGroups");
        myGroups.addStyleName("MyGroups");
        leftColumn.add(myGroups);
        if (data.myGroups.size() == 0) {
            myGroups.add(MsoyUI.createLabel(_msgs.galaxyMyGroupsNone(), "NoGroups"));
        } else {
            for (GroupCard group : data.myGroups) {
                myGroups.add(createQuickGroupWidget(group));
            }
            myGroups.add(Link.create(_msgs.galaxySeeAll(), "SeeAll", Pages.GROUPS, "mygroups"));
        }

        // add the official groups display
        FlowPanel officialGroups = MsoyUI.createFlowPanel("QuickGroups");
        leftColumn.add(MsoyUI.createLabel(_msgs.galaxyOfficialGroupsTitle(),
                                          "OfficialGroupsHeader"));
        officialGroups.addStyleName("OfficialGroups");
        for (GroupCard group : data.officialGroups) {
            officialGroups.add(createQuickGroupWidget(group));
        }
        leftColumn.add(officialGroups);

        content.add(MsoyUI.createLabel(_msgs.galaxyFeaturedTitle(), "FeaturedHeader"));

        SmartTable grid = new SmartTable("GroupsList", 0, 10);
        int row = 0, col = 0;
        for (GroupCard card : data.featuredGroups) {
            grid.setWidget(row, col, createGroupWidget(card));
            if (++col == GRID_COLUMNS) {
                row++;
                col = 0;
            }
        }
        grid.addWidget(Link.create(_msgs.galaxySeeAll(), Pages.GROUPS, "list"),
                       GRID_COLUMNS, "SeeAll");
        content.add(grid);
    }

    /**
     * A single one of "my" groups on the left column.
     */
    protected Widget createQuickGroupWidget (GroupCard group)
    {
        FloatPanel widget = new FloatPanel("Group");
        widget.add(new ThumbBox(group.getLogo(), MediaDescSize.QUARTER_THUMBNAIL_SIZE,
                                Pages.GROUPS, "d", group.name.getGroupId()));
        widget.add(Link.create(group.name.toString(), "Name",
                               Pages.GROUPS, "d", group.name.getGroupId()));
        widget.add(Link.create(_msgs.galaxyMyGroupsDiscussions(), "Discussions",
                               Pages.GROUPS, "f", group.name.getGroupId()));
        return widget;
    }

    /**
     * A single group in the right column grid
     */
    protected Widget createGroupWidget (GroupCard group)
    {
        FlowPanel widget = MsoyUI.createFlowPanel("Group");

        FloatPanel logoAndName = new FloatPanel("LogoAndName");
        logoAndName.add(new ThumbBox(group.getLogo(), MediaDescSize.HALF_THUMBNAIL_SIZE,
                                     Pages.GROUPS, "d", group.name.getGroupId()));
        logoAndName.add(Link.create(group.name.toString(), "GroupName",
                                    Pages.GROUPS, "d", group.name.getGroupId()));

        widget.add(logoAndName);
        widget.add(MsoyUI.createLabel(_msgs.galaxyMemberCount(group.memberCount + " "),
                                      "MemberCount"));
        widget.add(Link.create(_msgs.galaxyPeopleInRooms(group.population + ""), "InRooms",
                               Pages.WORLD, "s" + group.homeSceneId));
        widget.add(Link.create(_msgs.galaxyDiscussions(), "ThreadCount",
                               Pages.GROUPS, "f", group.name.getGroupId()));
        return widget;
    }

    /* dynamic widgets */
    protected CategoryLinks _categoryLinks;
    
    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = GWT.create(GroupService.class);

    protected static final String ACTION_SEARCH = "search";
    protected static final int GRID_COLUMNS = 4;
}
