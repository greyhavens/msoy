//
// $Id$

package client.people;

import java.util.Date;

import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.StringUtil;

import com.threerings.msoy.data.all.Award.AwardType;
import com.threerings.msoy.data.all.Award;
import com.threerings.msoy.data.all.Friendship;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.profile.gwt.Profile;
import com.threerings.msoy.profile.gwt.ProfileService.GreeterStatus;
import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.profile.gwt.ProfileServiceAsync;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.imagechooser.ImageChooserPopup;
import client.item.ShopUtil;
import client.person.GalleryActions;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.Page;
import client.shell.ShellMessages;
import client.ui.DateFields;
import client.ui.MsoyUI;
import client.ui.RowPanel;
import client.ui.ThumbBox;
import client.util.InfoCallback;
import client.util.Link;
import client.util.MediaUtil;
import client.util.events.NameChangeEvent;
import client.util.events.PageCommandEvent;

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
            setText(0, 0, _msgs.profileLoadFailed());
        } else {
            _pdata = pdata;
            _profile = pdata.profile;
            _greeter = _pdata.greeterStatus;
            displayProfile();
        }
    }

    protected void displayProfile ()
    {
        boolean isMe = (_name.getId() == CShell.getMemberId());

        // create our photo section with various buttons
        FlowPanel photo = MsoyUI.createFlowPanel("Photo");
        boolean hasProfileGallery = hasProfileGallery();
        photo.add(!hasProfileGallery ? new ThumbBox(_profile.photo) :
                  new ThumbBox(_profile.photo, Pages.PEOPLE,
                               GalleryActions.VIEW_PROFILE, _name.getId()));
        if (isMe && _profile.role == WebCreds.Role.REGISTERED) {
            photo.add(Link.create(_msgs.subscribeNow(), Pages.BILLING, "subscribe"));
        } else {
            photo.add(MsoyUI.createRoleLabel(_profile.role));
        }
        if (hasProfileGallery) {
            photo.add(Link.create(_msgs.photosOfMe(),
                    Pages.PEOPLE, GalleryActions.VIEW_PROFILE, _name.getId()));
        } else if (isMe) {
            photo.add(Link.create(_msgs.addPhotosOfMe(),
                    Pages.PEOPLE, GalleryActions.CREATE_PROFILE, _name.getId()));
        }

        // create the info section with their name, a/s/l, etc.
        SmartTable info = new SmartTable("Info", 0, 5);
        Widget icon = MsoyUI.createRoleIcon(_profile.role);
        Widget name = MsoyUI.createInlineLabel(_name.toString(), "Name");
        if (icon != null) {
            HorizontalPanel hbox = new HorizontalPanel();
            hbox.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
            hbox.add(icon);
            hbox.add(name);
            info.addWidget(hbox, 1);
        } else {
            info.addWidget(name, 1);
        }
        // info.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        if (!StringUtil.isBlank(_profile.headline)) {
            info.addText(_profile.headline, 1, "Status");
        }
        String ageSex = "";
        switch (_profile.sex) {
        case Profile.SEX_MALE:
            ageSex = _msgs.sex(_msgs.sexMale());
            break;
        case Profile.SEX_FEMALE:
            ageSex = _msgs.sex(_msgs.sexFemale());
            break;
        }
        if (_profile.age > 0) {
            if (ageSex.length() > 0) {
                ageSex += ", ";
            }
            ageSex += _msgs.age("" + _profile.age);
        }
        if (ageSex.length() > 0) {
            info.addText(ageSex, 1, "LesserInfo");
        }
        if (!StringUtil.isBlank(_profile.location)) {
            info.addText(_profile.location, 1, "LesserInfo");
        }
        if (_greeter == GreeterStatus.GREETER) {
            info.addText(_msgs.profileGreeterLabel(), 1, "LesserInfo");
        }

        // create our award box
        if (_pdata.profile.award != null) {
            Award award = _pdata.profile.award;
            FlowPanel awardBox = MsoyUI.createFlowPanel("AwardBox");
            info.addWidget(awardBox, 1);

            String page = award.type == Award.AwardType.BADGE ? "passport" : "medals";
            ClickHandler clicker = Link.createHandler(Pages.ME, page, _name.getId());

            if (award.type == AwardType.BADGE) {
                String hexCode = Integer.toHexString(award.awardId);
                awardBox.add(MsoyUI.createActionLabel(
                    _dmsgs.get("badge_" + hexCode, award.name), "Name", clicker));
                awardBox.add(MsoyUI.createLabel(_msgs.awardBadge(), "Type"));

            } else if (award.type == AwardType.MEDAL) {
                awardBox.add(MsoyUI.createActionLabel(award.name, "Name", clicker));
                awardBox.add(MsoyUI.createLabel(_msgs.awardMedal(), "Type"));
            }

            awardBox.add(MsoyUI.makeActionImage(
                MsoyUI.createImage(award.icon.getMediaPath(), "Icon"), null, clicker));
            awardBox.add(MsoyUI.createLabel(_msgs.awardEarned(DateUtil.formatDate(
                new Date(award.whenEarned))), "WhenEarned"));
        }

        // create the detail section with level, last online, etc.
        FlowPanel details = MsoyUI.createFlowPanel("Details");

        FlowPanel level = MsoyUI.createFlowPanel("Level");
        level.add(MsoyUI.createLabel(_msgs.level(), "Label"));
        level.add(MsoyUI.createLabel("" + _profile.level, "Value"));
        details.add(level);

        SmartTable dbits = new SmartTable(0, 5);
        if (!StringUtil.isBlank(_profile.permaName)) {
            addDetail(dbits, _msgs.permaName(), _profile.permaName);
        }
        if (_profile.memberSince > 0L) {
            String since = DateUtil.formatDate(new Date(_profile.memberSince));
            if (CShell.isSupport()) {
                addDetail(dbits, _msgs.memberSince(),
                          Link.create(since, Pages.ADMINZ, "info", _name.getId()));
            } else {
                addDetail(dbits, _msgs.memberSince(), since);
            }
        }
        if (_profile.lastLogon > 0L) {
            addDetail(dbits, _msgs.lastOnline(),
                      DateUtil.formatDateTime(new Date(_profile.lastLogon)));
        }
        if (!StringUtil.isBlank(_profile.homePageURL)) {
            Anchor homepage = new Anchor(
                _profile.homePageURL, _profile.homePageURL, "_blank", true);
            addDetail(dbits, _msgs.homepage(), homepage);
        }
        details.add(dbits);

        // create our action buttons
        _buttons = MsoyUI.createFlowPanel("Buttons");
        if (!isMe) {
            addFriendButton();
            if (!CShell.isGuest()) {
                addButton(_buttons, "/images/profile/sendmail.png", _msgs.sendMail(),
                    Pages.MAIL, "w", "m", ""+_name.getId());
            }
        }
        if (!CShell.getClientMode().isMinimal()) {
            addButton(_buttons, "/images/profile/visithome.png", _msgs.visitHome(),
                      Pages.WORLD, "m" + _name.getId());
            addButton(_buttons, "/images/profile/viewrooms.png", _msgs.seeRooms(),
                      Pages.PEOPLE, "rooms", _name.getId());
        }
        addButton(_buttons, "/images/profile/browseitems.png", _msgs.browseItems(),
                  Pages.SHOP, ShopUtil.composeArgs(MsoyItemType.AVATAR, null, null, _name.getId()));
        if (CShell.isAdmin()) {
            _buttons.add(new Button("Admin: Send feed", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    _profilesvc.sendRetentionEmail(_name.getId(), new InfoCallback<Void>() {
                        public void onSuccess (Void result) {
                            MsoyUI.info("Sent");
                        }
                    });
                }
            }));
        }

        // display all of our sections in a nice little layout
        SmartTable content = new SmartTable("Profile", 0, 0);
        content.setWidget(0, 0, photo);
        content.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        content.setWidget(0, 1, info);
        content.getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
        content.setWidget(0, 2, details);
        content.getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        content.setWidget(1, 1, _buttons, 2);
        setContent(content);

        // display the edit button if this is our profile or we're support
        if (CShell.isSupport() || _name.getId() == CShell.getMemberId()) {
            setFooterLabel(_msgs.profileEdit(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    startEdit();
                }
            });
            _registration = Page.register(new PageCommandEvent.Listener() {
                @Override public boolean act (PageCommandEvent commandEvent) {
                    if (commandEvent.getCommand().equals(PageCommandEvent.EDIT_PROFILE)) {
                        startEdit();
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    protected void addButton (FlowPanel buttons, String path, String text,
                              Pages page, Object... args)
    {
        buttons.add(Link.createImage(path, text, page, args));
        Widget link = Link.create(text, page, args);
        link.addStyleName("Link");
        buttons.add(link);
    }

    protected Widget addButton (FlowPanel buttons, String path, String text, ClickHandler listener)
    {
        Widget pair = MsoyUI.createButtonPair(
            MsoyUI.createActionImage(path, text, listener),
            MsoyUI.createActionLabel(text, "Link", listener));
        buttons.add(pair);
        return pair;
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

        unregisterForFlashCommands();

        SmartTable econtent = new SmartTable("profileEditor", 0, 5);

        int row = 0;
        econtent.setText(row, 0, _msgs.displayName());
        _ename = MsoyUI.createTextBox(_name.toString(), MemberName.MAX_DISPLAY_NAME_LENGTH, 30);
        econtent.setWidget(row++, 1, _ename);

        econtent.setText(row, 0, "Photo");
        RowPanel panel = new RowPanel();
        panel.add(_ephoto = new SimplePanel());
        _ephoto.setWidget(MediaUtil.createMediaView(_profile.photo, MediaDescSize.THUMBNAIL_SIZE));

        panel.add(new Button("Select New...", new ClickHandler() {
            public void onClick (ClickEvent event) {
                ImageChooserPopup.displayImageChooser(true, new InfoCallback<MediaDesc>() {
                    public void onSuccess (MediaDesc photo) {
                        if (photo != null) {
                            _profile.photo = photo;
                            _ephoto.setWidget(MediaUtil.createMediaView(
                                _profile.photo, MediaDescSize.THUMBNAIL_SIZE));
                        }
                    }
                });
            }
        }));
        econtent.setWidget(row++, 1, panel);

        econtent.setText(row, 0, _msgs.status());
        // seed the status line with a facebook-esque cue, if empty
        String status =  Strings.isNullOrEmpty(_profile.headline) ?
            _msgs.statusQue() : _profile.headline;
        _estatus = MsoyUI.createTextBox(status, Profile.MAX_STATUS_LENGTH, 30);
        econtent.setWidget(row++, 1, _estatus);

        econtent.setText(row, 0, _msgs.homepage());
        _ehomepage = MsoyUI.createTextBox(_profile.homePageURL, Profile.MAX_HOMEPAGE_LENGTH, 30);
        HorizontalPanel ehomepageBox = new HorizontalPanel();
        ehomepageBox.add(new Label("http://"));
        ehomepageBox.add(_ehomepage);
        econtent.setWidget(row++, 1, ehomepageBox);

        econtent.setText(row, 0, _msgs.esex());
        econtent.setWidget(row++, 1, _esex = new ListBox());
        _esex.addItem("Don't show");
        _esex.addItem(_msgs.sexMale());
        _esex.addItem(_msgs.sexFemale());
        _esex.setSelectedIndex(_profile.sex);

        econtent.setText(row, 0, _msgs.ebirthday());
        econtent.setWidget(row++, 1, _ebirthday = new DateFields());
        if (_profile.birthday != null) {
            _ebirthday.setDate(_profile.birthday);
        }

        econtent.setText(row, 0, _msgs.eage());
        econtent.setWidget(row++, 1, _eshowAge = new CheckBox(""));
        _eshowAge.setValue(_profile.age > 0);

        econtent.setText(row, 0, _msgs.elocation());
        econtent.setWidget(row++, 1, _elocation = MsoyUI.createTextBox(_profile.location, 255, 30));

        if (_greeter != GreeterStatus.DISABLED) {
            econtent.setText(row, 0, _msgs.egreeterLabel());
            econtent.setWidget(row++, 1, _egreeter = new CheckBox(_msgs.egreeterTip()));
            _egreeter.setValue(_greeter == GreeterStatus.GREETER);
        }

        econtent.setText(row, 0, _msgs.eawardLabel());
        FlowPanel awardPanel = MsoyUI.createFlowPanel("AwardPanel");
        awardPanel.add(MsoyUI.createInlineImage("/images/profile/awardbox.png"));
        awardPanel.add(new Label(_msgs.eawardTip()));
        econtent.setWidget(row++, 1, awardPanel);

        Button cancel = new Button(_cmsgs.cancel(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                displayProfile();
            }
        });
        Button commit = new Button(_cmsgs.update(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                commitEdit();
            }
        });
        econtent.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
        econtent.setWidget(row++, 0, MsoyUI.createButtonPair(cancel, commit), 2);

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
        if (!CShell.isSupport() && !MemberName.isValidNonSupportName(name)) {
            MsoyUI.infoNear(_cmsgs.nonSupportNameInvalid(), _ename);
            return;
        }

        // configure our profile instance with their bits
        _name = new MemberName(name, _name.getId());
        String status = _estatus.getText();
        _profile.headline = _msgs.statusQue().equals(status) ? "" : status.trim();
        _profile.homePageURL = _ehomepage.getText().trim();
        _profile.location = _elocation.getText().trim();
        _profile.sex = (byte)_esex.getSelectedIndex();
        int[] birthday = _ebirthday.getDate();
        if (birthday != null) { // leave their old birthday if they booch it
            _profile.birthday = birthday;
        }
        if (_eshowAge.getValue() && _profile.birthday != null) {
            // this is not totally accurate, but it's only shown when a user completes their edit,
            // otherwise we compute their age on the server and we do so accurately
            long birthTime = DateUtil.toDate(_profile.birthday).getTime();
            _profile.age = (int)((System.currentTimeMillis() - birthTime) / YEAR_MILLIS);
        } else {
            _profile.age = 0;
        }

        if (_egreeter != null) {
            _greeter = _egreeter.getValue() ? GreeterStatus.GREETER : GreeterStatus.NORMAL;
        }

        _profilesvc.updateProfile(_name.getId(), name, _greeter == GreeterStatus.GREETER,
            _profile, new InfoCallback<Void>() {
                public void onSuccess (Void result) {
                    displayProfile();
                    if (_name.getId() == CShell.getMemberId() &&
                        !name.equals(CShell.creds.name.toString())) {
                        CShell.frame.dispatchEvent(new NameChangeEvent(name));
                    }
                }
            });
    }

    protected void addFriendButton ()
    {
        final Command removeFriendBtn = new Command() {
            public void execute () {
                _buttons.remove(_friendBtn);
            }
        };
        String label;
        String image;
        ClickHandler handler;

        if (_pdata.friendship == Friendship.FRIENDS) {
            label = _msgs.removeFriend();
            image = "/images/profile/remove.png";
            handler = new FriendRemover(_pdata.name, removeFriendBtn);

        } else if (_pdata.friendship == Friendship.INVITED) {
            label = _msgs.retractFriend();
            image = "/images/profile/remove.png";
            handler = new ClickHandler() {
                public void onClick (ClickEvent event) {
                    _membersvc.removeFriend(_name.getId(), new InfoCallback<Void>() {
                        public void onSuccess (Void result) {
                            removeFriendBtn.execute();
                        }
                    });
                }
            };

        } else {
            label = (_pdata.friendship == Friendship.INVITEE) ?
                _msgs.validateFriend() : _msgs.inviteFriend();
            image = "/images/profile/addfriend.png";
            Boolean forceAuto = (_pdata.greeterStatus == GreeterStatus.GREETER) ||
                (_pdata.friendship == Friendship.INVITEE);
            handler = new FriendInviter(_name, "Profile", removeFriendBtn, forceAuto);
        }

        _friendBtn = addButton(_buttons, image, label, handler);
    }

    protected boolean hasProfileGallery ()
    {
        for (Gallery gal : _pdata.galleries) {
            if (gal.isProfileGallery()) {
                return true;
            }
        }
        return false;
    }

    @Override // from Widget
    protected void onDetach ()
    {
        super.onDetach();
        unregisterForFlashCommands();
    }

    protected void unregisterForFlashCommands ()
    {
        if (_registration != null) {
            _registration.remove();
            _registration = null;
        }
    }

    protected ProfileService.ProfileResult _pdata;
    protected Profile _profile;
    protected ProfileService.GreeterStatus _greeter;

    protected SimplePanel _ephoto;
    protected TextBox _ename, _estatus, _ehomepage, _elocation;
    protected CheckBox _eshowAge, _egreeter;
    protected ListBox _esex;
    protected DateFields _ebirthday;

    protected FlowPanel _buttons;
    /** The widgetry for adding or removing this person as a friend. */
    protected Widget _friendBtn;

    protected Page.Registration _registration;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final ProfileServiceAsync _profilesvc = GWT.create(ProfileService.class);
    protected static final WebMemberServiceAsync _membersvc = GWT.create(WebMemberService.class);

    protected static final long YEAR_MILLIS = (365L * 24L * 60L * 60L * 1000L);
}
