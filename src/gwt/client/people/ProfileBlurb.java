//
// $Id$

package client.people;

import java.util.Date;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.web.client.ProfileService;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.DateFields;
import client.util.FlashClients;
import client.util.ImageChooserPopup;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.RowPanel;
import client.util.events.FlashEvents;
import client.util.events.NameChangeEvent;

/**
 * Displays a person's basic profile information.
 */
public class ProfileBlurb extends Blurb
{
    // @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
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
        VerticalPanel bits = new VerticalPanel();
        bits.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
        bits.add(MediaUtil.createMediaView(_profile.photo, MediaDesc.THUMBNAIL_SIZE));
        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(5);
        bits.add(buttons);
        boolean isMe = (_name.getMemberId() == CPeople.getMemberId());

        // add our various buttons: homepage, send mail, visit, admin info
        if (!isBlank(_profile.homePageURL)) {
            Anchor homepage = new Anchor(_profile.homePageURL, "");
            homepage.setHTML("<img border=\"0\" src=\"/images/profile/homepage.png\">");
            homepage.setFrameTarget("_blank");
            homepage.setTitle(CPeople.msgs.showHomepage());
            buttons.add(homepage);
        }
        if (CPeople.getMemberId() != 0 && !isMe) {
            buttons.add(Application.createImageLink("/images/profile/sendmail.png",
                                                    CPeople.msgs.sendMail(), Page.MAIL,
                                                    Args.compose("w", "m", ""+_name.getMemberId())));
        }
        buttons.add(Application.createImageLink("/images/profile/visithome.png",
                                                CPeople.msgs.visitHome(),
                                                Page.WORLD, "m" + _name.getMemberId()));
        if (CPeople.getMemberId() != 0 && !_pdata.isOurFriend && !isMe) {
            buttons.add(MsoyUI.createActionImage("/images/profile/addfriend.png",
                                                 CPeople.msgs.inviteFriend(),
                                                 InviteFriendPopup.createListener(_name)));
        }
        if (CPeople.isAdmin()) {
            buttons.add(Application.createImageLink("/images/profile/admininfo.png",
                                                    CPeople.msgs.adminBrowse(), Page.ADMIN,
                                                    Args.compose("browser", _name.getMemberId())));
        }

        // create the info table with their name, a/s/l, etc.
        SmartTable info = new SmartTable(0, 5);
        info.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        info.addText(_name.toString(), 1, "Name");

        // add our informational bits (headline, A/S/L)
        if (!isBlank(_profile.headline)) {
            info.addText(_profile.headline, 1, "Headline");
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

        // create our detail bits
        SmartTable details = new SmartTable(0, 5);

        // add our various detail bits (level, permaname, first/last logon dates)
        addDetail(details, CPeople.msgs.level(), "" + _profile.level);
        if (!isBlank(_profile.permaName)) {
            addDetail(details, CPeople.msgs.permaName(), _profile.permaName);
        }
        if (_profile.memberSince > 0L) {
            addDetail(details, CPeople.msgs.memberSince(),
                      _sfmt.format(new Date(_profile.memberSince)));
        }
        if (_profile.lastLogon > 0L) {
            addDetail(details, CPeople.msgs.lastOnline(),
                      _lfmt.format(new Date(_profile.lastLogon)));
        }

        SmartTable content = new SmartTable("Profile", 0, 0);
        content.setWidget(0, 0, bits, 1, "Photo");
        content.getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
        content.setWidget(0, 1, info);
        content.setWidget(0, 2, details);
        setContent(content);

        // display the edit button if this is our profile
        if (_name.getMemberId() == CPeople.getMemberId()) {
            setFooterLabel("Edit Profile", new ClickListener() {
                public void onClick (Widget source) {
                    startEdit();
                }
            });
        }
    }

    protected void addDetail (SmartTable detail, String label, String text)
    {
        int row = detail.getRowCount();
        detail.setText(row, 0, label, 1, "Detail");
        // detail.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
        detail.setText(row, 1, text, 1, "Detail");
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
        _ename.setMaxLength(Profile.MAX_DISPLAY_NAME_LENGTH);
        _ename.setText(_name.toString());

        econtent.setText(row, 0, "Photo");
        RowPanel panel = new RowPanel();
        panel.add(_ephoto = new SimplePanel());
        _ephoto.setWidget(MediaUtil.createMediaView(_profile.photo, MediaDesc.HALF_THUMBNAIL_SIZE));

        panel.add(new Button("Select New...", new ClickListener() {
            public void onClick (Widget source) {
                ImageChooserPopup.displayImageChooser(true, new MsoyCallback() {
                    public void onSuccess (Object result) {
                        _profile.photo = (MediaDesc)result;
                        _ephoto.setWidget(MediaUtil.createMediaView(
                                              _profile.photo, MediaDesc.HALF_THUMBNAIL_SIZE));
                    }
                });
            }
        }));
        econtent.setWidget(row++, 1, panel);

        econtent.setText(row, 0, CPeople.msgs.headline());
        _eheadline = MsoyUI.createTextBox(_profile.headline, Profile.MAX_HEADLINE_LENGTH, 30);
        econtent.setWidget(row++, 1, _eheadline);

        econtent.setText(row, 0, CPeople.msgs.homepage());
        _ehomepage = MsoyUI.createTextBox(_profile.homePageURL, Profile.MAX_HOMEPAGE_LENGTH, 30);
        econtent.setWidget(row++, 1, _ehomepage);

        econtent.setText(row, 0, CPeople.msgs.esex());
        econtent.setWidget(row++, 1, _esex = new ListBox());
        _esex.addItem("Don't show");
        _esex.addItem("Male");
        _esex.addItem("Female");
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

        Button cancel = new Button(CPeople.cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget source) {
                displayProfile();
            }
        });
        Button commit = new Button(CPeople.cmsgs.update(), new ClickListener() {
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
        if (!Profile.isValidDisplayName(name)) {
            MsoyUI.infoNear(CPeople.msgs.displayNameInvalid(
                                "" + Profile.MIN_DISPLAY_NAME_LENGTH,
                                "" + Profile.MAX_DISPLAY_NAME_LENGTH), _ename);
            return;
        }

        // configure our profile instance with their bits
        _name = new MemberName(name, _name.getMemberId());
        _profile.headline = _eheadline.getText().trim();
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
            long birthTime = DateFields.toDate(_profile.birthday).getTime();
            _profile.age = (int)((System.currentTimeMillis() - birthTime) / YEAR_MILLIS);
        } else {
            _profile.age = 0;
        }

        CPeople.profilesvc.updateProfile(CPeople.ident, name, _profile, new MsoyCallback() {
            public void onSuccess (Object result) {
                displayProfile();
                if (!name.equals(CPeople.creds.name.toString())) {
                    FlashEvents.dispatchEvent(new NameChangeEvent(name));
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
    protected TextBox _ename, _eheadline, _ehomepage, _elocation;
    protected CheckBox _eshowAge;
    protected ListBox _esex;
    protected DateFields _ebirthday;

    protected static SimpleDateFormat _sfmt = new SimpleDateFormat("MMM dd, yyyy");
    protected static SimpleDateFormat _lfmt = new SimpleDateFormat("MMM dd, yyyy h:mmaa");

    protected static final long YEAR_MILLIS = (365L * 24L * 60L * 60L * 1000L);
}
