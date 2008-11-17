//
// $Id$

package client.groups;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.group.gwt.GalaxyData;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.group.gwt.MyGroupCard;
import com.threerings.msoy.group.gwt.GroupService.GroupQuery;
import com.threerings.msoy.group.gwt.GroupService.GroupsResult;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.Link;
import client.util.MsoyCallback;
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
                Link.go(Pages.GROUPS, Args.compose(ACTION_SEARCH, 0, _searchInput.getText()));
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

        _sortBox = new ListBox();
        _sortBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget widget) {
                int sort = SORT_VALUES[((ListBox)widget).getSelectedIndex()];
                // sort is only available with no search/tag action, and resets the page.
                Link.go(Pages.GROUPS, Args.compose("", 0, "", sort + ""));
            }
        });

        _groupGrid = new PagedGrid<GroupCard>(GRID_ROWS, GRID_COLUMNS) {
            protected void displayPageFromClick (int page) {
                // preserve action and args.
                String action = _query.searchString != null ? ACTION_SEARCH
                    : (_query.tag != null ? ACTION_TAG : "");
                String arg = _query.searchString != null ? _query.searchString
                    : (_query.tag != null ? _query.tag : "");
                Link.go(Pages.GROUPS, Args.compose(action, page + "", arg, _query.sort + ""));
            }
            protected Widget createWidget (GroupCard card) {
                return createGroupWidget(card);
            }
            protected String getEmptyMessage () {
                return _msgs.galaxyNoGroups();
            }

            @Override protected void addCustomControls (FlexTable controls) {
                controls.setWidget(
                    0, 0, new InlineLabel(_msgs.galaxySortBy(), false, false, false));
                controls.getFlexCellFormatter().setStyleName(0, 0, "SortBy");
                controls.setWidget(0, 1, _sortBox);
            }
        };
        _groupGrid.addStyleName("GroupsList");
        content.add(_groupGrid);

        // add info on creating a Group
        if (!CShell.isGuest()) {
            SmartTable create = new SmartTable("Create", 0, 0);
            create.setText(0, 0, _msgs.galaxyCreateTitle(), 3, "Header");
            create.setText(1, 0, _msgs.galaxyCreateBlurb(), 1, "Pitch");
            create.setWidget(1, 1, WidgetUtil.makeShim(10, 10));
            ClickListener onClick = Link.createListener(Pages.GROUPS, "edit");
            create.setWidget(1, 2, new Button(_msgs.galaxyCreate(), onClick), 1, "Button");
            add(create);
        }
    }

    /**
     * Called by parent page when the url changes; may be the first time the panel was loaded, or
     * the result of a tag, text search, or page change query.
     */
    public void setArgs (Args args)
    {
        String action = args.get(0, "");
        _query.page = args.get(1, 0);
        String arg = args.get(2, "");
        _query.sort = (byte)args.get(3, 0);
        boolean needCount = false;

        // clear out our status indicators (they'll be put back later)
        _currentTag.clear();
        _searchInput.setText("");

        // Search by tag: group-tag_NN_TAG
        if (action.equals(ACTION_TAG) && !arg.equals("")) {
            InlineLabel tagLabel = new InlineLabel(_msgs.galaxyCurrentTag(arg) + " ");
            tagLabel.addStyleName("Label");
            _currentTag.add(tagLabel);
            _currentTag.add(new InlineLabel("("));
            Widget clear = Link.create(_msgs.galaxyTagClear(), Pages.GROUPS, "");
            clear.addStyleName("inline");
            _currentTag.add(clear);
            _currentTag.add(new InlineLabel(")"));
            if (!arg.equals(_query.tag)) {
                _query.tag = arg;
                needCount = true;
            }
        } else if (_query.tag != null) {
            // clear the tag search
            needCount = true;
            _query.tag = null;
        }

        // Search by full text: group-search_NN_QUERY
        if (action.equals(ACTION_SEARCH) && !arg.equals("")) {
            _searchInput.setText(arg);
            if (!arg.equals(_query.searchString)) {
                _query.searchString = arg;
                needCount = true;
            }
        } else if (_query.searchString != null) {
            // clear the text search
            needCount = true;
            _query.searchString = null;
        }

        // first time loading the page; initialize query and get all page data
        if (_query.count == 0) {
            _query.count = GRID_COLUMNS * GRID_ROWS;
            _groupsvc.getGalaxyData(_query, new MsoyCallback<GalaxyData>() {
                public void onSuccess (GalaxyData galaxy) {
                    init(galaxy);
                }
            });

        // page already initialized, just refresh the grid data
        } else {
            fetchGroupData(needCount);
        }

        // populate the sort box
        setSortbox();
    }

    /**
     * If currently displaying search results, lock the sort box to "By Relevance", otherwise
     * display all search values and select the right one.
     */
    protected void setSortbox ()
    {
        if (_query.searchString != null || _query.tag != null) {
            if (_sortBox.getItemCount() != 1) {
                _sortBox.clear();
                _sortBox.addItem(_msgs.sortByRelevance());
                _sortBox.setEnabled(false);
            }

        } else {
            if (_sortBox.getItemCount() != SORT_LABELS.length) {
                _sortBox.clear();
                for (int ii = 0; ii < SORT_LABELS.length; ii++) {
                    _sortBox.addItem(SORT_LABELS[ii], SORT_VALUES[ii] + "");
                    if (_query.sort == SORT_VALUES[ii]) {
                        _sortBox.setSelectedIndex(ii);
                    }
                }
                _sortBox.setEnabled(true);
            }
        }
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
                _myGroups.add(createMyGroupWidget(group));
            }
            Widget seeAllLink = Link.create(_msgs.galaxyMyGroupsSeeAll(), Pages.GROUPS,
                "mygroups");
            seeAllLink.addStyleName("SeeAll");
            _myGroups.add(seeAllLink);
        }

        // set up our popular tags
        if (data.popularTags.size() == 0) {
            _popularTags.add(MsoyUI.createLabel(_msgs.galaxyNoPopularTags(), "Link"));
        } else {
            for (String tag : data.popularTags) {
                Widget link = Link.create(tag, Pages.GROUPS, Args.compose(ACTION_TAG, 0,
                    tag));
                link.addStyleName("Link");
                link.removeStyleName("inline");
                _popularTags.add(link);
            }
        }

        _totalGroups = data.publicGroups.totalCount;
        _groupGrid.setModel(new GroupDataModel(data.publicGroups.groups), _query.page);
    }

    /**
     * Get group grid data based on the contents of _query, then set the data model.
     */
    protected void fetchGroupData (boolean needCount)
    {
        _groupsvc.getGroups(_query, needCount, new MsoyCallback<GroupsResult>() {
            public void onSuccess (GroupsResult result) {
                if (result.totalCount > 0) {
                    _totalGroups = result.totalCount;
                }
                _groupGrid.setModel(new GroupDataModel(result.groups), _query.page);
            }
        });
    }

    /**
     * A single one of "my" groups on the left column.
     */
    protected Widget createMyGroupWidget (GroupCard group)
    {
        FloatPanel widget = new FloatPanel("MyGroup");
        String goArgs = Args.compose("d", group.name.getGroupId());
        widget.add(new ThumbBox(group.logo, MediaDesc.QUARTER_THUMBNAIL_SIZE, Pages.GROUPS,
            goArgs));
        widget.add(Link.create(group.name.toString(), "GroupName", Pages.GROUPS, goArgs));
        widget.add(Link.create(_msgs.galaxyMyGroupsDiscussions(), "Discussions", Pages.GROUPS,
            Args.compose("f", group.name.getGroupId())));
        return widget;
    }

    /**
     * A single group in the right column grid
     */
    protected Widget createGroupWidget (GroupCard group)
    {
        AbsolutePanel widget = new AbsolutePanel();
        widget.setStyleName("Group");
        String goArgs = Args.compose("d", group.name.getGroupId());
        widget.add(new ThumbBox(group.logo, MediaDesc.HALF_THUMBNAIL_SIZE, Pages.GROUPS, goArgs),
            5, 5);
        widget.add(Link.create(group.name.toString(), "GroupName", Pages.GROUPS, goArgs), 50, 5);
        widget.add(MsoyUI.createLabel(_msgs.galaxyMemberCount(group.memberCount + " "),
            "MemberCount"), 5, 40);
        widget.add(Link.create(_msgs.galaxyPeopleInRooms(group.population + ""), "InRooms",
            Pages.WORLD, "s" + group.homeSceneId), 5, 60);
        widget.add(Link.create(_msgs.galaxyThreadCount(group.threadCount + ""), "ThreadCount",
            Pages.GROUPS, Args.compose("f", group.name.getGroupId())), 5, 80);
        return widget;
    }

    /**
     * Data model for the groups list, which uses _totalGroups as the total count, and returns all
     * data whenever a subset is requested. Page limiting must be done at the repo level.
     */
    protected class GroupDataModel implements DataModel<GroupCard>
    {
        public GroupDataModel (List<GroupCard> groups) {
            _groups = groups;
        }
        public int getItemCount () {
            return _totalGroups;
        }
        public void doFetchRows (int start, int count, AsyncCallback<List<GroupCard>> callback) {
            callback.onSuccess(_groups);
        }
        public void removeItem (GroupCard item) {
            _groups.remove(item);
        }
        protected List<GroupCard> _groups;
    };

    /** The current search,tag,page, and sort being displayed */
    protected GroupQuery _query = new GroupQuery();

    /** Current total number of groups to display in the grid */
    protected int _totalGroups;

    /* dynamic widgets */
    protected PagedGrid<GroupCard> _groupGrid;
    protected FlowPanel _popularTags, _currentTag;
    protected TextBox _searchInput;
    protected FlowPanel _myGroups;
    protected ListBox _sortBox;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)
        ServiceUtil.bind(GWT.create(GroupService.class), GroupService.ENTRY_POINT);

    protected static final String ACTION_SEARCH = "search";
    protected static final String ACTION_TAG = "tag";
    protected static final int GRID_ROWS = 4;
    protected static final int GRID_COLUMNS = 4;

    protected static final String[] SORT_LABELS = new String[] {
        _msgs.sortByNewAndPopular(),
        _msgs.sortByName(),
        _msgs.sortByNumMembers(),
        _msgs.sortByCreatedDate()
    };
    protected static final byte[] SORT_VALUES = new byte[] {
        GroupService.GroupQuery.SORT_BY_NEW_AND_POPULAR,
        GroupService.GroupQuery.SORT_BY_NAME,
        GroupService.GroupQuery.SORT_BY_NUM_MEMBERS,
        GroupService.GroupQuery.SORT_BY_CREATED_DATE,
    };
}
