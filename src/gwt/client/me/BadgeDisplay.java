//
// $Id$

package client.me;

import java.util.Date;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.badge.data.all.BadgeCodes;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;

import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.Link;

class BadgeDisplay extends FlowPanel
{
    public BadgeDisplay (Badge badge)
    {
        setStyleName("badgeDisplay");

        buildBasics(badge);
        if (badge instanceof EarnedBadge) {
            addEarnedBits((EarnedBadge)badge);
        } else if (badge instanceof InProgressBadge) {
            addInProgressBits((InProgressBadge)badge);
        }
    }

    protected void buildBasics (Badge badge)
    {
        add(MsoyUI.createImage(badge.imageUrl(), "StampImage"));

        String hexCode = Integer.toHexString(badge.badgeCode);
        String badgeName = _dmsgs.get("badge_" + hexCode, Badge.getLevelName(badge.level));
        add(MsoyUI.createLabel(badgeName, "StampName"));

        // first look for a specific message for this level
        String badgeDesc = _dmsgs.xlate("badgeDesc" + badge.level + "_" + hexCode);
        // TODO: unhack this by adding contains() to MessagesLookup
        if (badgeDesc.startsWith("Invalid key")) {
            badgeDesc = _dmsgs.get("badgeDescN_" + hexCode, badge.levelUnits);
        }
        add(MsoyUI.createHTML(badgeDesc, "StampDescription"));
    }

    protected void addEarnedBits (EarnedBadge badge)
    {
        Date earnedDate = new Date(badge.whenEarned);
        String whenEarned = _msgs.passportFinishedSeries(MsoyUI.formatDate(earnedDate));
        add(MsoyUI.createLabel(whenEarned, "WhenEarned"));
    }

    protected void addInProgressBits (InProgressBadge badge)
    {
        FlowPanel coinReward = MsoyUI.createFlowPanel("CoinReward");
        coinReward.add(MsoyUI.createImage(Currency.COINS.getSmallIcon(), null));
        coinReward.add(MsoyUI.createLabel(Currency.COINS.format(badge.coinValue), null));
        add(coinReward);

        if (badge.progress >= 0) {
            add(new ProgressBar(badge.progress));
            add(MsoyUI.createImage("/images/me/passport_progress.png", "ProgressLabel"));
        }

        ClickListener goListener = getGoListener(badge);
        if (goListener != null) {
            add(MsoyUI.createImageButton("GoButton", goListener));
        }
    }

    protected ClickListener getGoListener (Badge badge)
    {
        // TODO: this method of determining where to go for each badge is disappointing... this
        // is the only piece of badge UI code that needs changing when a new badge is added.
        // It would be great if this were done more dynamically...

        switch (badge.badgeCode) {
        case BadgeCodes.FRIENDLY:
            return Link.createListener(Pages.GROUPS, "");

        case BadgeCodes.MAGNET:
            return Link.createListener(Pages.PEOPLE, "invites");

        case BadgeCodes.FIXTURE:
            return Link.createListener(Pages.GROUPS, "");

        case BadgeCodes.EXPLORER:
            return Link.createListener(Pages.ROOMS, "");

        case BadgeCodes.GAMER: // same as below
        case BadgeCodes.CONTENDER: // same as below
        case BadgeCodes.COLLECTOR:
            return Link.createListener(Pages.GAMES, "");

        case BadgeCodes.CHARACTER_DESIGNER:
            return Link.createListener(Pages.STUFF, "" + Item.AVATAR);

        case BadgeCodes.FURNITURE_BUILDER:
            return Link.createListener(Pages.STUFF, "" + Item.FURNITURE);

        case BadgeCodes.LANDSCAPE_PAINTER:
            return Link.createListener(Pages.STUFF, "" + Item.DECOR);

        case BadgeCodes.PROFESSIONAL:
            return Link.createListener(Pages.SHOP, "");

        case BadgeCodes.ARTISAN:
            return Link.createListener(Pages.SHOP, "");

        case BadgeCodes.JUDGE: // same as below
        case BadgeCodes.OUTSPOKEN: // same as below
        case BadgeCodes.SHOPPER:
            return Link.createListener(Pages.SHOP, "");
        }

        return null;
    }

    protected static class ProgressBar extends HorizontalPanel
    {
        public ProgressBar (float progress)
        {
            setStyleName("ProgressBar");
            setHeight(PROGRESS_HEIGHT + "px");

            if (progress <= 0) {
                addCap(Section.LEFT, Type.EMPTY);
                addFill(Type.EMPTY, PROGRESS_WIDTH - END_CAP_WIDTH * 2);
                addCap(Section.RIGHT, Type.EMPTY);
                return;
            }

            addCap(Section.LEFT, Type.FULL);
            int fullWidth = PROGRESS_WIDTH - END_CAP_WIDTH * 2;
            // if our progress isn't 100%, make sure there is enough room left over for the
            // transition cap so that it doesn't look like you're done until you actually are.
            fullWidth = progress == 1 ? fullWidth :
                Math.min(fullWidth - END_CAP_WIDTH, Math.max(0, (int)(progress * fullWidth)));
            if (fullWidth > 0) {
                addFill(Type.FULL, fullWidth);
            }
            transitionToEnd(PROGRESS_WIDTH - END_CAP_WIDTH - fullWidth);
        }

        protected void addCap (Section section, Type type)
        {
            add(MsoyUI.createImage(section.getPath(type), null));
        }

        protected void addFill (Type type, int width)
        {
            SimplePanel fillPanel = new SimplePanel();
            fillPanel.setStyleName("Fill");
            fillPanel.setHeight(PROGRESS_HEIGHT + "px");
            fillPanel.setWidth(width + "px");
            DOM.setStyleAttribute(fillPanel.getElement(),
                "backgroundImage", "url(" + Section.FILL.getPath(type) + ")");
            add(fillPanel);
        }

        protected void transitionToEnd(int remainingWidth)
        {
            // if we only have room for one cap, add it and be done
            if (remainingWidth <= END_CAP_WIDTH) {
                addCap(Section.RIGHT, Type.FULL);
                return;
            }

            add(MsoyUI.createImage(Type.TRANSITION.getBasePath(), null));
            // if we've only got room for two caps, add the second and be done.
            if (remainingWidth <= END_CAP_WIDTH * 2) {
                addCap(Section.RIGHT, Type.EMPTY);
                return;
            }

            addFill(Type.EMPTY, remainingWidth - END_CAP_WIDTH * 2);
            addCap(Section.RIGHT, Type.EMPTY);
        }

        protected static enum Section {
            LEFT("left.png"), FILL("tile.png"), RIGHT("right.png");

            public String getPath (Type type) {
                return type.getBasePath() + _path;
            }

            Section (String path) {
                _path = path;
            }

            protected String _path;
        }

        protected static enum Type {
            EMPTY("empty_"), TRANSITION("transition.png"), FULL("");

            public String getBasePath () {
                return BASE_PATH + _path;
            }

            Type (String path) {
                _path = path;
            }

            protected String _path;

            protected static final String BASE_PATH = "/images/me/passport_progress_";
        }
    }

    protected VerticalPanel _nameColumn;

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);

    protected static final int PROGRESS_WIDTH = 90;
    protected static final int END_CAP_WIDTH = 9;
    protected static final int PROGRESS_HEIGHT = 17;
}
