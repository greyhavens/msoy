//
// $Id$

package client.profile;

import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.Anchor;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.web.client.ProfileService;

import client.msgs.MailComposition;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.DateFields;
import client.util.FlashClients;
import client.util.ImageChooserPopup;
import client.util.MediaUtil;
import client.util.MsoyUI;

/**
 * Displays a person's basic profile information.
 */
public class ProfileBlurb extends Blurb
{
    // @Override // from Blurb
    protected Panel createContent ()
    {
        return new FlexTable();
    }

    // @Override // from Blurb
    protected void didInit (ProfileService.ProfileResult pdata)
    {
        if (pdata.profile != null) {
            setHeader(CProfile.msgs.profileTitle());
            _profile = pdata.profile;
            displayProfile();

        } else {
            setHeader(CProfile.msgs.errorTitle());
            setText(0, 0, CProfile.msgs.profileLoadFailed());
        }
    }

    protected void displayProfile ()
    {
        FlexTable content = new FlexTable();
        content.setStyleName("profileBlurb");

        VerticalPanel bits = new VerticalPanel();
        bits.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
        bits.add(MediaUtil.createMediaView(_profile.photo, MediaDesc.THUMBNAIL_SIZE));
        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(5);
        bits.add(buttons);
        content.setWidget(0, 0, bits);
        content.getFlexCellFormatter().setStyleName(0, 0, "Photo");
        content.getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);

        // add our various buttons: homepage, send mail, visit, admin info
        if (!isBlank(_profile.homePageURL)) {
            Anchor homepage = new Anchor(_profile.homePageURL, "");
            homepage.setHTML("<img border=\"0\" src=\"/images/profile/homepage.png\">");
            homepage.setFrameTarget("_blank");
            homepage.setTitle(CProfile.msgs.showHomepage());
            buttons.add(homepage);
        }
        if (_name.getMemberId() != CProfile.getMemberId()) {
            buttons.add(newControl(CProfile.msgs.sendMail(), "SendMail", new ClickListener() {
                public void onClick (Widget widget) {
                    new MailComposition(_name, null, null, null).show();
                }
            }));
        }
        buttons.add(newControl(CProfile.msgs.visitHome(), "VisitHome", new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.WORLD, "m" + _name.getMemberId());
            }
        }));
        if (CProfile.isAdmin()) {
            buttons.add(newControl(CProfile.msgs.adminBrowse(), "AdminInfo", new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.ADMIN, Args.compose("browser", _name.getMemberId()));
                }
            }));
        }

        // add their name in big happy font
        content.setText(0, 1, _name.toString());
        content.getFlexCellFormatter().setStyleName(0, 1, "Name");
        content.getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);

        // display the edit button if this is our profile
        if (_name.getMemberId() == CProfile.getMemberId()) {
            content.setWidget(0, 2, new Button("Edit", new ClickListener() {
                public void onClick (Widget source) {
                    startEdit();
                }
            }));
            content.getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);
            content.getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        } else {
            content.getFlexCellFormatter().setColSpan(0, 1, 2);
        }

        // add our informational bits (headline, A/S/L)
        if (!isBlank(_profile.headline)) {
            addInfo(content, _profile.headline, "Headline");
        }
        String ageSex = "";
        switch (_profile.sex) {
        case Profile.SEX_MALE:
            ageSex = CProfile.msgs.sex(CProfile.msgs.sexMale());
            break;
        case Profile.SEX_FEMALE:
            ageSex = CProfile.msgs.sex(CProfile.msgs.sexFemale());
            break;
        }
        if (_profile.age > 0) {
            if (ageSex.length() > 0) {
                ageSex += ", ";
            }
            ageSex += CProfile.msgs.age("" + _profile.age);
        }
        if (ageSex.length() > 0) {
            addInfo(content, ageSex, null);
        }
        if (!isBlank(_profile.location)) {
            addInfo(content, _profile.location, null);
        }

        // make the left column span all the info
        content.getFlexCellFormatter().setRowSpan(0, 0, content.getRowCount());

        // add our various detail bits (level, permaname, first/last logon dates)
        addDetail(content, CProfile.msgs.level(), "" + _profile.level);
        if (!isBlank(_profile.permaName)) {
            addDetail(content, CProfile.msgs.permaName(), _profile.permaName);
        }
        if (_profile.memberSince > 0L) {
            addDetail(content, CProfile.msgs.memberSince(),
                      _lfmt.format(new Date(_profile.memberSince)));
        }
        if (_profile.lastLogon > 0L) {
            addDetail(content, CProfile.msgs.lastOnline(),
                      _lfmt.format(new Date(_profile.lastLogon)));
        }

        setContent(content);
    }

    protected void addInfo (FlexTable content, String text, String style)
    {
        int row = content.getRowCount();
        content.setText(row, 0, text);
        content.getFlexCellFormatter().setColSpan(row, 0, 2);
        if (style != null) {
            content.getFlexCellFormatter().setStyleName(row, 0, style);
        }
    }

    protected void addDetail (FlexTable content, String label, String text)
    {
        int row = content.getRowCount();
        content.setText(row, 0, label);
        content.getFlexCellFormatter().setStyleName(row, 0, "Detail");
        content.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
        content.setText(row, 1, text);
        content.getFlexCellFormatter().setStyleName(row, 1, "Detail");
        content.getFlexCellFormatter().setColSpan(row, 1, 2);
    }

    protected void startEdit ()
    {
        if (_profile == null) {
            return; // nothing doing
        }

        FlexTable econtent = new FlexTable();
        econtent.setStyleName("profileEditor");

        int row = 0;
        econtent.setText(row, 0, CProfile.msgs.displayName());
        econtent.setWidget(row++, 1, _ename = new TextBox());
        _ename.setMaxLength(Profile.MAX_DISPLAY_NAME_LENGTH);
        _ename.setText(_name.toString());

        final AsyncCallback callback = new AsyncCallback() {
            public void onSuccess (Object result) {
                Photo photo = (Photo)result;
                if (photo != null) {
                    _profile.photo = photo.getThumbnailMedia();
                }
            }
            public void onFailure (Throwable cause) {
                CProfile.log("Failed to load images for profile photo pick.", cause);
                MsoyUI.error(CProfile.serverError(cause));
            }
        };
        econtent.setText(row, 0, "Photo");
        econtent.setWidget(row++, 1, _ephoto = new Button("Select...", new ClickListener() {
            public void onClick (Widget source) {
                ImageChooserPopup.displayImageChooser(callback);
            }
        }));

        econtent.setText(row, 0, CProfile.msgs.headline());
        econtent.setWidget(row++, 1, _eheadline = new TextBox());
        _eheadline.setVisibleLength(30);
        _eheadline.setText(unBlank(_profile.headline));

        econtent.setText(row, 0, CProfile.msgs.homepage());
        econtent.setWidget(row++, 1, _ehomepage = new TextBox());
        _ehomepage.setVisibleLength(30);
        _ehomepage.setText(unBlank(_profile.homePageURL));

        econtent.setText(row, 0, CProfile.msgs.esex());
        econtent.setWidget(row++, 1, _esex = new ListBox());
        _esex.addItem("Don't show");
        _esex.addItem("Male");
        _esex.addItem("Female");
        _esex.setSelectedIndex(_profile.sex);

        econtent.setText(row, 0, CProfile.msgs.ebirthday());
        econtent.setWidget(row++, 1, _ebirthday = new DateFields());
        if (_profile.birthday != null) {
            _ebirthday.setDate(_profile.birthday);
        }

        econtent.setText(row, 0, CProfile.msgs.eage());
        econtent.setWidget(row++, 1, _eshowAge = new CheckBox(""));
        _eshowAge.setChecked(_profile.age > 0);

        econtent.setText(row, 0, CProfile.msgs.elocation());
        econtent.setWidget(row++, 1, _elocation = new TextBox());
        _elocation.setVisibleLength(30);
        _elocation.setText(unBlank(_profile.location));

        econtent.setWidget(row, 1, new Button("Done", new ClickListener() {
            public void onClick (Widget source) {
                commitEdit();
            }
        }));
        getFlexCellFormatter().setHorizontalAlignment(row, 1, HasAlignment.ALIGN_RIGHT);

        setContent(econtent);
    }

    protected void commitEdit ()
    {
        // validate their display name
        String name = _ename.getText().trim();
        if (name.length() < Profile.MIN_DISPLAY_NAME_LENGTH ||
            name.length() > Profile.MAX_DISPLAY_NAME_LENGTH) {
            MsoyUI.infoNear(CProfile.msgs.displayNameInvalid(
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

        CProfile.profilesvc.updateProfile(CProfile.ident, name, _profile, new AsyncCallback() {
            public void onSuccess (Object result) {
                displayProfile();
                FlashClients.tutorialEvent("profileEdited");
            }
            public void onFailure (Throwable cause) {
                CProfile.log("Failed to update profile.", cause);
                MsoyUI.error(CProfile.serverError(cause));
            }
        });
    }

    protected Label newControl (String title, String style, ClickListener listener)
    {
        Label button = MsoyUI.createActionLabel("", style, listener);
        button.addStyleName("ControlButton");
        button.setTitle(title);
        return button;
    }

    protected VerticalPanel wrapEditor (String label, Widget editor)
    {
        VerticalPanel panel = new VerticalPanel();
        panel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
        panel.add(MsoyUI.createLabel(label, "EditLabel"));
        panel.add(editor);
        return panel;
    }

    protected boolean isBlank (String text)
    {
        return (text == null) || (text.length() == 0);
    }

    protected String unBlank (String text)
    {
        return (text == null) ? "" : text;
    }

    protected Profile _profile;

    protected FlexTable _content;

    protected Button _ephoto;
    protected TextBox _ename, _eheadline, _ehomepage, _elocation;
    protected CheckBox _eshowAge;
    protected ListBox _esex;
    protected DateFields _ebirthday;

    protected static SimpleDateFormat _lfmt = new SimpleDateFormat("MMM dd, yyyy");

    protected static final long YEAR_MILLIS = (365L * 24L * 60L * 60L * 1000L);
}
