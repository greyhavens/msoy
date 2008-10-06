//
// $Id$

package client.me;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.badge.gwt.StampCategory;

import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MeServiceAsync;
import com.threerings.msoy.person.gwt.PassportData;

import client.shell.DynamicLookup;
import client.ui.HeaderBox;
import client.ui.Marquee;
import client.ui.MsoyUI;
import client.ui.TongueBox;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class PassportPanel extends FlowPanel
{
    public PassportPanel (int memberId)
    {
        setStyleName("passport");

        _mesvc.loadBadges(memberId, new MsoyCallback<PassportData> () {
            public void onSuccess (PassportData data) {
                if (data == null) {
                    MsoyUI.error(_msgs.passportPlayerNotFound());
                } else {
                    init(data);
                }
            }
        });
    }

    protected void init (PassportData data)
    {
        if (data.nextBadges != null) {
            add(new NextPanel(data.nextBadges));
        }

        HeaderBox contents = new HeaderBox(null, _msgs.passportStampsTitle(data.stampOwner));
        contents.makeRoundBottom();
        add(contents);
        for (StampCategory category : StampCategory.values()) {
            String catNameLower = category.toString().toLowerCase();
            String catName = _dmsgs.xlate("passportCategory_" + catNameLower);
            FlowPanel stamps = new FlowPanel();
            contents.add(new TongueBox(MsoyUI.createImage(
                "/images/me/icon_" + catNameLower + ".png", null), catName, stamps));

            if (data.stamps.get(category).size() == 0) {
                stamps.add(MsoyUI.createSimplePanel(MsoyUI.createLabel(
                    _msgs.passportEmptyCategory(data.stampOwner, catName), null), "EmptyLabel"));
                continue;
            }

            for (Badge badge : data.stamps.get(category)) {
                stamps.add(MsoyUI.createSimplePanel(new BadgeDisplay(badge), "BoxedBadge"));
            }
        }
   }

    protected static class NextPanel extends VerticalPanel
    {
        public NextPanel (List<InProgressBadge> badges)
        {
            setStyleName("NextPanel");
            setSpacing(0);

            _badges = badges;
            buildUI();
            shuffle();
        }

        protected void buildUI ()
        {
            HorizontalPanel header = new HorizontalPanel();
            header.add(MsoyUI.createImage("/images/me/passport_icon.png", "Icon"));
            header.add(MsoyUI.createLabel(_msgs.passportDescription(), "Description"));
            header.add(new Marquee(null, _msgs.passportMarquee()));
            add(header);

            HorizontalPanel nextHeader = new HorizontalPanel();
            nextHeader.setStyleName("NextHeader");
            nextHeader.add(MsoyUI.createImage("/images/me/passport_header_left.png", null));
            HorizontalPanel headerContent = new HorizontalPanel();
            headerContent.setStyleName("NextHeaderContent");
            headerContent.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            headerContent.add(MsoyUI.createImage("/images/me/passport_next.png", null));
            Label label = MsoyUI.createLabel(_msgs.passportNextBar(), "NextHeaderText");
            headerContent.add(label);
            headerContent.setCellWidth(label, "100%");
            headerContent.add(MsoyUI.createActionImage(
                "/images/me/passport_shuffle.png", new ClickListener () {
                    public void onClick (Widget sender) {
                        shuffle();
                    }
                }));
            nextHeader.add(headerContent);
            nextHeader.setCellWidth(headerContent, "100%");
            nextHeader.add(MsoyUI.createImage("/images/me/passport_header_right.png", null));
            add(nextHeader);

            HorizontalPanel nextBadges = new HorizontalPanel();
            nextBadges.setStyleName("NextBadges");
            nextBadges.add(MsoyUI.createImage("/images/me/passport_box_left.png", null));
            _badgePanel = new HorizontalPanel();
            _badgePanel.setStyleName("NextBadgesPanel");
            nextBadges.add(_badgePanel);
            nextBadges.add(MsoyUI.createImage("/images/me/passport_box_right.png", null));
            add(nextBadges);
        }

        protected void shuffle ()
        {
            _badgePanel.clear();
            // I would use Collections.shuffle(), but it isn't implemented in GWT's JRE emulation.
            ArrayList<Integer> indexesUnused = new ArrayList<Integer>();
            for (int ii = 0; ii < _badges.size(); ii++) {
                indexesUnused.add(ii);
            }
            for (int ii = 0, max = Math.min(MAX_NEXT_BADGES, _badges.size()); ii < max; ii++) {
                _badgePanel.add(new BadgeDisplay(_badges.get(
                    indexesUnused.remove((int)(Math.random() * indexesUnused.size())))));
                if (ii < max - 1) {
                    _badgePanel.add(
                        MsoyUI.createImage("/images/me/passport_box_divider.png", "BadgeDivider"));
                }
            }
        }

        protected HorizontalPanel _badgePanel;
        protected List<InProgressBadge> _badges;
    }

    protected static final int MAX_NEXT_BADGES = 4;

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final MeServiceAsync _mesvc = (MeServiceAsync)
        ServiceUtil.bind(GWT.create(MeService.class), MeService.ENTRY_POINT);
}
