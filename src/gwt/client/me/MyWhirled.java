//
// $Id$

package client.me;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
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

public class MyWhirled extends SmartTable
{
    public MyWhirled ()
    {
        super("myWhirled", 0, 0);

        CMe.worldsvc.getMyWhirled(CMe.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                MyWhirledData data = (MyWhirledData) result;
                fillUI(data);
            }
        });
    }

    protected void fillUI (MyWhirledData data)
    {
        // set up our sidebar
        VerticalPanel sidebar = new VerticalPanel(), box;
        sidebar.setStyleName("mePanel");
        setWidget(0, 0, sidebar);
        getFlexCellFormatter().setVerticalAlignment(0, 0, VerticalPanel.ALIGN_TOP);

        // add our own profile picture to the left column
        sidebar.add(createHeader(CMe.msgs.headerProfile()));
        sidebar.add(box = createListBox("PictureBox"));
        box.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        MediaDesc photo = (data.photo == null) ? Profile.DEFAULT_PHOTO : data.photo;
        box.add(MediaUtil.createMediaView(photo, MediaDesc.THUMBNAIL_SIZE, new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.PEOPLE, "" + CMe.getMemberId());
            }
        }));

        // add a list of tools
        sidebar.add(createHeader(CMe.msgs.headerTools()));
        sidebar.add(box = createListBox("ListBox"));
        box.add(Application.createLink("My Discussions", Page.WHIRLEDS, "unread"));
        box.add(Application.createLink("My Mail", Page.MAIL, ""));
        box.add(Application.createLink("My Account", Page.ME, "account"));
        if (CMe.isSupport()) {
            box.add(Application.createLink("Admin Console", Page.ADMIN, ""));
        }

        // add all of our rooms
        sidebar.add(createHeader(CMe.msgs.headerRooms()));
        sidebar.add(box = createListBox("ListBox"));

        // first add our home room
        Integer homeId = new Integer(data.homeSceneId);
        box.add(createIconLink("/images/whirled/my_home.png", (String)data.rooms.get(homeId),
                               Page.WORLD, "s" + homeId));

        // next add the remainder of our rooms in purchased order (lowest scene id first)
        Object[] sceneIds = data.rooms.keySet().toArray();
        Arrays.sort(sceneIds);
        for (int ii = 0; ii < sceneIds.length; ii++) {
            if (homeId.equals(sceneIds[ii])) {
                continue;
            }
            String sname = (String)data.rooms.get(sceneIds[ii]);
            box.add(Application.createLink(sname, Page.WORLD, "s" + sceneIds[ii]));
        }

        // set up the main page contents
        VerticalPanel contents = new VerticalPanel();
        contents.setSpacing(10);
        setWidget(0, 1, contents);
        getFlexCellFormatter().setVerticalAlignment(0, 1, VerticalPanel.ALIGN_TOP);

        // display the Whirled population
        contents.add(new Label(CMe.msgs.populationDisplay("" + data.whirledPopulation)));

        // display our online friends if we have any
        if (data.friends.size() > 0) {
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
            FlowPanel ppanel = new FlowPanel();
            ppanel.addStyleName("rightLabel");
            ppanel.add(people);
            Hyperlink link = Application.createLink(
                "All your friends...", Page.PEOPLE, Args.compose("f", CMe.getMemberId()));
            link.addStyleName("tipLabel");
            ppanel.add(link);
            contents.add(MsoyUI.createBox("people", CMe.msgs.headerPeople(), ppanel));
        }

        // add links to our stuff
        SmartTable stuff = new SmartTable();
        stuff.setStyleName("Stuff");
        for (int ii = 0; ii < Item.TYPES.length; ii++) {
            final byte type = Item.TYPES[ii];
            ClickListener onClick = new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.STUFF, "" + type);
                }
            };
            String ipath = Item.getDefaultThumbnailMediaFor(type).getMediaPath();
            stuff.setWidget(0, ii, MsoyUI.createActionImage(ipath, onClick), 1, "Item");
            String ilabel = CMe.dmsgs.getString("pItemType" + type);
            stuff.setWidget(1, ii, MsoyUI.createActionLabel(ilabel, onClick), 1, "Item");
        }
        contents.add(MsoyUI.createBox("aux", CMe.msgs.headerStuff(), stuff));

        // add our news feed
        FeedPanel feed = new FeedPanel();
        feed.setFeed(data.feed, false);
        contents.add(feed);
    }

    protected Widget createHeader (String title)
    {
        HorizontalPanel header = new HorizontalPanel();
        header.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        header.setStyleName("SectionHeaderContainer");
        header.add(MsoyUI.createLabel(title, "SectionHeader"));
        return header;
    }

    protected VerticalPanel createListBox (String styleName)
    {
        VerticalPanel box = new VerticalPanel();
        box.setStyleName(styleName);
        box.addStyleName("borderedBox");
        box.setSpacing(3);
        return box;
    }

    protected Widget createIconLink (String icon, String label, final String page, final String args)
    {
        HorizontalPanel row = new HorizontalPanel();
        Widget image = MsoyUI.createActionImage(icon, new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(page, args);
            }
        });
        image.setWidth("18px");
        row.add(image);
        row.add(WidgetUtil.makeShim(2, 2));
        row.add(Application.createLink(label, page, args));
        return row;
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
