//
// $Id$

package client.whirled;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.web.data.OnlineMemberCard;
import com.threerings.msoy.web.data.MyWhirledData;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;

public class MyWhirled extends SmartTable
{
    public MyWhirled (final PopulationDisplay popDisplay)
    {
        super("myWhirled", 0, 0);

        CWhirled.worldsvc.getMyWhirled(CWhirled.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                MyWhirledData data = (MyWhirledData) result;
//                 popDisplay.displayPopulation(data.whirledPopulation);
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
        sidebar.add(createHeader(CWhirled.msgs.headerProfile()));
        sidebar.add(box = createListBox("PictureBox"));
        box.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        MediaDesc photo = (data.photo == null) ? Profile.DEFAULT_PHOTO : data.photo;
        box.add(MediaUtil.createMediaView(photo, MediaDesc.THUMBNAIL_SIZE, new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.PROFILE, "" + CWhirled.getMemberId());
            }
        }));

        // add a list of tools
        sidebar.add(createHeader(CWhirled.msgs.headerTools()));
        sidebar.add(box = createListBox("ListBox"));
        box.add(Application.createLink("My Discussions", Page.GROUP, "unread"));
        box.add(Application.createLink("My Mail", Page.MAIL, ""));
        box.add(Application.createLink("My Account", Page.ACCOUNT, "edit"));
        if (CWhirled.isSupport()) {
            box.add(Application.createLink("Admin Console", Page.ADMIN, ""));
        }

        // add all of our rooms
        sidebar.add(createHeader(CWhirled.msgs.headerRooms()));
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

        // display our online friends if we have any
        if (data.friends.size() > 0) {
            // sort our friends list alphabetically and map them by id
            Collections.sort(data.friends, new Comparator() {
                public int compare (Object o1, Object o2) {
                    if (!(o1 instanceof OnlineMemberCard) || !(o2 instanceof OnlineMemberCard)) {
                        return 0;
                    }
                    OnlineMemberCard m1 = (OnlineMemberCard) o1;
                    OnlineMemberCard m2 = (OnlineMemberCard) o2;
                    return ("" + m1.name).compareTo("" + m2.name);
                }
                public boolean equals (Object obj) {
                    return obj == this;
                }
            });

            SmartTable people = new SmartTable();
            people.setStyleName("Friends");
            for (int ii = 0; ii < data.friends.size(); ii++) {
                OnlineMemberCard card = (OnlineMemberCard)data.friends.get(ii);
                people.setWidget(ii % PEOPLE_COLUMNS, ii % PEOPLE_COLUMNS, new PersonWidget(card));
            }
            FlowPanel ppanel = new FlowPanel();
            ppanel.addStyleName("rightLabel");
            ppanel.add(people);
            Hyperlink link = Application.createLink(
                "All your friends...", Page.PROFILE, Args.compose("f", CWhirled.getMemberId()));
            link.addStyleName("tipLabel");
            ppanel.add(link);
            contents.add(MsoyUI.createBox("people", CWhirled.msgs.headerPeople(), ppanel));
        }

        // add links to our stuff
        SmartTable stuff = new SmartTable();
        stuff.setStyleName("Stuff");
        for (int ii = 0; ii < Item.TYPES.length; ii++) {
            final byte type = Item.TYPES[ii];
            ClickListener onClick = new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.INVENTORY, "" + type);
                }
            };
            String ipath = Item.getDefaultThumbnailMediaFor(type).getMediaPath();
            stuff.setWidget(0, ii, MsoyUI.createActionImage(ipath, onClick), 1, "Item");
            String ilabel = CWhirled.dmsgs.getString("pItemType" + type);
            stuff.setWidget(1, ii, MsoyUI.createActionLabel(ilabel, onClick), 1, "Item");
        }
        contents.add(MsoyUI.createBox("aux", CWhirled.msgs.headerStuff(), stuff));

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
        public PersonWidget (final OnlineMemberCard card)
        {
            setStyleName("PersonWidget");
            setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);

            ClickListener onClick;
            String where = null;
            switch (card.placeType) {
            case OnlineMemberCard.ROOM_PLACE:
                onClick = new ClickListener() {
                    public void onClick (Widget sender) {
                        Application.go(Page.WORLD, "s" + card.placeId);
                    }
                };
                if (card.placeName != null) {
                    where = CWhirled.msgs.friendIn(card.placeName);
                }
                break;

            case OnlineMemberCard.GAME_PLACE:
                onClick = new ClickListener() {
                    public void onClick (Widget sender) {
                        Application.go(Page.WORLD, Args.compose("game", card.placeId));
                    }
                };
                if (card.placeName != null) {
                    where = CWhirled.msgs.friendPlaying(card.placeName);
                }
                break;

            default:
                onClick = new ClickListener() {
                    public void onClick (Widget sender) {
                        Application.go(Page.WORLD, "m" + card.name.getMemberId());
                    }
                };
                break;
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
