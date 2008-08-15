//
// $Id$

package client.me;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.badge.data.all.InProgressBadge;

import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MeServiceAsync;
import com.threerings.msoy.person.gwt.PassportData;

import client.shell.DynamicMessages;
import client.ui.HeaderBox;
import client.ui.Marquee;
import client.ui.MsoyUI;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class PassportPanel extends VerticalPanel
{
    public PassportPanel ()
    {
        setStyleName("passport");

        _mesvc.loadBadges(new MsoyCallback<PassportData> () {
            public void onSuccess (PassportData data) {
                init(data);
            }
        });
    }

    protected void init (PassportData data)
    {
        add(new NextPanel(data.nextBadges));
        HeaderBox contents = new HeaderBox(null, _msgs.passportStampsTitle(data.stampOwner));
        contents.makeRoundBottom();
        add(contents);
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

            // TODO pull out the real "next" badges
            HorizontalPanel nextBadges = new HorizontalPanel();
            nextBadges.setStyleName("NextBadges");
            nextBadges.add(MsoyUI.createImage("/images/me/passport_box_left.png", null));
            _badgePanel = new HorizontalPanel();
            _badgePanel.setStyleName("NextBadgesPanel");
            nextBadges.add(_badgePanel);
            nextBadges.setCellWidth(_badgePanel, "100%");
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
            }
        }

        protected HorizontalPanel _badgePanel;
        protected List<InProgressBadge> _badges;
    }

    protected static final int MAX_NEXT_BADGES = 4;

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
    protected static final MeServiceAsync _mesvc = (MeServiceAsync)
        ServiceUtil.bind(GWT.create(MeService.class), MeService.ENTRY_POINT);
}
