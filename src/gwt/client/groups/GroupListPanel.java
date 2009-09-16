//
// $Id$

package client.groups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.PagedResult;
import com.threerings.gwt.util.StringUtil;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MiniNowLoadingWidget;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.Link;
import client.util.MsoyPagedServiceDataModel;

/**
 * Displays a list of groups that meet some criteria.
 */
public class GroupListPanel extends FlowPanel
{
    public GroupListPanel ()
    {
        setStyleName("groupList");

        SmartTable navi = new SmartTable("Navi", 0, 5);
        navi.setWidget(0, 0, _categoryLinks = new CategoryLinks());
        navi.setWidget(0, 1, _searchInput = MsoyUI.createTextBox("", 50, 10));
        ClickHandler doSearch = new ClickHandler() {
            public void onClick (ClickEvent event) {
                Link.go(Pages.GROUPS, ACTION_SEARCH, 0, _searchInput.getText().trim());
            }
        };
        _searchInput.addKeyPressHandler(new EnterClickAdapter(doSearch));
        navi.setWidget(0, 2, MsoyUI.createImageButton("GoButton", doSearch));
        add(navi);

        _sortBox = new ListBox();
        _sortBox.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent event) {
                int sort = SORT_VALUES[((ListBox)event.getSource()).getSelectedIndex()];
                // sort is only available with no search/tag action, and resets the page
                Link.go(Pages.GROUPS, ACTION_LIST, 0, "", sort);
            }
        });

        add(_groupGrid = new PagedGrid<GroupCard>(PAGE_ROWS, 1) {
            @Override protected void displayPageFromClick (int page) {
                if (!StringUtil.isBlank(_query.search)) {
                    Link.go(Pages.GROUPS, ACTION_SEARCH, page, _query.search, _query.sort);
                } else if (!StringUtil.isBlank(_query.tag)) {
                    Link.go(Pages.GROUPS, ACTION_TAG, page, _query.tag, _query.sort);
                } else {
                    Link.go(Pages.GROUPS, "list", page, "", _query.sort);
                }
            }
            @Override protected Widget createWidget (GroupCard card) {
                return createGroupWidget(card);
            }
            @Override protected String getEmptyMessage () {
                return _msgs.galaxyNoGroups();
            }
            @Override protected boolean displayNavi (int items) {
                return true;
            }
            @Override protected Widget getNowLoadingWidget () {
                return new MiniNowLoadingWidget();
            }
            @Override protected void addCustomControls (FlexTable controls) {
                controls.setWidget(
                    0, 0, new InlineLabel(_msgs.galaxySortBy(), false, false, false));
                controls.getFlexCellFormatter().setStyleName(0, 0, "SortBy");
                controls.setWidget(0, 1, _sortBox);
            }
        });
        _groupGrid.addStyleName("dottedGrid");
    }

    public void setArgs (Args args)
    {
        GroupService.GroupQuery query = new GroupService.GroupQuery();
        String action = args.get(0, ACTION_LIST);
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

        _categoryLinks.setTag(query.tag);

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

        // If the query has changed, instantiate a new data model for the group grid.
        if (_query == null || !_query.equals(query)) {
            _query = query;
            _groupGrid.setModel(
                new MsoyPagedServiceDataModel<GroupCard, PagedResult<GroupCard>>() {
                    protected void callFetchService (
                        int start, int count, boolean needCount,
                        AsyncCallback<PagedResult<GroupCard>> callback) {
                        _groupsvc.getGroups(start, count, _query, needCount, callback);
                    }
                }, page);
        } else {
            _groupGrid.displayPage(page, false);
        }
    }

    /**
     * A single group in the right column grid
     */
    protected Widget createGroupWidget (GroupCard group)
    {
        int groupId = group.name.getGroupId();

        SmartTable card = new SmartTable("Group", 0, 5);
        card.setWidget(0, 0, new ThumbBox(group.getLogo(), MediaDesc.HALF_THUMBNAIL_SIZE,
                                          Pages.GROUPS, "d", groupId), 1, "Thumb");
        card.getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);

        card.setWidget(0, 1, Link.create(group.name.toString(),
                                         Pages.GROUPS, "d", groupId), 1, "Name");
        card.setText(0, 2, _msgs.galaxyMemberCount(""+group.memberCount));

        card.setWidget(1, 0, MsoyUI.createLabel(group.blurb, "Blurb"));
        card.getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_TOP);
        card.setWidget(1, 1, Link.create(_msgs.galaxyPeopleInRooms(""+group.population),
                                         Pages.WORLD, "s" + group.homeSceneId));

        card.setWidget(2, 0, Link.create(_msgs.galaxyDiscussions(), Pages.GROUPS, "f", groupId));

        card.getFlexCellFormatter().setRowSpan(0, 0, card.getRowCount());
        card.getFlexCellFormatter().setRowSpan(1, 0, card.getRowCount()-1);

        return card;
    }

    protected CategoryLinks _categoryLinks;
    protected TextBox _searchInput;
    protected ListBox _sortBox;
    protected PagedGrid<GroupCard> _groupGrid;

    protected GroupService.GroupQuery _query;
    
    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = GWT.create(GroupService.class);

    protected static final int PAGE_ROWS = 15;

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

    protected static final String ACTION_LIST = "list";
    protected static final String ACTION_SEARCH = "search";
    protected static final String ACTION_TAG = "tag";
}
