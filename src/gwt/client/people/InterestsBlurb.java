//
// $Id$

package client.people;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.person.data.Interest;
import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.MemberCard;

import client.people.FriendsBlurb.FriendWidget;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.ClickCallback;
import client.util.MsoyUI;

public class InterestsBlurb extends Blurb
{
    // @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.interests.size() > 0 || CPeople.getMemberId() == pdata.name.getMemberId());
    }

    // @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);

        _interests = pdata.interests;

        setHeader(CPeople.msgs.interestsTitle());
        displayInterests();
    }

    protected void displayInterests ()
    {
        SmartTable contents = new SmartTable(0, 5);
        for (int ii = 0; ii < _interests.size(); ii++) {
            Interest interest = (Interest) _interests.get(ii);
            contents.setText(ii, 0, CPeople.dmsgs.getString("interest" + interest.type));
            contents.setWidget(ii, 1, linkify(interest.interests));
        }
        setContent(contents);

        // display the edit button if this is our profile
        if (_name.getMemberId() == CPeople.getMemberId()) {
            setFooterLabel(CPeople.msgs.interestsEdit(), new ClickListener() {
                public void onClick (Widget source) {
                    startEdit();
                }
            });
        }
    }

    protected void startEdit ()
    {
        SmartTable editor = new SmartTable(0, 5);

        _iEditors = new TextBox[Interest.TYPES.length];

        int row = 0;
        for (int ii = 0; ii < _iEditors.length; ii++) {
            int type = Interest.TYPES[ii];
            editor.setText(row, 0, CPeople.dmsgs.getString("interest" + type));
            _iEditors[ii] = MsoyUI.createTextBox(
                getCurrentInterest(type), Interest.MAX_INTEREST_LENGTH, -1);
            editor.setWidget(row++, 1, _iEditors[ii]);
        }

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(5);
        editor.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
        editor.setWidget(row++, 0, buttons, 2, null);

        buttons.add(new Button(CPeople.cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget source) {
                displayInterests();
            }
        }));

        Button update = new Button(CPeople.cmsgs.update());
        new ClickCallback(update) {
            public boolean callService () {
                _newInterests = getNewInterests();
                CPeople.profilesvc.updateInterests(CPeople.ident, _newInterests, this);
                return true;
            }

            public boolean gotResult (Object result) {
                // filter out our blank new interests
                for (int ii = 0; ii < _newInterests.size(); ii++) {
                    Interest interest = (Interest) _newInterests.get(ii);
                    if (interest.interests.length() == 0) {
                        _newInterests.remove(ii--);
                    }
                }
                _interests = _newInterests;
                displayInterests();
                return true;
            }

            protected List _newInterests;
        };
        buttons.add(update);

        setContent(editor);
    }

    protected List getNewInterests ()
    {
        List interests = new ArrayList();
        for (int ii = 0; ii < _iEditors.length; ii++) {
            Interest interest = new Interest();
            interest.type = Interest.TYPES[ii];
            interest.interests = _iEditors[ii].getText();
            interests.add(interest);
        }

        return interests;
    }

    protected String getCurrentInterest (int type)
    {
        for (int ii = 0; ii < _interests.size(); ii++) {
            Interest interest = (Interest) _interests.get(ii);
            if (interest.type == type) {
                return interest.interests;
            }
        }

        return "";
    }

    protected FlowPanel linkify (String interests)
    {
        FlowPanel panel = new FlowPanel();
        String[] ivec = interests.split(",");
        for (int ii = 0; ii < ivec.length; ii++) {
            if (panel.getWidgetCount() > 0) {
                panel.add(new InlineLabel(",", false, false, true));
            }
            String interest = ivec[ii].trim();
            panel.add(Application.createLink(interest, Page.PEOPLE,
                Args.compose("search", "0", interest)));
        }

        return panel;
    }

    protected List _interests;
    protected TextBox[] _iEditors;
}
