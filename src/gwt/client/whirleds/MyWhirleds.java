//
// $Id: ThreadListPanel.java 9296 2008-05-28 14:51:30Z mdb $

package client.whirleds;

import java.util.List;

import org.gwtwidgets.client.util.SimpleDateFormat;

import client.people.MemberList;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.HeaderBox;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.ThumbBox;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;
import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.MyGroupCard;

/**
 * Displays a list of threads.
 */
public class MyWhirleds extends AbsolutePanel
{
    public MyWhirleds ()
    {
        setStyleName("MyWhirleds");
        CWhirleds.groupsvc.getMyGroups(CWhirleds.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                gotData((List)result);
            }
        });
    }

    /**
     * When data for this page is received, create a new WhirledsGrid to display it.
     */
    protected void gotData (List whirleds)
    {
        WhirledsGrid whirledsGrid = new WhirledsGrid();
        add(new HeaderBox(CWhirleds.msgs.myWhirledsTitle(), whirledsGrid));
        whirledsGrid.setModel(new SimpleDataModel(whirleds), 0);
    }
    
    /**
     * Displays a list of whirleds in a paged grid.
     */
    protected class WhirledsGrid extends PagedGrid
    {
        public static final int WHIRLEDS_PER_PAGE = 10;

        public WhirledsGrid () 
        {
            super(WHIRLEDS_PER_PAGE, 1, MemberList.NAV_ON_BOTTOM);
            setWidth("100%");
            addStyleName("dottedGrid");
        }

        // @Override // from PagedGrid
        protected Widget createWidget (Object item)
        {
            return new WhirledWidget((MyGroupCard) item);
        }

        // @Override // from PagedGrid
        protected String getEmptyMessage ()
        {
            return "You don't belong to any whirleds!";
        }

        // @Override // from PagedGrid
        protected boolean displayNavi (int items)
        {
            return true;
        }

        /**
         * Displays a single whirled
         */
        protected class WhirledWidget extends AbsolutePanel
        {
            public WhirledWidget (MyGroupCard card) 
            {
                setStyleName("whirledWidget");

                // logo links to whirled
                ClickListener whirledClick = Application.createLinkListener(
                    Page.WHIRLEDS, Args.compose("d", card.name.getGroupId()));
                ThumbBox logo = new ThumbBox(card.logo, whirledClick);
                SimplePanel logoBox = new SimplePanel();
                logoBox.setStyleName("LogoBox");
                logoBox.setWidget(logo);
                add(logoBox);
                
                // name links to whirled
                Hyperlink name = Application.createLink(
                    card.name.toString(), Page.WHIRLEDS, Args.compose("d", card.name.getGroupId()));
                name.setStyleName("Name");
                add(name);
                
                // only show members online if there is at least one
                if (card.population > 0) {
                    Label membersOnline = new Label(CWhirleds.msgs.myMembersOnline(""+card.population));
                    membersOnline.setStyleName("MembersOnline");
                    add(membersOnline);
                }
                
                // # of unread threads links to discussions for this group
                Hyperlink unreadThreads = Application.createLink(
                    CWhirleds.msgs.myUnreadThreads(""+card.numUnreadThreads), 
                    Page.WHIRLEDS, Args.compose("f", card.name.getGroupId()));
                unreadThreads.setStyleName("UnreadThreads");
                add(unreadThreads);

                // latest thread subject links to thread
                if (card.latestThread != null) {
                    Label latestThread = new Label(CWhirleds.msgs.myLatestThread());
                    latestThread.setStyleName("LatestThread");
                    add(latestThread);
                    
                    Hyperlink latestThreadSubject = Application.createLink(
                        card.latestThread.subject, 
                        Page.WHIRLEDS, Args.compose("t", card.latestThread.threadId));
                    latestThreadSubject.setStyleName("LatestThreadSubject");
                    add(latestThreadSubject);

                    // posted by <a href="#people-{ID}">{NAME}</a> on {DATE}
                    HTML postedBy = new HTML(CWhirleds.msgs.myPostedOn(
                        ""+card.latestThread.firstPost.poster.name.getMemberId(),
                        card.latestThread.firstPost.poster.name.toString(), 
                        DATE_FORMAT.format(card.latestThread.firstPost.created)));
                    postedBy.setStyleName("PostedBy");
                    add(postedBy);
                }
                else {
                    HTML noThreads = new HTML("No discussions");
                    noThreads.setStyleName("NoThreads");
                    add(noThreads);
                }
                
                // discussions button goes to discussions
                Hyperlink discussions = Application.createLink(
                    CWhirleds.msgs.myDiscussions(), 
                    Page.WHIRLEDS, Args.compose("f", card.name.getGroupId()));
                discussions.setStyleName("Discussions");
                add(discussions);
                
                // display a star if player is a manager of this group
                if (card.rank == GroupMembership.RANK_MANAGER) {
                    Image managerStar = MsoyUI.createImage("/images/whirled/games_star.png", "ManagerStar");
                    managerStar.setTitle(CWhirleds.msgs.myManagerStarTitle());
                    add(managerStar);
                }

            }
        }
    }
    
    /** Used to format the most recent post date. */
    protected static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEEE, MMMMM dd, yyyy");
}
