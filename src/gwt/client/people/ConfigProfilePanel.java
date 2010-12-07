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
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.DateUtil;

import com.threerings.orth.data.MediaDesc;

import com.threerings.orth.data.MediaDescSize;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.profile.gwt.Profile;
import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.profile.gwt.ProfileServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.imagechooser.ImageChooserPopup;
import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.StretchButton;
import client.util.ClickCallback;
import client.util.Link;
import client.util.MediaUtil;
import client.util.InfoCallback;
import client.util.TextBoxUtil;
import client.util.events.NameChangeEvent;

/**
 * Displays a streamlined interface for configuring some basic profile information for a new user.
 * You are brought here after validating the registration email.
 */
public class ConfigProfilePanel extends FlowPanel
{
    public ConfigProfilePanel ()
    {
        setStyleName("configProfile");

        add(MsoyUI.createLabel(_msgs.cpPageTitle(), "Title"));
        add(MsoyUI.createImage("/images/people/confprof_rooms_promo.png", "RoomsPromo"));
        add(_dynamicContent = MsoyUI.createFlowPanel("DynamicContent"));
        _dynamicContent.add(MsoyUI.createNowLoading());

//
// // TODO: need proper image (step 3)
// add(new TongueBox(null, InvitePanel.makeHeader(
// "/images/people/share_header.png",
// _msgs.cpIntro(""+CoinAwards.CREATED_PROFILE))));
//
// add(new TongueBox(_msgs.cpWhoAreYou(), _bits = new FlowPanel()));
// _bits.setStyleName("Bits");
// _bits.add(MsoyUI.createLabel(_msgs.cpLoading(), null));

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
        _dynamicContent.clear();

        _dynamicContent.add(MsoyUI.createLabel(_msgs.cpInstructions(), "Instructions"));

        // name and photo form on the left, preview card on the right
        FloatPanel configAndPreview = new FloatPanel("ConfigAndPreview");
        _dynamicContent.add(configAndPreview);

        SmartTable config = new SmartTable("Config", 0, 5);
        configAndPreview.add(config);
        int row = 0;
        config.setText(row, 0, _msgs.cpPickName());
        _name = MsoyUI.createTextBox(name.toString(), MemberName.MAX_DISPLAY_NAME_LENGTH, 20);
        TextBoxUtil.addTypingListener(_name, new Command() {
            public void execute () {
                _preview.setText(0, 1, fiddleName(_name.getText().trim()));
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
                            _preview.setWidget(0, 0, MediaUtil.createMediaView(photo,
                                MediaDescSize.THUMBNAIL_SIZE));
                        }
                    }
                });
            }
        }));

        _preview = new SmartTable("Preview", 0, 10);
        _preview.setWidget(0, 0, MediaUtil.createMediaView(profile.photo,
            MediaDescSize.THUMBNAIL_SIZE));
        _preview.getFlexCellFormatter().setRowSpan(0, 0, 2);
        _preview.setText(0, 1, fiddleName(name.toString()), 2, "Name");
        _preview.setText(1, 0, _msgs.memberSince());
        _preview.setText(1, 1, DateUtil.formatDate(new Date(profile.memberSince), false));
        configAndPreview.add(MsoyUI.createFlowPanel("PreviewContainer", _preview));

//        _profile = profile;
//        _bits.clear();
//
//
//        _bits.add(MsoyUI.createLabel(_msgs.cpWhoTip(), "Tip"));
//
//        SmartTable config = new SmartTable("Config", 0, 5);
//        _bits.add(config);
//        int row = 0;
//        config.setText(row, 0, _msgs.cpPickName());
//        _name = MsoyUI.createTextBox(name.toString(), MemberName.MAX_DISPLAY_NAME_LENGTH, 20);
//        TextBoxUtil.addTypingListener(_name, new Command() {
//            public void execute () {
//                _preview.setText(0, 1, fiddleName(_name.getText().trim()));
//            }
//        });
//        config.setWidget(row++, 1, _name);
//
//        config.setText(row, 0, _msgs.cpUploadPhoto());
//        config.setWidget(row++, 1, new Button(_msgs.cpSelect(), new ClickHandler() {
//            public void onClick (ClickEvent event) {
//                ImageChooserPopup.displayImageChooser(true, new InfoCallback<MediaDesc>() {
//                    public void onSuccess (MediaDesc photo) {
//                        if (photo != null) {
//                            _profile.photo = photo;
//                            _preview.setWidget(0, 0, MediaUtil.createMediaView(
//                                                photo, MediaDesc.THUMBNAIL_SIZE));
//                        }
//                    }
//                });
//            }
//        }));

        StretchButton done = StretchButton.makeOrange(_msgs.cpDone(), null);
        _dynamicContent.add(done);

        // PushButton done = MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.cpDone(), null);
        // config.setWidget(row, 0, done, 2);
        // config.getFlexCellFormatter().setHorizontalAlignment(row++, 0,
        // HasAlignment.ALIGN_RIGHT);

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
                _profilesvc.updateProfile(CShell.getMemberId(), _dname, false, _profile, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                CShell.frame.dispatchEvent(new NameChangeEvent(_dname));
                Link.go(Pages.WORLD, "h");
                return false;
            }
            protected String _dname;
        };
    }

    protected static String fiddleName (String name)
    {
        return (name.length() == 0) ? _msgs.cpBlankName() : name;
    }

    protected FlowPanel _dynamicContent;
    // protected FlowPanel _bits;
    protected SmartTable _preview;
    protected TextBox _name;
    protected Profile _profile;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final ProfileServiceAsync _profilesvc = GWT.create(ProfileService.class);
}
