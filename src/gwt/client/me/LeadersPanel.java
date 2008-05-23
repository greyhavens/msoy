package client.me;

import java.util.List;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.MemberCard;
import client.shell.Application;
import client.shell.CShell;
import client.shell.Page;
import client.util.MediaUtil;
import client.util.MsoyCallback;

/**
 * Displays a summary of what Whirled is and calls to action.
 */
public class LeadersPanel extends AbsolutePanel
{
    public LeadersPanel ()
    {
        setStyleName("LeadersPanel");
        
        add(_leaders = new SmartTable("LeadersTable", 0, 0), 10, 35);
        
        SimplePanel levelUp = new SimplePanel();
        levelUp.setStyleName("LevelUp");
        Anchor anchor = new Anchor("http://wiki.whirled.com/Level", CMe.msgs.landingLevelUp(), "_blank");
        levelUp.add(anchor);
        add(levelUp, 10, 285);
        
        /*
         * TODO add once leader board page is complete
        FlowPanel viewEntire = new FlowPanel();
        viewEntire.setStyleName("ViewLeaderBoard");
        viewEntire.add(Application.createLink(CMe.msgs.landingViewLeaderBoard(), Page.HELP, ""));
        add(viewEntire, 10, 315);
        */
        
        CShell.membersvc.getLeaderList(new MsoyCallback() {
            public void onSuccess (Object result) {
                setLeaders((List) result);
            }
        });
    }
    
    /**
     * Display the top 6 leading members in the _leaders table
     */
    protected void setLeaders(List leaders) 
    {
        if (_leaders.getRowCount() > 0) {
            return;
        }

        for (int i = 0; i < 6; i++) {
            if (i >= leaders.size()) {
                return;
            }
            MemberCard card = (MemberCard) leaders.get(i);

            ClickListener click = Application.createLinkListener(Page.PEOPLE, "" + card.name.getMemberId());
            Widget photo = MediaUtil.createMediaView(card.photo, MediaDesc.HALF_THUMBNAIL_SIZE, click);
            Widget name = Application.createLink(
                    card.name.toString(), Page.PEOPLE, ""+card.name.getMemberId());
            Widget level = new HTML("" + card.level);
            
            _leaders.setWidget(i*2, 0, photo);
            _leaders.getFlexCellFormatter().setStyleName(i*2, 0, "Photo");
            _leaders.setWidget(i*2, 1, name);
            _leaders.getFlexCellFormatter().setStyleName(i*2, 1, "Name");
            _leaders.setWidget(i*2, 2, level);
            _leaders.getFlexCellFormatter().setHorizontalAlignment(i*2, 2, HasAlignment.ALIGN_CENTER);
            _leaders.getFlexCellFormatter().setStyleName(i*2, 2, "Level");           
            _leaders.setWidget((i*2)+1, 0, WidgetUtil.makeShim(1, 1));
            //_leaders.setText((i*2)+1, 0, "");
            _leaders.getFlexCellFormatter().setStyleName((i*2)+1, 0, "Divider"); 
            _leaders.getFlexCellFormatter().setColSpan((i*2)+1, 0, 3);
        }
    }
    
    protected SmartTable _leaders;
}
