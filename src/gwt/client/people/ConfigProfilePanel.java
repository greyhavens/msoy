//
// $Id$

package client.people;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.CoinAwards;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.profile.gwt.Profile;
import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.profile.gwt.ProfileServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.item.ImageChooserPopup;
import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.TongueBox;
import client.util.ClickCallback;
import client.util.Link;
import client.util.MediaUtil;
import client.util.InfoCallback;
import client.util.ServiceUtil;
import client.util.TextBoxUtil;
import client.util.events.NameChangeEvent;

/**
 * Displays a streamlined interface for configuring some basic profile information for a new
 * user. This is step two of the register -> config profile -> find friends -> spam friends
 * registration process.
 */
public class ConfigProfilePanel extends FlowPanel
{
    public ConfigProfilePanel ()
    {
        setStyleName("configProfile");

        // TODO: need proper image (step 3)
        add(new TongueBox(null, InvitePanel.makeHeader(
                              "/images/people/share_header.png",
                              _msgs.cpIntro(""+CoinAwards.CREATED_PROFILE))));

        add(new TongueBox(_msgs.cpWhoAreYou(), _bits = new FlowPanel()));
        _bits.setStyleName("Bits");
        _bits.add(MsoyUI.createLabel(_msgs.cpLoading(), null));

        // load up whatever profile they have at the moment
        _profilesvc.loadProfile(
            CShell.getMemberId(), new InfoCallback<ProfileService.ProfileResult>() {
            public void onSuccess (ProfileService.ProfileResult result) {
                if (result != null) {
                    gotProfile(result.name, result.profile);
                } else {
                    onFailure(new Exception("What? Missing own profile?"));
                }
            }
        });
    }

    protected void gotProfile (MemberName name, final Profile profile)
    {
        _profile = profile;
        _bits.clear();

        _bits.add(_card = new SmartTable("Card", 0, 10));
        _card.setWidget(0, 0, MediaUtil.createMediaView(profile.photo, MediaDesc.THUMBNAIL_SIZE));
        _card.getFlexCellFormatter().setRowSpan(0, 0, 2);
        _card.setText(0, 1, fiddleName(name.toString()), 2, "Name");
        _card.setText(1, 0, _msgs.memberSince());
        _card.setText(1, 1, MsoyUI.formatDate(new Date(profile.memberSince), false));

        _bits.add(MsoyUI.createLabel(_msgs.cpWhoTip(), "Tip"));

        SmartTable config = new SmartTable("Config", 0, 5);
        _bits.add(config);
        int row = 0;
        config.setText(row, 0, _msgs.cpPickName());
        _name = MsoyUI.createTextBox(name.toString(), MemberName.MAX_DISPLAY_NAME_LENGTH, 20);
        TextBoxUtil.addTypingListener(_name, new Command() {
            public void execute () {
                _card.setText(0, 1, fiddleName(_name.getText().trim()));
            }
        });
        config.setWidget(row++, 1, _name);

        config.setText(row, 0, _msgs.cpUploadPhoto());
        config.setWidget(row++, 1, new Button(_msgs.cpSelect(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                ImageChooserPopup.displayImageChooser(true, new InfoCallback<MediaDesc>() {
                    public void onSuccess (MediaDesc photo) {
                        if (photo != null) {
                            _profile.photo = photo;
                            _card.setWidget(0, 0, MediaUtil.createMediaView(
                                                photo, MediaDesc.THUMBNAIL_SIZE));
                        }
                    }
                });
            }
        }));

        PushButton done = MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.cpDone(), null);
        config.setWidget(row, 0, done, 2, null);
        config.getFlexCellFormatter().setHorizontalAlignment(row++, 0, HasAlignment.ALIGN_RIGHT);

        new ClickCallback<Void>(done) {
            protected boolean callService () {
                _dname = _name.getText().trim();
                if (!MemberName.isValidDisplayName(_dname)) {
                    MsoyUI.infoNear(_cmsgs.displayNameInvalid(
                                        "" + MemberName.MIN_DISPLAY_NAME_LENGTH,
                                        "" + MemberName.MAX_DISPLAY_NAME_LENGTH), _name);
                    return false;
                } else if (!MemberName.isValidNonSupportName(_dname)) {
                    MsoyUI.infoNear(_cmsgs.nonSupportNameInvalid(), _name);
                    return false;
                }
                _profilesvc.updateProfile(_dname, false, _profile, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                CShell.frame.dispatchEvent(new NameChangeEvent(_dname));
                Link.go(Pages.PEOPLE, "ff");
                return false;
            }
            protected String _dname;
        };
    }

    protected static String fiddleName (String name)
    {
        return (name.length() == 0) ? _msgs.cpBlankName() : name;
    }

    protected FlowPanel _bits;
    protected SmartTable _card;
    protected TextBox _name;
    protected Profile _profile;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final ProfileServiceAsync _profilesvc = (ProfileServiceAsync)
        ServiceUtil.bind(GWT.create(ProfileService.class), ProfileService.ENTRY_POINT);
}
