//
// $Id$

package client.whirled;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.DOM;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.person.data.FeedMessage;
import com.threerings.msoy.person.data.FriendFeedMessage;
import com.threerings.msoy.person.data.GroupFeedMessage;

import client.shell.Application;
import client.shell.Args;
import client.shell.CShell;
import client.shell.Page;
import client.util.MsoyUI;
import client.game.GameDetailPanel;

public class FeedPanel extends VerticalPanel
{
    public FeedPanel ()
    {
        buildUi();

        CWhirled.membersvc.loadFeed(CWhirled.ident, DEFAULT_CUTOFF, new AsyncCallback() {
            public void onSuccess (Object result) {
                List messages = (List)result;
                fillUi(messages);
            }
            public void onFailure(Throwable caught) {
                MsoyUI.error(CWhirled.serverError(caught));
            }
        });
    }

    protected void buildUi ()
    {
        setStyleName("FeedContainer");
        HorizontalPanel header = new HorizontalPanel();
        header.setStyleName("Header");
        header.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        Label star = new Label();
        star.setStyleName("HeaderLeft");
        header.add(star);
        Label title = new Label(CWhirled.msgs.headerFeed());
        title.setStyleName("HeaderCenter");
        header.add(title);
        header.setCellWidth(title, "100%");
        star = new Label();
        star.setStyleName("HeaderRight");
        header.add(star);
        add(header);

        add(_feeds = new FeedList());
    }

    protected void fillUi (List messages)
    {
        _feeds.populate(messages);
    }

    protected static class FeedList extends ScrollPanel
    {
        public FeedList ()
        {
            setStyleName("FeedList");
            setAlwaysShowScrollBars(true);
            DOM.setStyleAttribute(getElement(), "overflowX", "hidden");
        }

        public void populate (List messages)
        {
            VerticalPanel feedContainer = new VerticalPanel();
            feedContainer.setStyleName("FeedListContainer");
            feedContainer.setSpacing(0);
            setWidget(feedContainer);

            if (messages.size() == 0) {
                showEmptyEntry(feedContainer);
                return;
            }

            // sort in descending order by posted
            Object[] messageArray = messages.toArray();
            Arrays.sort(messageArray, new Comparator () {
                public int compare (Object o1, Object o2) {
                    if (!(o1 instanceof FeedMessage) || !(o2 instanceof FeedMessage)) {
                        return 0;
                    }

                    return (int)(((FeedMessage)o2).posted - ((FeedMessage)o1).posted);
                }
                public boolean equals (Object obj) {
                    return obj == this;
                }
            });
            messages = Arrays.asList(messageArray);

            Iterator msgIter = messages.iterator();
            while (msgIter.hasNext()) {
                FeedMessage message = (FeedMessage)msgIter.next();
                if (message instanceof FriendFeedMessage) {
                    addFriendMessage((FriendFeedMessage)message, feedContainer);
                } else if (message instanceof GroupFeedMessage) {
                    addGroupMessage((GroupFeedMessage)message, feedContainer);
                } else {
                    addMessage(message, feedContainer);
                }
            }

        }

        protected void addMessage (FeedMessage message, VerticalPanel feedContainer)
        {
        }

        protected void addFriendMessage (FriendFeedMessage message, VerticalPanel feedContainer)
        {
            String friendLink = profileLink(
                    message.friend.toString(), String.valueOf(message.friend.getMemberId()));
            switch (message.type) {
            // FRIEND_ADDED_FRIEND
            case 100:
                feedContainer.add(new BasicWidget(CWhirled.msgs.friendAddedFriend(
                            friendLink, profileLink(message.data[0], message.data[1]))));
                break;

            // FRIEND_UPDATED_ROOM
            case 101:
                feedContainer.add(new BasicWidget(CWhirled.msgs.friendUpdatedRoom(friendLink,
                                Application.createLinkToken(Page.WORLD, "s" + message.data[0]))));
                break;

            // FRIEND_WON_TROPHY
            case 102:
                feedContainer.add(new BasicWidget(CWhirled.msgs.friendWonTrophy(friendLink,
                                Application.createLinkHtml(message.data[0], Page.GAME,
                                    Args.compose(new String[] {
                                        "d", message.data[1], GameDetailPanel.TROPHIES_TAB })))));
                break;

            // FRIEND_LISTED_ITEM
            case 103:
                feedContainer.add(new BasicWidget(CWhirled.msgs.friendListedItem(friendLink,
                                CShell.dmsgs.getString("itemType" + message.data[1]),
                                Application.createLinkHtml(message.data[0], Page.CATALOG,
                                    Args.compose(new String[] {
                                        message.data[1], "i", message.data[2] })))));
                break;
            }

        }

        protected void addGroupMessage (GroupFeedMessage message, VerticalPanel feedContainer)
        {
        }

        protected String profileLink (String name, String id)
        {
            return Application.createLinkHtml(name, Page.PROFILE, id);
        }

        protected void showEmptyEntry (VerticalPanel feedContainer)
        {
            feedContainer.add(new BasicWidget(CWhirled.msgs.emptyFeed(
                            Application.createLinkToken("whirled", "whirledwide"))));
        }

    }

    protected static class BasicWidget extends HorizontalPanel
    {
        public BasicWidget (String html)
        {
            setStyleName("FeedWidget");
            addStyleName("FeedBasic");
            setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
            add(new HTML(html));
        }
    }

    protected static class FeedWidget extends HorizontalPanel
    {
        public FeedWidget (FeedMessage message)
        {
        }
    }

    protected FeedList _feeds;

    /** The default number of days of feed information to show. */
    protected static final int DEFAULT_CUTOFF = 2;
}
