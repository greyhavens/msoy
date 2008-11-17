//
// $Id$

package client.groups;

import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.group.gwt.MyGroupCard;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.DateUtil;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays a list of threads.
 */
public class MyGroups extends AbsolutePanel
{
    public MyGroups (byte sortMethod)
    {
        setStyleName("myGroups");

        _sortBox = new ListBox();
        for (int ii = 0; ii < SORT_LABELS.length; ii ++) {
            _sortBox.addItem(SORT_LABELS[ii]);
            if (SORT_VALUES[ii] == sortMethod) {
                _sortBox.setSelectedIndex(ii);
            }
        }
        _sortBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget widget) {
                byte newSortMethod = SORT_VALUES[((ListBox)widget).getSelectedIndex()];
                Link.go(Pages.GROUPS, Args.compose("mygroups", newSortMethod));
            }
        });

        _groupsvc.getMyGroups(sortMethod, new MsoyCallback<List<MyGroupCard>>() {
            public void onSuccess (List<MyGroupCard> groups) {
                gotData(groups);
            }
        });
    }

    /**
     * When data for this page is received, create a new GroupsGrid to display it.
     */
    protected void gotData (List<MyGroupCard> groups)
    {
        GroupsGrid grid = new GroupsGrid();
        add(MsoyUI.createSimplePanel(grid, "GroupsGrid"));
        grid.setModel(new SimpleDataModel<MyGroupCard>(groups), 0);
    }

    /**
     * Displays a list of groups in a paged grid.
     */
    protected class GroupsGrid extends PagedGrid<MyGroupCard>
    {
        public static final int GROUPS_PER_PAGE = 10;

        public GroupsGrid ()
        {
            super(GROUPS_PER_PAGE, 1);
            setWidth("100%");
        }

        @Override // from PagedGrid
        protected Widget createWidget (MyGroupCard card)
        {
            return new GroupWidget(card);
        }

        @Override // from PagedGrid
        protected String getEmptyMessage ()
        {
            return _msgs.myNoGroups();
        }

        @Override // from PagedGrid
        protected boolean displayNavi (int items)
        {
            return true;
        }

        @Override // from PagedGrid
        protected void addCustomControls (FlexTable controls) {
            controls.setWidget(0, 0, new InlineLabel(_msgs.mySortBy(), false, false, false));
            controls.getFlexCellFormatter().setStyleName(0, 0, "SortBy");
            controls.setWidget(0, 1, _sortBox);

            // add a second row with table titles
            FlowPanel headers = new FlowPanel();
            headers.setStyleName("Headers");
            controls.setWidget(1, 0, headers);
            controls.getFlexCellFormatter().setColSpan(1, 0, 7);
            headers.add(MsoyUI.createLabel(_msgs.myHeaderName(), "GroupNameHeader"));
            headers.add(MsoyUI.createLabel(_msgs.myHeaderLatest(), "LatestPostHeader"));
            headers.add(MsoyUI.createLabel(_msgs.myHeaderThreadCount(), "ThreadsHeader"));
            headers.add(MsoyUI.createLabel(_msgs.myHeaderPostCount(), "PostsHeader"));
        }

        /**
         * Displays a single group
         */
        protected class GroupWidget extends AbsolutePanel
        {
            public GroupWidget (MyGroupCard card)
            {
                setStyleName("GroupWidget");

                // logo links to group
                SimplePanel logoBox = new SimplePanel();
                logoBox.setStyleName("LogoBox");
                logoBox.setWidget(new ThumbBox(card.logo, Pages.GROUPS,
                                               Args.compose("d", card.name.getGroupId())));
                add(logoBox);

                // name links to group
                FlowPanel name = new FlowPanel();
                name.setStyleName("Name");
                Widget nameText = Link.create(
                    card.name.toString(), Pages.GROUPS, Args.compose("d", card.name.getGroupId()));
                nameText.addStyleName("inline");
                name.add(nameText);
                // display a star beside name if player is a manager of this group
                if (card.rank == GroupMembership.RANK_MANAGER) {
                    Image managerStar = MsoyUI.createInlineImage("/images/group/manager_star.png");
                    managerStar.setTitle(_msgs.myManagerStarTitle());
                    managerStar.addStyleName("ManagerStar");
                    name.add(managerStar);
                }
                add(name);

                // only show members online if there is at least one
                if (card.population > 0) {
                    Label membersOnline = new Label(
                        _msgs.myMembersOnline(""+card.population));
                    membersOnline.setStyleName("MembersOnline");
                    add(membersOnline);
                }

                // latest thread subject links to thread
                if (card.latestThread != null) {

                    Widget latestThreadSubject = Link.create(
                        card.latestThread.subject, Pages.GROUPS,
                        Args.compose("t", card.latestThread.threadId));
                    latestThreadSubject.setStyleName("LatestThreadSubject");
                    add(latestThreadSubject);

                    //MsoyUI.createLabel(text, styleName)
                    FlowPanel postedBy = new FlowPanel();
                    postedBy.setStyleName("PostedBy");
                    add(postedBy);
                    postedBy.add(new InlineLabel(_msgs.myPostedBy()+" "));
                    String memberName = card.latestThread.firstPost.poster.name.toString();
                    int memberId = card.latestThread.firstPost.poster.name.getMemberId();
                    postedBy.add(Link.memberView(memberName, memberId));

                    FlowPanel date = new FlowPanel();
                    date.setStyleName("Date");
                    add(date);
                    Date created = card.latestThread.firstPost.created;
                    Date now = new Date();
                    if (DateUtil.getDayOfMonth(created) == DateUtil.getDayOfMonth(now) &&
                        DateUtil.getMonth(created) == DateUtil.getMonth(now) &&
                        DateUtil.getYear(created) == DateUtil.getYear(now)) {
                        date.add(new InlineLabel(_msgs.myToday()));
                    } else {
                        date.add(new InlineLabel(MsoyUI.formatDate(created) + " "));
                    }
                    InlineLabel time = new InlineLabel(" " + MsoyUI.formatTime(created));
                    time.addStyleName("Time");
                    date.add(time);

                } else {
                    add(MsoyUI.createHTML("No discussions.", "NoThreads"));
                }

                // #threads and #posts link to discussions
                Widget numThreads = Link.create(
                    card.numThreads+"", Pages.GROUPS, Args.compose("f", card.name.getGroupId()));
                numThreads.addStyleName("NumThreads");
                add(numThreads);
                Widget numPosts = Link.create(
                    card.numPosts+"", Pages.GROUPS, Args.compose("f", card.name.getGroupId()));
                numPosts.addStyleName("NumPosts");
                add(numPosts);
            }
        }
    }

    /** Dropdown of sort methods */
    protected ListBox _sortBox;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)
        ServiceUtil.bind(GWT.create(GroupService.class), GroupService.ENTRY_POINT);

    protected static final String[] SORT_LABELS = new String[] {
        _msgs.mySortByPeopleOnline(),
        _msgs.mySortByName(),
        _msgs.mySortByManager(),
        _msgs.mySortByNewestPost()
    };
    protected static final byte[] SORT_VALUES = new byte[] {
        MyGroupCard.SORT_BY_PEOPLE_ONLINE,
        MyGroupCard.SORT_BY_NAME,
        MyGroupCard.SORT_BY_MANAGER,
        MyGroupCard.SORT_BY_NEWEST_POST
    };
}
