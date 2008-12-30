//
// $Id$

package client.groups;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
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
import com.threerings.gwt.ui.InlinePanel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.group.gwt.GalaxyData;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.group.gwt.MyGroupCard;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.MiniNowLoadingWidget;
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
        search.add(MsoyUI.createLabel(_msgs.galaxySearchTitle(), "SearchTitle"));
        search.add(_searchInput = MsoyUI.createTextBox("", 255, 20));
        ClickListener doSearch = new ClickListener() {
            public void onClick (Widget sender) {
                if (_searchInput.getText().equals("")) {
                    Link.go(Pages.GROUPS, "");
                } else {
                    Link.go(Pages.GROUPS, Args.compose(ACTION_SEARCH, 0, _searchInput.getText()));
                }
            }
        };
        _searchInput.addKeyboardListener(new EnterClickAdapter(doSearch));
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
        _categoryLinks = MsoyUI.createFlowPanel("CategoryLinks");
        add(_categoryLinks);

        FloatPanel content = new FloatPanel("Content");
        add(content);

        FlowPanel leftColumn = MsoyUI.createFlowPanel("LeftColumn");

        _myGroups = MsoyUI.createFlowPanel("QuickGroups");
        if (!CShell.isGuest()) {
            leftColumn.add(MsoyUI.createLabel(_msgs.galaxyMyGroupsTitle(), "MyGroupsHeader"));
            leftColumn.add(_myGroups = MsoyUI.createFlowPanel("QuickGroups"));
            _myGroups.addStyleName("MyGroups");
        }

        leftColumn.add(MsoyUI.createLabel(_msgs.galaxyOfficialGroupsTitle(),
            "OfficialGroupsHeader"));
        leftColumn.add(_officialGroups = MsoyUI.createFlowPanel("QuickGroups"));
        _officialGroups.addStyleName("OfficialGroups");
        content.add(leftColumn);

        _sortBox = new ListBox();
        _sortBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget widget) {
                int sort = SORT_VALUES[((ListBox)widget).getSelectedIndex()];
                // sort is only available with no search/tag action, and resets the page.
                Link.go(Pages.GROUPS, Args.compose("", 0, "", sort));
            }
        });

        _groupGrid = new PagedGrid<GroupCard>(GRID_ROWS, GRID_COLUMNS) {
            protected void displayPageFromClick (int page) {
                // preserve action and args.
                String action = _query.search != null ? ACTION_SEARCH
                    : (_query.tag != null ? ACTION_TAG : "");
                String arg = _query.search != null ? _query.search
                    : (_query.tag != null ? _query.tag : "");
                Link.go(Pages.GROUPS, Args.compose(action, page, arg, _query.sort));
            }
            protected Widget createWidget (GroupCard card) {
                return createGroupWidget(card);
            }
            protected String getEmptyMessage () {
                return _msgs.galaxyNoGroups();
            }
            @Override // from PagedWidget
            protected Widget getNowLoadingWidget () {
                return new MiniNowLoadingWidget();
            }
            @Override // from PagedWidget
            protected void addCustomControls (FlexTable controls) {
                controls.setWidget(
                    0, 0, new InlineLabel(_msgs.galaxySortBy(), false, false, false));
                controls.getFlexCellFormatter().setStyleName(0, 0, "SortBy");
                controls.setWidget(0, 1, _sortBox);
            }
        };
        _groupGrid.addStyleName("GroupsList");
        content.add(_groupGrid);

        _groupsvc.getGalaxyData(new MsoyCallback<GalaxyData>() {
            public void onSuccess (GalaxyData galaxy) {
                init(galaxy);
            }
        });
        _nowLoading1 = new MiniNowLoadingWidget();
        _myGroups.add(_nowLoading1);
        _nowLoading2 = new MiniNowLoadingWidget();
        _officialGroups.add(_nowLoading2);
    }

    /**
     * Called by parent page when the url changes; may be the first time the panel was loaded, or
     * the result of a tag, text search, or page change query.
     */
    public void setArgs (Args args)
    {
        // Create a new GroupQuery based on the args
        GroupService.GroupQuery query = new GroupService.GroupQuery();
        String action = args.get(0, "");
        int page = args.get(1, 0);
        String arg = args.get(2, "");
        query.sort = (byte)args.get(3, 0);
        if (action.equals(ACTION_SEARCH) && !arg.equals("")) {
            query.search = arg;
        } else if (action.equals(ACTION_TAG) && !arg.equals("")) {
            query.tag = arg;
        }

        // set the current tag and search text for the new query
        _searchInput.setText("");
        if (query.search != null) {
            _searchInput.setText(arg);
        }

        _categoryLinks.clear();
        _categoryLinks.add(MsoyUI.createLabel(_msgs.galaxyCategoryTitle(), "CategoryTitle inline"));
        for (String tag : CATEGORY_TAGS) {
            String tagStyle = "Link";
            if (query.tag != null && query.tag.equals(tag.toLowerCase())) {
                tagStyle = "SelectedLink";
            }
            _categoryLinks.add(Link.create(tag, tagStyle, Pages.GROUPS, Args.compose(ACTION_TAG,
                0, tag.toLowerCase(), false)));
        }
        _categoryLinks.add(Link.create(_msgs.galaxyCategoryAll(), "Link", Pages.GROUPS, ""));


        // If currently displaying search results, lock the sort box to "By Relevance", otherwise
        // display all search values and select the right one.
        if (query.search != null || query.tag != null) {
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
                    if (query.sort == SORT_VALUES[ii]) {
                        _sortBox.setSelectedIndex(ii);
                    }
                }
                _sortBox.setEnabled(true);
            }
        }

        // If the query has changed, instanciate a new data model for the group grid.
        if (_query == null || !_query.equals(query)) {
            _query = query;
            _groupGrid.setModel(new GroupDataModel(), page);
        } else {
            _groupGrid.displayPage(page, false);
        }
    }

    /**
     * Called when my groups & tags data is retrieved after this panel is created; populate the
     * page with data.
     */
    protected void init (final GalaxyData data)
    {
        _myGroups.remove(_nowLoading1);
        _officialGroups.remove(_nowLoading2);
        
        // set up my groups
        if (!CShell.isGuest() && data.myGroups.size() == 0) {
            _myGroups.add(MsoyUI.createLabel(_msgs.galaxyMyGroupsNone(), "NoGroups"));
        } else {
            for (MyGroupCard group : data.myGroups) {
                _myGroups.add(createQuickGroupWidget(group));
            }
            Widget seeAllLink = Link.create(_msgs.galaxyMyGroupsSeeAll(), Pages.GROUPS,
                "mygroups");
            seeAllLink.addStyleName("SeeAll");
            _myGroups.add(seeAllLink);
        }

        // set up official groups
        for (GroupCard group : data.officialGroups) {
            _officialGroups.add(createQuickGroupWidget(group));
        }
    }

    /**
     * A single one of "my" groups on the left column.
     */
    protected Widget createQuickGroupWidget (GroupCard group)
    {
        FloatPanel widget = new FloatPanel("Group");
        String goArgs = Args.compose("d", group.name.getGroupId());
        widget.add(new ThumbBox(group.getLogo(), MediaDesc.QUARTER_THUMBNAIL_SIZE, Pages.GROUPS,
            goArgs));
        widget.add(Link.create(group.name.toString(), "Name", Pages.GROUPS, goArgs));
        widget.add(Link.create(_msgs.galaxyMyGroupsDiscussions(), "Discussions", Pages.GROUPS,
            Args.compose("f", group.name.getGroupId())));
        return widget;
    }

    /**
     * A single group in the right column grid
     */
    protected Widget createGroupWidget (GroupCard group)
    {
        FlowPanel widget = MsoyUI.createFlowPanel("Group");

        FloatPanel logoAndName = new FloatPanel("LogoAndName");
        String goArgs = Args.compose("d", group.name.getGroupId());
        logoAndName.add(new ThumbBox(group.getLogo(), MediaDesc.HALF_THUMBNAIL_SIZE, Pages.GROUPS,
            goArgs));
        logoAndName.add(Link.create(group.name.toString(), "GroupName", Pages.GROUPS, goArgs));

        widget.add(logoAndName);
        widget.add(MsoyUI.createLabel(_msgs.galaxyMemberCount(group.memberCount + " "),
            "MemberCount"));
        widget.add(Link.create(_msgs.galaxyPeopleInRooms(group.population + ""), "InRooms",
            Pages.WORLD, "s" + group.homeSceneId));
        widget.add(Link.create(_msgs.galaxyThreadCount(group.threadCount + ""), "ThreadCount",
            Pages.GROUPS, Args.compose("f", group.name.getGroupId())));
        return widget;
    }

    /**
     * Data model for the groups list, which uses _totalGroups as the total count, and returns all
     * data whenever a subset is requested. Page limiting must be done at the repo level.
     */
    protected class GroupDataModel
        extends ServiceBackedDataModel<GroupCard, GroupService.GroupsResult>
    {
        protected void callFetchService (int start, int count, boolean needCount,
            AsyncCallback<GroupService.GroupsResult> callback) {
            _groupsvc.getGroups(start, count, _query, needCount, callback);
        }
        protected int getCount (GroupService.GroupsResult result) {
            return result.totalCount;
        }
        protected List<GroupCard> getRows (GroupService.GroupsResult result) {
            return result.groups;
        }
    };

    /** The current search,tag,page, and sort being displayed */
    protected GroupService.GroupQuery _query;

    /* dynamic widgets */
    protected PagedGrid<GroupCard> _groupGrid;
    protected TextBox _searchInput;
    protected FlowPanel _myGroups;
    protected FlowPanel _officialGroups;
    protected ListBox _sortBox;
    protected FlowPanel _categoryLinks;
    protected MiniNowLoadingWidget _nowLoading1, _nowLoading2;
    
    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)
        ServiceUtil.bind(GWT.create(GroupService.class), GroupService.ENTRY_POINT);

    protected static final String ACTION_SEARCH = "search";
    protected static final String ACTION_TAG = "tag";
    protected static final int GRID_ROWS = 4;
    protected static final int GRID_COLUMNS = 4;

    protected static final String[] SORT_LABELS = {
        _msgs.sortByNewAndPopular(),
        _msgs.sortByName(),
        _msgs.sortByNumMembers(),
        _msgs.sortByCreatedDate()
    };
    protected static final byte[] SORT_VALUES = {
        GroupService.GroupQuery.SORT_BY_NEW_AND_POPULAR,
        GroupService.GroupQuery.SORT_BY_NAME,
        GroupService.GroupQuery.SORT_BY_NUM_MEMBERS,
        GroupService.GroupQuery.SORT_BY_CREATED_DATE,
    };

    protected static final String[] CATEGORY_TAGS = { "Games", "Music", "Dance", "Art", "Flash",
        "Fashion", "Pets", "Sports", "Humor" };
}
