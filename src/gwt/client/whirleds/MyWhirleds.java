//
// $Id: ThreadListPanel.java 9296 2008-05-28 14:51:30Z mdb $

package client.whirleds;

import java.util.Date;
import java.util.List;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;

import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.group.gwt.MyGroupCard;


import client.shell.Args;
import client.shell.Page;

import client.shop.CShop;

import client.util.Link;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.ThumbBox;

/**
 * Displays a list of threads.
 */
public class MyWhirleds extends AbsolutePanel
{
    public MyWhirleds (byte sortMethod)
    {
        setStyleName("myWhirleds");

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
                Link.go(Page.WHIRLEDS, Args.compose("mywhirleds", newSortMethod));
            }
        });

        CWhirleds.groupsvc.getMyGroups(
            CWhirleds.ident, sortMethod, new MsoyCallback<List<MyGroupCard>>() {
                public void onSuccess (List<MyGroupCard> whirleds) {
                    gotData(whirleds);
                }
            });
    }

    /**
     * When data for this page is received, create a new WhirledsGrid to display it.
     */
    protected void gotData (List<MyGroupCard> whirleds)
    {
        WhirledsGrid grid = new WhirledsGrid();
        add(MsoyUI.createSimplePanel("WhirledsGrid", grid));
        grid.setModel(new SimpleDataModel<MyGroupCard>(whirleds), 0);
    }

    /**
     * Displays a list of whirleds in a paged grid.
     */
    protected class WhirledsGrid extends PagedGrid<MyGroupCard>
    {
        public static final int WHIRLEDS_PER_PAGE = 10;

        public WhirledsGrid ()
        {
            super(WHIRLEDS_PER_PAGE, 1);
            setWidth("100%");
        }

        @Override // from PagedGrid
        protected Widget createWidget (MyGroupCard card)
        {
            return new WhirledWidget(card);
        }

        @Override // from PagedGrid
        protected String getEmptyMessage ()
        {
            return CWhirleds.msgs.myNoWhirleds();
        }

        @Override // from PagedGrid
        protected boolean displayNavi (int items)
        {
            return true;
        }

        @Override // from PagedGrid
        protected void addCustomControls (FlexTable controls) {
            controls.setWidget(
                0, 0, new InlineLabel(CShop.msgs.catalogSortBy(), false, false, false));
            controls.getFlexCellFormatter().setStyleName(0, 0, "SortBy");
            controls.setWidget(0, 1, _sortBox);

            // add a second row with table titles
            FlowPanel headers = new FlowPanel();
            headers.setStyleName("Headers");
            controls.setWidget(1, 0, headers);
            controls.getFlexCellFormatter().setColSpan(1, 0, 7);
            headers.add(MsoyUI.createLabel("Whirled Name", "WhirledNameHeader"));
            headers.add(MsoyUI.createLabel("Latest Post", "LatestPostHeader"));
            headers.add(MsoyUI.createLabel("Threads", "ThreadsHeader"));
            headers.add(MsoyUI.createLabel("Posts", "PostsHeader"));
        }

        /**
         * Displays a single whirled
         */
        protected class WhirledWidget extends AbsolutePanel
        {
            public WhirledWidget (MyGroupCard card)
            {
                setStyleName("WhirledWidget");

                // logo links to whirled
                ClickListener whirledClick = Link.createListener(
                    Page.WHIRLEDS, Args.compose("d", card.name.getGroupId()));
                ThumbBox logo = new ThumbBox(card.logo, whirledClick);
                SimplePanel logoBox = new SimplePanel();
                logoBox.setStyleName("LogoBox");
                logoBox.setWidget(logo);
                add(logoBox);

                // name links to whirled
                FlowPanel name = new FlowPanel();
                name.setStyleName("Name");
                Hyperlink nameText = Link.create(
                    card.name.toString(), Page.WHIRLEDS, Args.compose("d", card.name.getGroupId()));
                nameText.addStyleName("inline");
                name.add(nameText);
                // display a star beside name if player is a manager of this group
                if (card.rank == GroupMembership.RANK_MANAGER) {
                    Image managerStar = MsoyUI.createInlineImage("/images/group/manager_star.png");
                    managerStar.setTitle(CWhirleds.msgs.myManagerStarTitle());
                    managerStar.addStyleName("ManagerStar");
                    name.add(managerStar);
                }
                add(name);

                // only show members online if there is at least one
                if (card.population > 0) {
                    Label membersOnline = new Label(CWhirleds.msgs.myMembersOnline(""+card.population));
                    membersOnline.setStyleName("MembersOnline");
                    add(membersOnline);
                }

                // latest thread subject links to thread
                if (card.latestThread != null) {

                    Hyperlink latestThreadSubject = Link.create(
                        card.latestThread.subject,
                        Page.WHIRLEDS, Args.compose("t", card.latestThread.threadId));
                    latestThreadSubject.setStyleName("LatestThreadSubject");
                    add(latestThreadSubject);

                    //MsoyUI.createLabel(text, styleName)
                    FlowPanel postedBy = new FlowPanel();
                    postedBy.setStyleName("PostedBy");
                    add(postedBy);
                    postedBy.add(new InlineLabel(CWhirleds.msgs.myPostedBy()+" "));
                    String memberName = card.latestThread.firstPost.poster.name.toString();
                    int memberId = card.latestThread.firstPost.poster.name.getMemberId();
                    postedBy.add(Link.memberView(memberName, memberId));

                    FlowPanel date = new FlowPanel();
                    date.setStyleName("Date");
                    add(date);
                    Date created = card.latestThread.firstPost.created;
                    Date now = new Date();
                    if(created.getDate() == now.getDate()
                            && created.getMonth() == now.getMonth()
                            && created.getYear() == now.getYear()) {
                        date.add(new InlineLabel(CWhirleds.msgs.myToday()));
                    }
                    else {
                        date.add(new InlineLabel(DATE_FORMAT.format(created) + " "));
                    }
                    InlineLabel time = new InlineLabel(" " + TIME_FORMAT.format(created));
                    time.addStyleName("Time");
                    date.add(time);

                }
                else {
                    HTML noThreads = new HTML("No discussions");
                    noThreads.setStyleName("NoThreads");
                    add(noThreads);
                }

                // #threads and #posts link to discussions
                Hyperlink numThreads = Link.create(
                    card.numThreads+"",
                    Page.WHIRLEDS, Args.compose("f", card.name.getGroupId()));
                numThreads.addStyleName("NumThreads");
                add(numThreads);
                Hyperlink numPosts = Link.create(
                    card.numPosts+"",
                    Page.WHIRLEDS, Args.compose("f", card.name.getGroupId()));
                numPosts.addStyleName("NumPosts");
                add(numPosts);
            }
        }
    }

    protected static final String[] SORT_LABELS = new String[] {
        CWhirleds.msgs.mySortByPeopleOnline(),
        CWhirleds.msgs.mySortByName(),
        CWhirleds.msgs.mySortByManager(),
        CWhirleds.msgs.mySortByNewestPost()
    };
    protected static final byte[] SORT_VALUES = new byte[] {
        MyGroupCard.SORT_BY_PEOPLE_ONLINE,
        MyGroupCard.SORT_BY_NAME,
        MyGroupCard.SORT_BY_MANAGER,
        MyGroupCard.SORT_BY_NEWEST_POST
    };

    /** Dropdown of sort methods */
    protected ListBox _sortBox;

    /** Used to format the most recent post date. */
    protected static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-yy");
    protected static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm aa");
}
