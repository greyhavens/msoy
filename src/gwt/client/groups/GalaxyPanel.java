//
// $Id$

package client.groups;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.DataModel;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.group.gwt.GalaxyData;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.group.gwt.MyGroupCard;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceBackedDataModel;
import client.util.ServiceUtil;

/**
 * Display the public groups in a sensical manner, including a sorted list of characters that
 * start the groups, allowing people to select a subset of the public groups to view.
 */
public class GalaxyPanel extends FlowPanel
{
    public GalaxyPanel ()
    {
        setStyleName("galaxyPanel");

        // search box floats on far right
        FloatPanel search = new FloatPanel("Search");
        search.add(_searchInput = MsoyUI.createTextBox("", 255, 20));
        ClickListener doSearch = new ClickListener() {
            public void onClick (Widget sender) {
                Link.go(Pages.GROUPS, Args.compose("search", "0", _searchInput.getText()));
            }
        };
        _searchInput.addKeyboardListener(new EnterClickAdapter(doSearch));
        search.add(new Button(_msgs.galaxySearch(), doSearch));
        add(search);

        // tag currently being searched floats in middle
        add(_currentTag = MsoyUI.createFlowPanel("CurrentTag"));

        FloatPanel content = new FloatPanel("Content");
        add(content);

        FlowPanel leftColumn = MsoyUI.createFlowPanel("LeftColumn");

        _myGroups = MsoyUI.createFlowPanel("MyGroups");
        if (!CShell.isGuest()) {
            leftColumn.add(MsoyUI.createLabel(_msgs.galaxyMyGroupsTitle(), "MyGroupsHeader"));
            leftColumn.add(_myGroups = MsoyUI.createFlowPanel("MyGroups"));
        }
        leftColumn.add(_popularTags = MsoyUI.createFlowPanel("tagCloud"));
        content.add(leftColumn);

        _groupGrid = new PagedGrid<GroupCard>(GRID_ROWS, GRID_COLUMNS) {
            protected void displayPageFromClick (int page) {
                Link.go(Pages.GROUPS, Args.compose(_action, ""+page, _arg));
            }
            protected Widget createWidget (GroupCard card) {
                return new GroupWidget(card);
            }
            protected String getEmptyMessage () {
                return _msgs.galaxyNoGroups();
            }
        };
        _groupGrid.addStyleName("GroupsList");
        content.add(_groupGrid);

        // add info on creating a Whirled
        if (!CShell.isGuest()) {
            SmartTable create = new SmartTable("Create", 0, 0);
            create.setText(0, 0, _msgs.galaxyCreateTitle(), 3, "Header");
            create.setText(1, 0, _msgs.galaxyCreateBlurb(), 1, "Pitch");
            create.setWidget(1, 1, WidgetUtil.makeShim(10, 10));
            ClickListener onClick = Link.createListener(Pages.GROUPS, "edit");
            create.setWidget(1, 2, new Button(_msgs.galaxyCreate(), onClick), 1, "Button");
            add(create);
        }

        _groupsvc.getGalaxyData(new MsoyCallback<GalaxyData>() {
            public void onSuccess (GalaxyData galaxy) {
                init(galaxy);
            }
        });
    }

    /**
     * Called by parent page when the url changes; may be the first time the panel was loaded, or
     * the result of a tag, text search, or page change query.
     */
    public void setArgs (Args args)
    {
        String action = args.get(0, ""), arg = args.get(2, "");
        int page = args.get(1, 0);

        // clear out our status indicators (they'll be put back later)
        _currentTag.clear();
        _searchInput.setText("");

        // group-tag_NN_TAG
        if (action.equals("tag") && displayTag(arg, page)) {
            return;
        }

        // group-search_NN_QUERY
        if (action.equals("search") && displaySearch(arg, page)) {
            return;
        }

        // group-p_NN or group
        setModel("p", "", page, new ModelLoader() {
            public void loadModel (MsoyCallback<DataModel<GroupCard>> callback) {
                callback.onSuccess(_gmodel);
            }
        });
    }

    /**
     * Called when data is retrieved after this panel is created; populate the page with data.
     */
    protected void init (GalaxyData data)
    {
        // set up my groups
        if (!CShell.isGuest() && data.myGroups.size() == 0) {
            _myGroups.add(MsoyUI.createLabel(_msgs.galaxyMyGroupsNone(), "NoGroups"));
        } else {
            for (MyGroupCard group : data.myGroups) {
                _myGroups.add(new MyGroupWidget(group));
            }
            Widget seeAllLink = Link.create(_msgs.galaxyMyGroupsSeeAll(), Pages.GROUPS,
                "mywhirleds");
            seeAllLink.addStyleName("SeeAll");
            _myGroups.add(seeAllLink);
        }

        // set up our popular tags
        if (data.popularTags.size() == 0) {
            _popularTags.add(MsoyUI.createLabel(_msgs.galaxyNoPopularTags(), "Link"));
        } else {
            for (String tag : data.popularTags) {
                Widget link = Link.create(
                    tag, Pages.GROUPS, Args.compose("tag", "0", tag));
                link.addStyleName("Link");
                link.removeStyleName("inline");
                _popularTags.add(link);
            }
        }
    }

    protected boolean displayTag (final String tag, int page)
    {
        if (tag.equals("")) {
            return false;
        }

        InlineLabel tagLabel = new InlineLabel(_msgs.galaxyCurrentTag(tag) + " ");
        tagLabel.addStyleName("Label");
        _currentTag.add(tagLabel);
        _currentTag.add(new InlineLabel("("));
        Widget clear = Link.create(_msgs.galaxyTagClear(), Pages.GROUPS, "");
        clear.addStyleName("inline");
        _currentTag.add(clear);
        _currentTag.add(new InlineLabel(")"));

        setModel("tag", tag, page, new ModelLoader() {
            public void loadModel (MsoyCallback<DataModel<GroupCard>> callback) {
                _groupsvc.searchForTag(tag, new ListToModel(callback));
            }
        });
        return true;
    }

    protected boolean displaySearch (final String query, int page)
    {
        if (query.equals("")) {
            return false;
        }
        _searchInput.setText(query);
        setModel("search", query, page, new ModelLoader() {
            public void loadModel (MsoyCallback<DataModel<GroupCard>> callback) {
                _groupsvc.searchGroups(query, new ListToModel(callback));
            }
        });
        return true;
    }

    protected void setModel (final String action, final String arg, final int page,
                             ModelLoader loader)
    {
        if (action.equals(_action) && arg.equals(_arg)) {
            _groupGrid.displayPage(page, false);
        } else {
            loader.loadModel(new MsoyCallback<DataModel<GroupCard>>() {
                public void onSuccess (DataModel<GroupCard> model) {
                    _action = action;
                    _arg = arg;
                    _groupGrid.setModel(model, page);
                }
            });
        }
    }

    protected static interface ModelLoader
    {
        void loadModel (MsoyCallback<DataModel<GroupCard>> callback);
    }

    /**
     * A single one of "my" groups on the left column.
     */
    protected class MyGroupWidget extends FloatPanel
    {
        public MyGroupWidget (MyGroupCard group)
        {
            super("MyGroup");
            ClickListener groupListener = Link.createListener(Pages.GROUPS, Args.compose("d",
                group.name.getGroupId()));
            add(new ThumbBox(group.logo, MediaDesc.QUARTER_THUMBNAIL_SIZE, groupListener));
            add(MsoyUI.createActionLabel(group.name.toString(), "GroupName", groupListener));
            add(MsoyUI.createActionLabel(_msgs.galaxyMyGroupsDiscussions(),
                "Discussions", Link.createListener(
                Pages.GROUPS, Args.compose("f", group.name.getGroupId()))));
        }
    }

    /**
     * A single group in the right column grid
     */
    protected class GroupWidget extends AbsolutePanel
    {
        public GroupWidget (GroupCard group)
        {
            setStyleName("Group");
            ClickListener groupListener = Link.createListener(Pages.GROUPS, Args.compose("d",
                group.name.getGroupId()));
            add(new ThumbBox(group.logo, MediaDesc.HALF_THUMBNAIL_SIZE, groupListener), 5, 5);
            add(MsoyUI.createActionLabel(group.name.toString(), "GroupName", groupListener), 50,
                5);

            add(MsoyUI.createLabel(_msgs.galaxyMemberCount(group.memberCount + " "),
                "MemberCount"), 5, 40);

            add(MsoyUI.createActionLabel(_msgs.galaxyPeopleInRooms(group.population + ""),
                "InRooms", Link.createListener(Pages.WORLD, "s" + group.name.getGroupId())),
                5, 60);

            add(MsoyUI.createActionLabel(_msgs.galaxyThreadCount(group.threadCount + ""),
                "ThreadCount", Link.createListener(
                Pages.GROUPS, Args.compose("f",
                    group.name.getGroupId()))), 5, 80);

        }
    }

    protected static class ListToModel implements AsyncCallback<List<GroupCard>>
    {
        public ListToModel (AsyncCallback<DataModel<GroupCard>> target) {
            _target = target;
        }
        public void onSuccess (List<GroupCard> list) {
            _target.onSuccess(new SimpleDataModel<GroupCard>(list));
        }
        public void onFailure (Throwable cause) {
            _target.onFailure(cause);
        }
        protected AsyncCallback<DataModel<GroupCard>> _target;
    }

    /** Handles loading groups a page at a time. */
    protected ServiceBackedDataModel<GroupCard, GroupService.GroupsResult> _gmodel =
        new ServiceBackedDataModel<GroupCard, GroupService.GroupsResult>() {
        protected void callFetchService (int start, int count, boolean needCount,
                                         AsyncCallback<GroupService.GroupsResult> callback) {
            _groupsvc.getGroups(start, count, needCount, callback);
        }
        protected int getCount (GroupService.GroupsResult result) {
            return result.totalCount;
        }
        protected List<GroupCard> getRows (GroupService.GroupsResult result) {
            return result.groups;
        }
    };

    protected String _action, _arg;
    protected FlowPanel _popularTags, _currentTag;
    protected TextBox _searchInput;
    protected PagedGrid<GroupCard> _groupGrid;
    protected FlowPanel _myGroups;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)
        ServiceUtil.bind(GWT.create(GroupService.class), GroupService.ENTRY_POINT);

    protected static final int GRID_ROWS = 4;
    protected static final int GRID_COLUMNS = 4;
}
