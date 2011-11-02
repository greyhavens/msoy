//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MeServiceAsync;
import com.threerings.msoy.person.gwt.MyWhirledData;
import com.threerings.msoy.web.gwt.Pages;

import client.person.PersonMessages;
import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.PromotionBox;
import client.ui.RoundBox;
import client.util.Link;
import client.util.PageCallback;

public class MyWhirled extends FlowPanel
{
    public MyWhirled ()
    {
        setStyleName("myWhirled");
        add(MsoyUI.createNowLoading());
        _mesvc.getMyWhirled(new PageCallback<MyWhirledData>(this) {
            public void onSuccess (MyWhirledData data) {
                init(data);
            }
        });
    }

    protected void init (final MyWhirledData data)
    {
        RoundBox rbits = new RoundBox(RoundBox.MEDIUM_BLUE);
        rbits.addStyleName("QuickNav");
        rbits.add(MsoyUI.createLabel(
                      _msgs.populationDisplay(""+data.whirledPopulation), null));
        rbits.add(makeQuickLink("My Profile", Pages.PEOPLE, ""+CShell.getMemberId()));
        rbits.add(makeQuickLink("My Transactions", Pages.ME, "transactions"));
        rbits.add(makeQuickLink("My Blocklist", Pages.PEOPLE, "blocklist"));
        rbits.add(makeQuickLink("My Passport", Pages.ME, "passport"));
        rbits.add(makeQuickLink("Invite Friends", Pages.PEOPLE, "invites"));
        rbits.add(makeQuickLink("Share Whirled", Pages.PEOPLE, "invites", "links"));
        rbits.add(makeQuickLink("Contests", Pages.ME, "contests"));

        FlowPanel feedBox = MsoyUI.createFlowPanel("FeedBox");

        FlowPanel titleBar = new FlowPanel();
        titleBar.addStyleName("NewsBar");
        titleBar.add(MsoyUI.createInlineLabel(_msgs.newsTitle(), "NewsTitle"));
        if (!CShell.getEmbedding().isMinimal() && data.updatedThreads > 0) {
            titleBar.add(Link.create(
                _msgs.unreadThreads(""+data.updatedThreads), "NewsLink", Pages.GROUPS, "unread"));
        }
        feedBox.add(titleBar);

        StreamPanel stream = new StreamPanel(data.stream);
        stream.expand();
        feedBox.add(stream);

        // promo and news feed on the left, bits and friends on the right
        HorizontalPanel horiz = new HorizontalPanel();
        horiz.setStyleName("NewsAndFriends");
        horiz.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);

        FlowPanel left = new FlowPanel();
        if (!CShell.isValidated()) {
            FlowPanel warn = MsoyUI.createFlowPanel("MustValidate");
            warn.add(MsoyUI.createHTML(_msgs.meMustValidate(), null));
            warn.add(MsoyUI.createHTML("&nbsp;", null));
            warn.add(Link.create(_msgs.meGoValidate(), Pages.ACCOUNT, "edit"));
            left.add(warn);

        } else if (CShell.isNewbie()) {
            SmartTable newbie = new SmartTable("Newbie", 0, 5);
            int start = Random.nextInt(NEWBIE_SUGS.length);
            for (int col = 0; col < 3; col++) {
                NewbieSuggestion sug = NEWBIE_SUGS[(col+start) % NEWBIE_SUGS.length];
                newbie.setWidget(0, col, Link.createImage(sug.image, null, sug.page, sug.args));
                newbie.setWidget(1, col, Link.create(sug.text, sug.page, sug.args));
            }
            left.add(newbie);

        } else if (data.promos.size() > 0) {
            left.add(new PromotionBox(data.promos));
            left.add(WidgetUtil.makeShim(10, 10));
        }

        left.add(feedBox);
        horiz.add(left);

        FlowPanel right = MsoyUI.createFlowPanel("RightBits");
        right.add(rbits);
        right.add(WidgetUtil.makeShim(10, 10));
        right.add(new MeFriendsPanel(data));
        horiz.add(right);

        clear();
        add(horiz);
    }

    protected Widget makeQuickLink (String label, Pages page, Object... args)
    {
        // TODO: add a little bullet to the left
        return Link.createBlock(label, null, page, args);
    }

    protected static class NewbieSuggestion
    {
        public final String text;
        public final String image;
        public final Pages page;
        public final Object[] args;

        public NewbieSuggestion (String text, String image, Pages page, Object... args) {
            this.text = text;
            this.image = "/images/" + image;
            this.page = page;
            this.args = args;
        }
    }

    protected static final MeMessages _msgs = (MeMessages)GWT.create(MeMessages.class);
    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
    protected static final MeServiceAsync _mesvc = GWT.create(MeService.class);

    protected static final NewbieSuggestion[] NEWBIE_SUGS = {
        new NewbieSuggestion(_msgs.newbieGames(), "people/links/125play.png", Pages.GAMES),
        new NewbieSuggestion(_msgs.newbieHome(), "people/links/125home.jpg", Pages.WORLD, "h"),
        new NewbieSuggestion(_msgs.newbieRooms(), "people/links/125see.png", Pages.WORLD, "tour"),
        new NewbieSuggestion(_msgs.newbieTrophies(), "people/links/125trophies.png", Pages.GAMES),
        new NewbieSuggestion(_msgs.newbieGroups(), "people/links/125tofu.png", Pages.GROUPS),
        new NewbieSuggestion(_msgs.newbieMe(), "people/links/125findme.jpg", Pages.PEOPLE, "me"),
    };
}
