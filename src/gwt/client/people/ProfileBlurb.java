//
// $Id$

package client.people;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.profile.gwt.Profile;
import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.profile.gwt.ProfileServiceAsync;

import client.item.ImageChooserPopup;
import client.item.ShopUtil;
import client.shell.Args;
import client.shell.Pages;
import client.shell.ShellMessages;
import client.ui.DateFields;
import client.ui.MsoyUI;
import client.ui.RowPanel;
import client.util.DateUtil;
import client.util.FlashClients;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;
import client.util.events.NameChangeEvent;

/**
 * Displays a person's basic profile information.
 */
public class ProfileBlurb extends Blurb
{
    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);

        // we're sort of a hacky blurb, so we nix our tongueBox style
        addStyleName("ProfileTongue");

        if (pdata.profile == null) {
            setText(0, 0, CPeople.msgs.profileLoadFailed());
        } else {
            _pdata = pdata;
            _profile = pdata.profile;
            displayProfile();
        }
    }

    protected void displayProfile ()
    {
        boolean isMe = (_name.getMemberId() == CPeople.getMemberId());

        // create our photo section with various buttons
        SmartTable photo = new SmartTable("Photo", 0, 5);
        photo.addWidget(
            MediaUtil.createMediaView(_profile.photo, MediaDesc.THUMBNAIL_SIZE), 2, null);
        if (!CPeople.isGuest() && !_pdata.isOurFriend && !isMe) {
            addButton(photo, "/images/profile/addfriend.png", CPeople.msgs.inviteFriend(),
                      InviteFriendPopup.createListener(_name));
        }
        if (!CPeople.isGuest() && !isMe) {
            addButton(photo, "/images/profile/sendmail.png", CPeople.msgs.sendMail(),
                      Pages.MAIL, Args.compose("w", "m", ""+_name.getMemberId()));
        }
        addButton(photo, "/images/profile/visithome.png", CPeople.msgs.visitHome(),
                  Pages.WORLD, "m" + _name.getMemberId());
        addButton(photo, "/images/profile/visithome.png",
                  CPeople.msgs.seeRooms(), Pages.PEOPLE, Args.compose("rooms", _name.getMemberId()));
        addButton(photo, "/images/profile/browseitems.png", CPeople.msgs.browseItems(),
                  Pages.SHOP, ShopUtil.composeArgs(Item.AVATAR, null, null, _name.getMemberId()));

        // create the info section with their name, a/s/l, etc.
        SmartTable info = new SmartTable("Info", 0, 5);
        info.addText(_name.toString(), 1, "Name");
        info.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        if (!isBlank(_profile.headline)) {
            info.addText(_profile.headline, 1, "Status");
        }
        String ageSex = "";
        switch (_profile.sex) {
        case Profile.SEX_MALE:
            ageSex = CPeople.msgs.sex(CPeople.msgs.sexMale());
            break;
        case Profile.SEX_FEMALE:
            ageSex = CPeople.msgs.sex(CPeople.msgs.sexFemale());
            break;
        }
        if (_profile.age > 0) {
            if (ageSex.length() > 0) {
                ageSex += ", ";
            }
            ageSex += CPeople.msgs.age("" + _profile.age);
        }
        if (ageSex.length() > 0) {
            info.addText(ageSex, 1, null);
        }
        if (!isBlank(_profile.location)) {
            info.addText(_profile.location, 1, null);
        }

        // create the detail section with level, last online, etc.
        FlowPanel details = new FlowPanel();
        details.setStyleName("Details");

        FlowPanel level = new FlowPanel();
        level.setStyleName("Level");
        level.add(MsoyUI.createLabel(CPeople.msgs.level(), "Label"));
        level.add(MsoyUI.createLabel("" + _profile.level, "Value"));
        details.add(level);

        SmartTable dbits = new SmartTable(0, 5);
        if (!isBlank(_profile.permaName)) {
            addDetail(dbits, CPeople.msgs.permaName(), _profile.permaName);
        }
        if (_profile.memberSince > 0L) {
            String since = MsoyUI.formatDate(new Date(_profile.memberSince));
            if (CPeople.isSupport()) {
                addDetail(dbits, CPeople.msgs.memberSince(),
                          Link.create(since, Pages.ADMINZ,
                                      Args.compose("info", _name.getMemberId())));
            } else {
                addDetail(dbits, CPeople.msgs.memberSince(), since);
            }
        }
        if (_profile.lastLogon > 0L) {
            addDetail(dbits, CPeople.msgs.lastOnline(),
                      MsoyUI.formatDateTime(new Date(_profile.lastLogon)));
        }
        if (!isBlank(_profile.homePageURL)) {
            Anchor homepage = new Anchor(_profile.homePageURL, _profile.homePageURL, "_blank", true);
            addDetail(dbits, CPeople.msgs.homepage(), homepage);
        }
        details.add(dbits);

        // display all of our sections in a nice little row
        HorizontalPanel content = new HorizontalPanel();
        content.setStyleName("Profile");
        content.setVerticalAlignment(HasAlignment.ALIGN_TOP);
        content.add(photo);
        content.add(info);
        content.add(details);
        setContent(content);

        // display the edit button if this is our profile
        if (_name.getMemberId() == CPeople.getMemberId()) {
            setFooterLabel(CPeople.msgs.profileEdit(), new ClickListener() {
                public void onClick (Widget source) {
                    startEdit();
                }
            });
        }
    }

    protected void addButton (SmartTable table, String path, String text, Pages page, String args)
    {
        int row = table.addWidget(Link.createImage(path, text, page, args), 1, null);
        table.setWidget(row, 1, Link.create(text, page, args), 1, null);
    }

    protected void addButton (SmartTable table, String path, String text, ClickListener listener)
    {
        int row = table.addWidget(MsoyUI.createActionImage(path, text, listener), 1, null);
        table.setWidget(row, 1, MsoyUI.createActionLabel(text, listener), 1, null);
    }

    protected void addDetail (SmartTable details, String label, String text)
    {
        int row = details.addText(label, 1, "Detail");
        details.setText(row, 1, text, 1, "Detail");
    }

    protected void addDetail (SmartTable details, String label, Widget widget)
    {
        int row = details.addText(label, 1, "Detail");
        details.setWidget(row, 1, widget, 1, "Detail");
    }

    protected void startEdit ()
    {
        if (_profile == null) {
            return; // nothing doing
        }

        SmartTable econtent = new SmartTable("profileEditor", 0, 5);

        int row = 0;
        econtent.setText(row, 0, CPeople.msgs.displayName());
        econtent.setWidget(row++, 1, _ename = new TextBox());
        _ename.setMaxLength(MemberName.MAX_DISPLAY_NAME_LENGTH);
        _ename.setText(_name.toString());

        econtent.setText(row, 0, "Photo");
        RowPanel panel = new RowPanel();
        panel.add(_ephoto = new SimplePanel());
        _ephoto.setWidget(MediaUtil.createMediaView(_profile.photo, MediaDesc.HALF_THUMBNAIL_SIZE));

        panel.add(new Button("Select New...", new ClickListener() {
            public void onClick (Widget source) {
                ImageChooserPopup.displayImageChooser(true, new MsoyCallback<MediaDesc>() {
                    public void onSuccess (MediaDesc photo) {
                        _profile.photo = photo;
                        _ephoto.setWidget(MediaUtil.createMediaView(
                                              _profile.photo, MediaDesc.HALF_THUMBNAIL_SIZE));
                    }
                });
            }
        }));
        econtent.setWidget(row++, 1, panel);

        econtent.setText(row, 0, CPeople.msgs.status());
        // seed the status line with a facebook-esque que, if empty
        String status = _profile.headline == "" || _profile.headline == null ?
            CPeople.msgs.statusQue() : _profile.headline;
        _estatus = MsoyUI.createTextBox(status, Profile.MAX_STATUS_LENGTH, 30);
        econtent.setWidget(row++, 1, _estatus);

        econtent.setText(row, 0, CPeople.msgs.homepage());
        _ehomepage = MsoyUI.createTextBox(_profile.homePageURL, Profile.MAX_HOMEPAGE_LENGTH, 30);
        HorizontalPanel ehomepageBox = new HorizontalPanel();
        ehomepageBox.add(new Label("http://"));
        ehomepageBox.add(_ehomepage);
        econtent.setWidget(row++, 1, ehomepageBox);

        econtent.setText(row, 0, CPeople.msgs.esex());
        econtent.setWidget(row++, 1, _esex = new ListBox());
        _esex.addItem("Don't show");
        _esex.addItem(CPeople.msgs.sexMale());
        _esex.addItem(CPeople.msgs.sexFemale());
        _esex.setSelectedIndex(_profile.sex);

        econtent.setText(row, 0, CPeople.msgs.ebirthday());
        econtent.setWidget(row++, 1, _ebirthday = new DateFields());
        if (_profile.birthday != null) {
            _ebirthday.setDate(_profile.birthday);
        }

        econtent.setText(row, 0, CPeople.msgs.eage());
        econtent.setWidget(row++, 1, _eshowAge = new CheckBox(""));
        _eshowAge.setChecked(_profile.age > 0);

        econtent.setText(row, 0, CPeople.msgs.elocation());
        econtent.setWidget(row++, 1, _elocation = new TextBox());
        _elocation.setVisibleLength(30);
        _elocation.setText(unBlank(_profile.location));

        Button cancel = new Button(_cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget source) {
                displayProfile();
            }
        });
        Button commit = new Button(_cmsgs.update(), new ClickListener() {
            public void onClick (Widget source) {
                commitEdit();
            }
        });
        econtent.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
        econtent.setWidget(row++, 0, MsoyUI.createButtonPair(cancel, commit), 2, null);

        setContent(econtent);
        setFooter(null);
    }

    protected void commitEdit ()
    {
        // validate their display name
        final String name = _ename.getText().trim();
        if (!MemberName.isValidDisplayName(name)) {
            MsoyUI.infoNear(_cmsgs.displayNameInvalid(
                                "" + MemberName.MIN_DISPLAY_NAME_LENGTH,
                                "" + MemberName.MAX_DISPLAY_NAME_LENGTH), _ename);
            return;
        }
        if (!CPeople.isSupport() && !MemberName.isValidNonSupportName(name)) {
            MsoyUI.infoNear(_cmsgs.nonSupportNameInvalid(), _ename);
            return;
        }

        // configure our profile instance with their bits
        _name = new MemberName(name, _name.getMemberId());
        String status = _estatus.getText();
        _profile.headline = status == CPeople.msgs.statusQue() ? "" : status.trim();
        _profile.homePageURL = _ehomepage.getText().trim();
        _profile.location = _elocation.getText().trim();
        _profile.sex = (byte)_esex.getSelectedIndex();
        int[] birthday = _ebirthday.getDate();
        if (birthday != null) { // leave their old birthday if they booch it
            _profile.birthday = birthday;
        }
        if (_eshowAge.isChecked() && _profile.birthday != null) {
            // this is not totally accurate, but it's only shown when a user completes their edit,
            // otherwise we compute their age on the server and we do so accurately
            long birthTime = DateUtil.toDate(_profile.birthday).getTime();
            _profile.age = (int)((System.currentTimeMillis() - birthTime) / YEAR_MILLIS);
        } else {
            _profile.age = 0;
        }

        _profilesvc.updateProfile(name, _profile, new MsoyCallback<Void>() {
            public void onSuccess (Void result) {
                displayProfile();
                if (!name.equals(CPeople.creds.name.toString())) {
                    CPeople.frame.dispatchEvent(new NameChangeEvent(name));
                }
                FlashClients.tutorialEvent("profileEdited");
            }
        });
    }

    protected boolean isBlank (String text)
    {
        return (text == null) || (text.length() == 0);
    }

    protected String unBlank (String text)
    {
        return (text == null) ? "" : text;
    }

    protected ProfileService.ProfileResult _pdata;
    protected Profile _profile;

    protected SimplePanel _ephoto;
    protected TextBox _ename, _estatus, _ehomepage, _elocation;
    protected CheckBox _eshowAge;
    protected ListBox _esex;
    protected DateFields _ebirthday;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final ProfileServiceAsync _profilesvc = (ProfileServiceAsync)
        ServiceUtil.bind(GWT.create(ProfileService.class), ProfileService.ENTRY_POINT);

    protected static final long YEAR_MILLIS = (365L * 24L * 60L * 60L * 1000L);
}
