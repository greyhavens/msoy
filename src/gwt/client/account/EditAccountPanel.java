//
// $Id$

package client.account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.CharityInfo;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.AccountInfo;
import com.threerings.msoy.web.gwt.ExternalSiteId;
import com.threerings.msoy.web.gwt.FacebookCreds;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;
import client.shell.FBConnect;
import client.shell.FullLogonPanel;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.PromptPopup;
import client.ui.TongueBox;
import client.util.BillingUtil;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.util.Link;
import client.util.MediaUtil;
import client.util.TextBoxUtil;

/**
 * Displays account information, allows twiddling.
 */
public class EditAccountPanel extends FlowPanel
{
    public EditAccountPanel ()
    {
        setStyleName("editAccount");

        if (CShell.getMemberId() == 0) {
            SmartTable table = new SmartTable(0, 10);
            table.setText(0, 0, _msgs.editMustLogon(), 1, "Header");
            table.setWidget(1, 0, new FullLogonPanel());
            add(new TongueBox(null, table));
            return;
        }

        _usersvc.getAccountInfo(new InfoCallback<AccountInfo>() {
            public void onSuccess (AccountInfo info) {
                init(info);
            }
        });
    }

    protected void init (final AccountInfo accountInfo)
    {
        _accountInfo = accountInfo;

        // add our myriad account configuration sections
        add(new TongueBox(null, makePermanameSection()));
        add(new TongueBox(_msgs.editEmailHeader(), makeChangeEmailSection()));
        add(new TongueBox(_msgs.editPrefsHeader(), makePrefsSection()));
        add(new TongueBox(_msgs.editSubscribeHeader(), makeSubscribeSection()));
        add(new TongueBox(_msgs.editRealNameHeader(), makeRealNameSection()));
        add(new TongueBox(_msgs.editPasswordHeader(), makeChangePasswordSection()));
        add(new TongueBox(_msgs.fbconnectHeader(), makeFacebookConnectSection()));
        add(new TongueBox(_msgs.charitiesHeader(), makeCharitySection()));
        add(new TongueBox(_msgs.deleteRequestHeader(), makeDeleteSection()));
    }

    protected void refresh ()
    {
        clear();
        init(_accountInfo);
    }

    protected Widget makePermanameSection ()
    {
        _perma = new SmartTable(0, 10);
        if (CShell.creds.permaName == null) {
            _perma.setText(0, 0, _msgs.editPermaName(), 1, "rightLabel");
            _pname = MsoyUI.createTextBox("", MemberName.MAXIMUM_PERMANAME_LENGTH, -1);
            _perma.setWidget(0, 1, _pname);
            _perma.getFlexCellFormatter().setWidth(0, 1, "10px");
            TextBoxUtil.addTypingListener(_pname, new Command() {
                public void execute () {
                    validatePermaName();
                }
            });
            _uppname = new Button(_cmsgs.set(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    configurePermaName();
                }
            });
            _uppname.setEnabled(false);
            _perma.setWidget(0, 2, _uppname);
            _perma.setHTML(1, 1, _msgs.editPermaNameTip(), 2, "Tip");

        } else {
            _perma.setText(0, 0, _msgs.editPermaName(), 1, "rightLabel");
            _perma.setText(0, 1, CShell.creds.permaName, 1, "PermaName");
        }
        return _perma;
    }

    protected Widget makeChangeEmailSection ()
    {
        SmartTable table = new SmartTable(0, 10);

        // the first row allows changing of their address
        table.setText(0, 0, _msgs.editEmail(), 1, "rightLabel");
        table.setWidget(0, 1, _email = MsoyUI.createTextBox("", MemberName.MAX_EMAIL_LENGTH, -1));
        table.getFlexCellFormatter().setWidth(0, 1, "10px");
        _email.setText(CShell.creds.accountName);
        TextBoxUtil.addTypingListener(_email, new Command() {
            public void execute () {
                validateEmail();
            }
        });
        table.setWidget(0, 2, _upemail = new Button(_cmsgs.update(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                updateEmail();
            }
        }));
        _upemail.setEnabled(false);

        // the second row informs of their validation status and allows resend of validation email
        table.setText(1, 0, _msgs.editEmailValid(), 1, "rightLabel");
        if (CShell.creds.validated) {
            table.setText(1, 1, _msgs.editEmailIsValid(), 2);
        } else {
            table.setText(1, 1, _msgs.editEmailNotValid(), 2, "Warning");
            table.setWidget(2, 2, _revalidate = new Button(_msgs.editEmailResend()));
            new ClickCallback<Void>(_revalidate) {
                protected boolean callService () {
                    _usersvc.resendValidationEmail(this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    MsoyUI.infoNear(_msgs.editEmailResent(CShell.creds.accountName), _revalidate);
                    return true;
                }
            };
        }
        return table;
    }

    protected Widget makePrefsSection ()
    {
        SmartTable table = new SmartTable(0, 10);
        table.setText(0, 0, _msgs.editWhirledMailEmail(), 1, "rightLabel");
        _whirledEmail = new CheckBox(_msgs.editWhirledMailEmailTip());
        table.setWidget(0, 1, _whirledEmail, 2);
        _whirledEmail.addStyleName("tipLabel");
        _whirledEmail.setValue(_accountInfo.emailWhirledMail);

        table.setText(1, 0, _msgs.editAnnounceEmail(), 1, "rightLabel");
        table.setWidget(1, 1, _announceEmail = new CheckBox(_msgs.editAnnounceEmailTip()), 2);
        _announceEmail.addStyleName("tipLabel");
        _announceEmail.setValue(_accountInfo.emailAnnouncements);

        table.setText(2, 0, _msgs.autoFlash(), 1, "rightLabel");
        table.setWidget(2, 1, _autoFlash = new CheckBox(_msgs.autoFlashTip()), 2);
        _autoFlash.addStyleName("tipLabel");
        _autoFlash.setValue(_accountInfo.autoFlash);

        _upprefs = new Button(_cmsgs.update(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                updatePrefs();
            }
        });
        table.setWidget(3, 1, _upprefs);
        return table;
    }

    protected Widget makeSubscribeSection ()
    {
        SmartTable table = new SmartTable(0, 10);
        if (CShell.isSubscriber()) {
            table.setWidget(0, 0, MsoyUI.createHTML(_msgs.editSubscribeIs(
                                                        BillingUtil.getAccountStatusPage()), null));
        } else {
            table.setText(0, 0, _msgs.editSubscribeIsnt());
            table.setWidget(0, 1, Link.create(_msgs.editSubscribeJoin(),
                                              Pages.BILLING, "subscribe"));
        }
        return table;
    }

    protected Widget makeRealNameSection ()
    {
        SmartTable table = new SmartTable(0, 10);
        table.setText(0, 0, _msgs.editRealName(), 1, "rightLabel");
        _rname = MsoyUI.createTextBox("", MemberName.MAX_REALNAME_LENGTH, -1);
        table.setWidget(0, 1, _rname);
        table.getFlexCellFormatter().setWidth(0, 1, "10px");
        _rname.setText(_accountInfo.realName);
        TextBoxUtil.addTypingListener(_rname, new Command() {
            public void execute () {
                validateRealName();
            }
        });
        _uprname = new Button(_cmsgs.update(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                updateRealName();
            }
        });
        _uprname.setEnabled(false);
        table.setWidget(0, 2, _uprname);
        table.setHTML(1, 1, _msgs.editRealNameTip(), 2, "Tip");
        return table;
    }

    protected Widget makeChangePasswordSection ()
    {
        SmartTable table = new SmartTable(0, 10);
        table.setText(0, 0, _msgs.editPassword(), 1, "rightLabel");
        table.setWidget(0, 1, _password = new PasswordTextBox());
        TextBoxUtil.addTypingListener(_password, new Command() {
            public void execute () {
                _uppass.setEnabled(_password.getText().trim().length() > 0);
            }
        });
        EnterClickAdapter.bind(_password, new ClickHandler() {
            public void onClick (ClickEvent event) {
                _confirm.setFocus(true);
            }
        });

        table.setText(1, 0, _msgs.editConfirm(), 1, "rightLabel");
        table.setWidget(1, 1, _confirm = new PasswordTextBox());
        _uppass = new Button(_cmsgs.update(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                updatePassword();
            }
        });
        _uppass.setEnabled(false);
        table.setWidget(1, 2, _uppass);
        table.setWidget(2, 1, MsoyUI.createHTML(_msgs.editChangePasswordTip(), null), 2, "Tip");
        return table;
    }

    protected Widget makeDeleteSection ()
    {
        SmartTable table = new SmartTable(0, 10);
        table.setText(0, 0, _msgs.deleteRequestTip(), 3, "Info");

        final Button send = new Button(_msgs.deleteRequestSend());
        table.setWidget(1, 0, send);
        new ClickCallback<Void>(send) {
            protected boolean callService () {
                _usersvc.requestAccountDeletion(this);
                MsoyUI.info(_msgs.deleteRequestSent());
                return true;
            }
            protected boolean gotResult (Void result) {
                // leave the button disable, no reason to send multiple delete requests
                return false;
            }
        };

        return table;
    }

    protected Widget makeCharitySection ()
    {
        final SmartTable charityTable = new SmartTable(0, 10);
        charityTable.setText(0, 0, _msgs.charities(), 3, "Info");

        final List<RadioButton> charityButtons =
            new ArrayList<RadioButton>(_accountInfo.charityNames.size() + 1);

        // Add random charity
        RadioButton randomCharity = new RadioButton(CHARITY_RADIO_GROUP);
        charityButtons.add(randomCharity);
        charityTable.setWidget(1, 0, randomCharity, 1, "rightLabel");
        charityTable.setText(1, 1, _msgs.defaultCharity(), 2);
        if (_accountInfo.charityMemberId == 0) {
            randomCharity.setValue(true);
        }

        // Add charity info for each charity.
        int row = 2;
        Collections.sort(_accountInfo.charityNames, MemberName.BY_DISPLAY_NAME);
        for (MemberName name : _accountInfo.charityNames) {
            CharityInfo charity = _accountInfo.charities.get(name.getId());
            MediaDesc photo = _accountInfo.charityPhotos.get(name.getId());

            RadioButton charityButton = new RadioButton(CHARITY_RADIO_GROUP);
            if (_accountInfo.charityMemberId == name.getId()) {
                charityButton.setValue(true);
            }
            charityButtons.add(charityButton);
            charityTable.setWidget(row, 0, charityButton);
            charityTable.setWidget(
                row, 1, MediaUtil.createMediaView(photo, MediaDescSize.THUMBNAIL_SIZE));
            charityTable.getFlexCellFormatter().setRowSpan(row, 1, 2);
            charityTable.getCellFormatter().setVerticalAlignment(row, 1, HasAlignment.ALIGN_TOP);
            charityTable.setText(row++, 2, name.getNormal());
            charityTable.setWidget(row++, 1,
                new HTML(charity.description), 1, "charityDescription");
        }

        charityTable.setWidget(row, 1,
            _upcharity = new Button(_cmsgs.update(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                // The index of the selected radio button in the list will be the index in the list
                // of charity names + 1 (the +1 for the random charity).
                int memberId = 0;
                for (int i = 1; i < charityButtons.size(); i++) {
                    if (charityButtons.get(i).getValue()) {
                        memberId = _accountInfo.charityNames.get(i-1).getId();
                        break;
                    }
                }
                updateCharity(memberId);
            }
        }));
        return charityTable;
    }

    protected Widget makeFacebookConnectSection ()
    {
        SmartTable table = new SmartTable(0, 10);
        if (_accountInfo.hasAuther(ExternalSiteId.Auther.FACEBOOK)) {
            table.setText(0, 0, _msgs.fbconnectActive());
            return table;
        }
        table.setWidget(0, 0, MsoyUI.createHTML(_msgs.fbconnectWhy(), "Info"), 2);
        table.setText(1, 0, _msgs.fbconnectLink(), 1, "rightLabel");
        table.setWidget(1, 1, MsoyUI.createActionImage(FBCON_IMG, new ClickHandler() {
            public void onClick (ClickEvent event) {
                // TODO: display a little circular "pending" icon; turn off clickability
                _fbconnect.requireSession(new InfoCallback<String>() {
                    public void onSuccess (String uid) {
                        connectToFacebook(FBConnect.readCreds(), false);
                    }
                });
            }
        }), 1, "FBLink");
        return table;
    }

    protected void updateRealName ()
    {
        final String oldRealName = _accountInfo.realName;
        _accountInfo.realName = _rname.getText().trim();
        _uprname.setEnabled(false);
        _rname.setEnabled(false);
        _usersvc.updateAccountInfo(_accountInfo, new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
                _rname.setEnabled(true);
                _uprname.setEnabled(false);
                MsoyUI.infoNear(_msgs.realNameUpdated(), _uprname);
            }
            public void onFailure (Throwable cause) {
                _rname.setText(_accountInfo.realName = oldRealName);
                _rname.setEnabled(true);
                _uprname.setEnabled(true);
                MsoyUI.errorNear(CShell.serverError(cause), _uprname);
            }
        });
    }

    protected void updateEmail ()
    {
        final String email = _email.getText().trim();
        _upemail.setEnabled(false);
        _usersvc.updateEmail(email, new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
                MsoyUI.infoNear(_msgs.emailUpdated(), _upemail);
                CShell.frame.emailUpdated(email, false);
                refresh();
            }
            public void onFailure (Throwable cause) {
                _upemail.setEnabled(true);
                MsoyUI.errorNear(CShell.serverError(cause), _upemail);
            }
        });
    }

    protected void updatePrefs ()
    {
        _upprefs.setEnabled(false);
        _usersvc.updatePrefs(
            _whirledEmail.getValue(), _announceEmail.getValue(), _autoFlash.getValue(),
            new AsyncCallback<Void>() {
                public void onSuccess (Void result) {
                    _upprefs.setEnabled(true);
                    MsoyUI.infoNear(_msgs.prefsUpdated(), _upprefs);
                }
                public void onFailure (Throwable cause) {
                    _upprefs.setEnabled(true);
                    MsoyUI.errorNear(CShell.serverError(cause), _upprefs);
                }
            });
    }

    protected void updateCharity (final int newCharityId)
    {
        _upcharity.setEnabled(false);
        _usersvc.updateCharity(newCharityId, new InfoCallback<Void>() {
            public void onSuccess (Void result) {
                MsoyUI.info(_msgs.echarityUpdated());
            }
        });
    }

    protected void updatePassword ()
    {
        String password = _password.getText().trim(), confirm = _confirm.getText().trim();
        if (confirm.length() == 0) {
            MsoyUI.errorNear(_msgs.editMissingConfirm(), _uppass);
            return;
        } else if (!password.equals(confirm)) {
            MsoyUI.errorNear(_msgs.editPasswordMismatch(), _uppass);
            return;
        }

        _uppass.setEnabled(false);
        _password.setEnabled(false);
        _confirm.setEnabled(false);
        _usersvc.updatePassword(CShell.frame.md5hex(password), new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
                _password.setText("");
                _password.setEnabled(true);
                _confirm.setText("");
                _confirm.setEnabled(true);
                MsoyUI.infoNear(_msgs.passwordUpdated(), _uppass);
            }
            public void onFailure (Throwable cause) {
                _password.setEnabled(true);
                _confirm.setEnabled(true);
                _uppass.setEnabled(true);
                MsoyUI.errorNear(CShell.serverError(cause), _uppass);
            }
        });
    }

    protected void configurePermaName ()
    {
        final String pname = _pname.getText().trim();

        // now check it for legality
        for (int ii = 0; ii < pname.length(); ii++) {
            char c = pname.charAt(ii);
            if ((ii == 0 && !Character.isLetter(c)) ||
                (!Character.isLetter(c) && !Character.isDigit(c) && c != '_')) {
                MsoyUI.errorNear(_msgs.editPermaInvalid(), _uppname);
                return;
            }
        }
        if (pname.length() < MemberName.MINIMUM_PERMANAME_LENGTH) {
            MsoyUI.errorNear(_msgs.editPermaShort(), _uppname);
        } else if (pname.length() > MemberName.MAXIMUM_PERMANAME_LENGTH) {
            MsoyUI.errorNear(_msgs.editPermaLong(), _uppname);
        }

        _uppname.setEnabled(false);
        _pname.setEnabled(false);
        _usersvc.configurePermaName(pname, new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
                CShell.creds.permaName = pname;
                _perma.getFlexCellFormatter().setStyleName(0, 1, "PermaName");
                _perma.setText(0, 1, pname);
                _perma.setText(0, 2, "");
                _perma.setText(1, 0, "");
                MsoyUI.infoNear(_msgs.permaNameConfigured(), _uppname);
            }
            public void onFailure (Throwable cause) {
                _pname.setEnabled(true);
                _uppname.setEnabled(true);
                MsoyUI.errorNear(CShell.serverError(cause), _uppname);
            }
        });
    }

    protected void validateRealName ()
    {
        String realName = _rname.getText().trim();
        _uprname.setEnabled(!_accountInfo.realName.equals(realName));
    }

    protected void validateEmail ()
    {
        String email = _email.getText().trim();
        _upemail.setEnabled(!(email.length() < 4 || email.indexOf("@") == -1 ||
                              email.equals(CShell.creds.accountName)));
    }

    protected void validatePermaName ()
    {
        // extract the permaname, but also show the user exactly what we're using, since we
        // lowercase and trim it
        int cursor = _pname.getCursorPos();
        String raw = _pname.getText();
        String pname = raw.trim();
        cursor -= raw.indexOf(pname);
        pname = pname.toLowerCase();
        _pname.setText(pname);
        _pname.setCursorPos(cursor);
        _uppname.setEnabled(pname.length() > 0);
    }

    protected void connectToFacebook (final FacebookCreds creds, boolean override)
    {
        _usersvc.linkExternalAccount(creds, override, new InfoCallback<Boolean>() {
            public void onSuccess (Boolean succeeded) {
                if (succeeded) {
                    MsoyUI.info(_msgs.fbconnectSuccess());
                    return;
                }
                new PromptPopup(_msgs.fbconnectOverride(), new Command() {
                    public void execute () {
                        connectToFacebook(creds, true);
                    }
                }).prompt();
            }
        });
    }

    protected AccountInfo _accountInfo;
    protected SmartTable _perma;

    protected TextBox _email, _pname, _rname;
    protected CheckBox _whirledEmail, _announceEmail, _delconf, _autoFlash;
    protected PasswordTextBox _password, _confirm, _delpass;
    protected Button _upemail, _upprefs, _uppass, _uppname, _uprname, _upcharity, _revalidate;

    protected FBConnect _fbconnect = new FBConnect();

    protected static final String CHARITY_RADIO_GROUP = "selectCharity";
    protected static final String FBCON_IMG = "/images/account/fbconnect.png";

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final AccountMessages _msgs = GWT.create(AccountMessages.class);
    protected static final WebUserServiceAsync _usersvc = GWT.create(WebUserService.class);
}
