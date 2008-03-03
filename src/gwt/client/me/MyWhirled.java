//
// $Id$

package client.me;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.MyWhirledData;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.StuffNaviBar;
import client.util.TongueBox;

public class MyWhirled extends VerticalPanel
{
    public MyWhirled ()
    {
        setStyleName("myWhirled");

        CMe.worldsvc.getMyWhirled(CMe.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((MyWhirledData)result);
            }
        });
    }

    protected void init (MyWhirledData data)
    {
        add(new StuffNaviBar(Item.NOT_A_TYPE));

        // display the Whirled population
        add(MsoyUI.createLabel(CMe.msgs.populationDisplay("" + data.whirledPopulation), "Pop"));

        // display our online friends if we have any
        if (data.friends != null && data.friends.size() > 0) {
            // sort our friends list alphabetically (hopefully this sort is stable...)
            Collections.sort(data.friends, new Comparator() {
                public int compare (Object o1, Object o2) {
                    MemberCard m1 = (MemberCard) o1, m2 = (MemberCard) o2;
                    return ("" + m1.name).compareTo("" + m2.name);
                }
            });

            SmartTable people = new SmartTable();
            people.setStyleName("Friends");
            for (int ii = 0; ii < data.friends.size(); ii++) {
                MemberCard card = (MemberCard)data.friends.get(ii);
                people.setWidget(ii / PEOPLE_COLUMNS, ii % PEOPLE_COLUMNS, new PersonWidget(card));
            }

            TongueBox fbox = new TongueBox(CMe.msgs.headerPeople(), people);
            fbox.setFooterLink("All your friends...", Page.PEOPLE,
                               Args.compose("f", CMe.getMemberId()));
            add(fbox);
        }

        // add our news feed
        FeedPanel feed = new FeedPanel(
            data.friendCount > 0 ? CMe.msgs.emptyFeed() : CMe.msgs.emptyFeedNoFriends());
        feed.setFeed(data.feed, false);
        add(feed);
    }

    protected static class PersonWidget extends VerticalPanel
    {
        public PersonWidget (final MemberCard card)
        {
            setStyleName("PersonWidget");
            setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);

            ClickListener onClick;
            String where = null;
            if (card.status instanceof MemberCard.InScene) {
                final MemberCard.InScene status = (MemberCard.InScene)card.status;
                onClick = new ClickListener() {
                    public void onClick (Widget sender) {
                        Application.go(Page.WORLD, "s" + status.sceneId);
                    }
                };
                if (status.sceneName != null) {
                    where = CMe.msgs.friendIn(status.sceneName);
                }

            } else if (card.status instanceof MemberCard.InGame) {
                final MemberCard.InGame status = (MemberCard.InGame)card.status;
                onClick = new ClickListener() {
                    public void onClick (Widget sender) {
                        Application.go(Page.WORLD, Args.compose("game", status.gameId));
                    }
                };
                if (status.gameName != null) {
                    where = CMe.msgs.friendIn(status.gameName);
                }

            } else {
                onClick = new ClickListener() {
                    public void onClick (Widget sender) {
                        Application.go(Page.WORLD, "m" + card.name.getMemberId());
                    }
                };
            }

            add(MediaUtil.createMediaView(card.photo, MediaDesc.THUMBNAIL_SIZE, onClick));
            add(MsoyUI.createActionLabel("" + card.name, "NameLabel", onClick));
            if (where != null) {
                add(MsoyUI.createLabel(where, "tipLabel"));
            }
        }
    }

    protected static final int PEOPLE_COLUMNS = 6;
}
