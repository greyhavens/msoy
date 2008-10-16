//
// $Id$

package client.people;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.person.gwt.Interest;
import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.profile.gwt.ProfileServiceAsync;
import com.threerings.msoy.web.client.Args;
import com.threerings.msoy.web.client.Pages;

import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * Displays a member's interests and other random bits.
 */
public class InterestsBlurb extends Blurb
{
    @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.interests.size() > 0 || CShell.getMemberId() == pdata.name.getMemberId());
    }

    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);

        _interests = pdata.interests;

        setHeader(_msgs.interestsTitle());
        displayInterests();
    }

    protected void displayInterests ()
    {
        SmartTable contents = new SmartTable("Interests", 0, 5);
        for (int ii = 0; ii < _interests.size(); ii++) {
            Interest interest = _interests.get(ii);
            contents.setText(ii, 0, _dmsgs.xlate("interest" + interest.type), 1, "Type");
            if (Interest.isLinkedType(interest.type)) {
                contents.setWidget(ii, 1, linkify(interest.interests), 1, "Text");
            } else {
                contents.setText(ii, 1, interest.interests, 1, "Text");
            }
        }
        setContent(contents);

        // display the edit button if this is our profile
        if (_name.getMemberId() == CShell.getMemberId()) {
            setFooterLabel(_msgs.interestsEdit(), new ClickListener() {
                public void onClick (Widget source) {
                    startEdit();
                }
            });
        } else {
            setFooter(null);
        }
    }

    protected void startEdit ()
    {
        SmartTable editor = new SmartTable("Interests", 0, 5);

        _iEditors = new TextBox[Interest.TYPES.length];

        int row = 0;
        for (int ii = 0; ii < _iEditors.length; ii++) {
            int type = Interest.TYPES[ii];
            editor.setText(row, 0, _dmsgs.xlate("interest" + type), 1, "Type");
            _iEditors[ii] = MsoyUI.createTextBox(
                getCurrentInterest(type), Interest.MAX_INTEREST_LENGTH, -1);
            _iEditors[ii].addStyleName("Editor");
            editor.setWidget(row++, 1, _iEditors[ii]);
        }

        Button cancel = new Button(_cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget source) {
                displayInterests();
            }
        });
        Button update = new Button(_cmsgs.update());
        new ClickCallback<Void>(update) {
            public boolean callService () {
                _newInterests = getNewInterests();
                _profilesvc.updateInterests(_newInterests, this);
                return true;
            }

            public boolean gotResult (Void result) {
                // filter out our blank new interests
                for (int ii = 0; ii < _newInterests.size(); ii++) {
                    Interest interest = _newInterests.get(ii);
                    if (interest.interests.length() == 0) {
                        _newInterests.remove(ii--);
                    }
                }
                _interests = _newInterests;
                displayInterests();
                return true;
            }

            protected List<Interest> _newInterests;
        };

        editor.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
        editor.setWidget(row++, 0, MsoyUI.createButtonPair(cancel, update), 2, null);

        setContent(editor);
        setFooter(null);
    }

    protected List<Interest> getNewInterests ()
    {
        List<Interest> interests = new ArrayList<Interest>();
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
            Interest interest = _interests.get(ii);
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
            panel.add(Link.create(interest, Pages.PEOPLE,
                Args.compose("search", "0", interest)));
        }

        return panel;
    }

    protected List<Interest> _interests;
    protected TextBox[] _iEditors;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final ProfileServiceAsync _profilesvc = (ProfileServiceAsync)
        ServiceUtil.bind(GWT.create(ProfileService.class), ProfileService.ENTRY_POINT);
}
