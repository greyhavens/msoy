//
// $Id$

package client.profile;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.Anchor;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.Profile;

import client.msgs.MailComposition;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.FlashClients;
import client.util.ImageChooserPopup;
import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * Displays a person's basic profile information.
 */
public class ProfileBlurb extends Blurb
{
    // @Override // from Blurb
    protected Panel createContent ()
    {
        _content = new FlexTable();
        _content.setStyleName("profileBlurb");

        int row = 0;
        _content.setWidget(row, 0, _photo = new Image());
        _content.getFlexCellFormatter().setStyleName(0, 0, "Photo");

        // editable stuff comes first
        _content.setWidget(row++, 1, _displayName = new Label("name"));
        _displayName.setStyleName("Name");
        _content.getFlexCellFormatter().setColSpan(0, 1, 2);
        _content.setWidget(row++, 1, _headline = new Label("headline"));
        _headline.setStyleName("Headline");

        // then status information and controls
        _content.setWidget(row++, 1, _permaName = new Label("permaname"));
        _content.setWidget(row++, 1, _lastOn = new Label("..."));
        _content.setWidget(row++, 1, _buttons = new RowPanel());

        // make the photo span everything
        _content.getFlexCellFormatter().setRowSpan(0, 0, row);

        _edit = new Button("Edit");
        _edit.addClickListener(new ClickListener() {
            public void onClick (Widget source) {
                if (_editing) {
                    commitEdit();
                } else {
                    startEdit();
                }
            }
        });

        return _content;
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
            _displayName.setText(CProfile.msgs.profileLoadFailed());
        }
    }

    protected void startEdit ()
    {
        if (_profile == null) {
            return; // nothing doing
        }

        // switch to update mode
        _edit.setText("Done");
        _editing = true;

        if (_ename == null) {
            final AsyncCallback callback = new AsyncCallback() {
                public void onSuccess (Object result) {
                    updatePhoto(((Photo)result).getThumbnailMedia());
                }
                public void onFailure (Throwable cause) {
                    CProfile.log("Failed to load images for profile photo pick.", cause);
                    MsoyUI.error(CProfile.serverError(cause));
                }
            };
            _ephoto = new Button("Select...");
            _ephoto.addClickListener(new ClickListener() {
                public void onClick (Widget source) {
                    ImageChooserPopup.displayImageChooser(callback);
                }
            });
            _ename = new TextBox();
            _ename.setMaxLength(Profile.MAX_DISPLAY_NAME_LENGTH);
            _eheadline = new TextBox();
            _eheadline.setVisibleLength(35);
            _ehomepage = new TextBox();
            _ehomepage.setVisibleLength(35);
        }

        _ename.setText(_name.toString());
        _eheadline.setText(_profile.headline == null ? "" : _profile.headline);
        _ehomepage.setText(_profile.homePageURL == null ? "" : _profile.homePageURL);

        VerticalPanel ppanel = new VerticalPanel();
        ppanel.add(_photo);
        ppanel.add(_ephoto);

        int row = 0;
        _content.setWidget(row, 0, ppanel);
        _content.setWidget(row++, 1, wrapEditor(CProfile.msgs.displayName(), _ename));
        _content.setWidget(row++, 1, wrapEditor(CProfile.msgs.headline(), _eheadline));
        _content.setWidget(row++, 1, wrapEditor(CProfile.msgs.homepage(), _ehomepage));
    }

    protected void displayProfile ()
    {
        updatePhoto(_profile.photo);

        _displayName.setText(_name.toString());
        if (_profile.permaName == null) {
            _permaName.setText("");
        } else {
            _permaName.setText(CProfile.msgs.permaName(_profile.permaName));
        }
        _headline.setText(_profile.headline);
        _lastOn.setText(_profile.lastLogon > 0L ?
                        CProfile.msgs.lastOnline(_lfmt.format(new Date(_profile.lastLogon))) : "");

        int row = 0;
        _content.setWidget(row, 0, _photo);
        _content.setWidget(row++, 1, _displayName);
        _content.setWidget(row++, 1, _headline);
        _content.setWidget(row++, 1, _permaName);

        _buttons.clear();

        // if they have a homepage configured show that button
        if (_profile.homePageURL != null && _profile.homePageURL.length() > 0) {
            Anchor homepage = new Anchor(_profile.homePageURL, "");
            homepage.setHTML("<img border=\"0\" src=\"/images/profile/homepage.png\">");
            homepage.setFrameTarget("_blank");
            homepage.setTitle(CProfile.msgs.showHomepage());
            _buttons.add(homepage);
        }

        // if this is not us, add some useful controls
        if (_name.getMemberId() != CProfile.getMemberId()) {
            _buttons.add(newControl(CProfile.msgs.sendMail(), "SendMail", new ClickListener() {
                public void onClick (Widget widget) {
                    new MailComposition(_name, null, null, null).show();
                }
            }));

//             _buttons.add(new Button("Neighborhood", new ClickListener() {
//                 public void onClick (Widget sender) {
//                     Application.go(Page.WORLD, "nm" + _name.getMemberId());
//                 }
//             }));

            _buttons.add(newControl(CProfile.msgs.visitHome(), "VisitHome", new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, "m" + _name.getMemberId());
                }
            }));
        }

        // if we're an admin, add a button for special info
        if (CProfile.isAdmin()) {
            _buttons.add(newControl(CProfile.msgs.adminBrowse(), "AdminInfo", new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.ADMIN, Args.compose("browser", _name.getMemberId()));
                }
            }));
        }

        // display the edit button if this is our profile
        if (_name.getMemberId() == CProfile.getMemberId()) {
            _buttons.add(_edit);
        }
    }

    protected VerticalPanel wrapEditor (String label, Widget editor)
    {
        VerticalPanel panel = new VerticalPanel();
        panel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
        panel.add(MsoyUI.createLabel(label, "EditLabel"));
        panel.add(editor);
        return panel;
    }

    protected Button newControl (String title, String style, ClickListener listener)
    {
        Button button = new Button("", listener);
        button.addStyleName("ControlButton");
        button.addStyleName(style);
        button.setTitle(title);
        return button;
    }

    protected void updatePhoto (MediaDesc photo)
    {
        if (photo != null) {
            _profile.photo = photo;
            _photo.setUrl(photo.getMediaPath());
        }
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

        CProfile.profilesvc.updateProfile(CProfile.ident, name, _profile, new AsyncCallback() {
            public void onSuccess (Object result) {
                // go back to edit mode
                _edit.setText("Edit");
                _editing = false;
                displayProfile();
                FlashClients.tutorialEvent("profileEdited");
            }
            public void onFailure (Throwable cause) {
                CProfile.log("Failed to update profile.", cause);
                MsoyUI.error(CProfile.serverError(cause));
            }
        });
    }

    protected FlexTable _content;
    protected boolean _editing = false;

    protected Profile _profile;
    protected Image _photo;
    protected Label _displayName, _headline, _lastOn, _permaName;

    protected RowPanel _buttons;
    protected Button _edit;

    protected Button _ephoto;
    protected TextBox _ename, _eheadline, _ehomepage;

    protected static SimpleDateFormat _lfmt = new SimpleDateFormat("MMM dd, yyyy");
}
