//
// $Id$

package client.account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.CharityInfo;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.AccountInfo;
import com.threerings.msoy.web.gwt.FacebookCreds;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.PromptPopup;
import client.ui.TongueBox;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

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

        _usersvc.getAccountInfo(new MsoyCallback<AccountInfo>() {
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
        add(new TongueBox(_msgs.editEPrefsHeader(), makeEmailPrefsSection()));
        add(new TongueBox(_msgs.editRealNameHeader(), makeRealNameSection()));
        add(new TongueBox(_msgs.editPasswordHeader(), makeChangePasswordSection()));
        add(new TongueBox(_msgs.charitiesHeader(), makeCharitySection()));
        add(new TongueBox(_msgs.fbconnectHeader(), makeFacebookConnectSection()));
    }

    protected Widget makePermanameSection ()
    {
        _perma = new SmartTable(0, 10);
        if (CShell.creds.permaName == null) {
            _perma.setText(0, 0, _msgs.editPermaName(), 1, "rightLabel");
            _pname = MsoyUI.createTextBox("", MemberName.MAXIMUM_PERMANAME_LENGTH, -1);
            _perma.setWidget(0, 1, _pname);
            _pname.addKeyboardListener(new DeferredKeyAdapter() {
                public void execute () {
                    validatePermaName();
                }
            });
            _uppname = new Button(_cmsgs.set(), new ClickListener() {
                public void onClick (Widget widget) {
                    configurePermaName();
                }
            });
            _uppname.setEnabled(false);
            _perma.setWidget(0, 2, _uppname);
            _perma.setHTML(1, 0, _msgs.editPermaNameTip(), 3, "Tip");

        } else {
            _perma.setText(0, 0, _msgs.editPermaName(), 1, "rightLabel");
            _perma.setText(0, 1, CShell.creds.permaName, 1, "PermaName");
        }
        return _perma;
    }

    protected Widget makeChangeEmailSection ()
    {
        SmartTable table = new SmartTable(0, 10);
        table.setText(0, 0, _msgs.editEmail(), 1, "rightLabel");
        table.setWidget(0, 1, _email = MsoyUI.createTextBox("", MemberName.MAX_EMAIL_LENGTH, -1));
        _email.setText(CShell.creds.accountName);
        _email.addKeyboardListener(new DeferredKeyAdapter() {
            public void execute () {
                validateEmail();
            }
        });
        _upemail = new Button(_cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updateEmail();
            }
        });
        _upemail.setEnabled(false);
        table.setWidget(0, 2, _upemail);
        return table;
    }

    protected Widget makeEmailPrefsSection ()
    {
        SmartTable table = new SmartTable(0, 10);
        table.setText(0, 0, _msgs.editWhirledMailEmail(), 1, "rightLabel");
        _whirledEmail = new CheckBox(_msgs.editWhirledMailEmailTip());
        table.setWidget(0, 1, _whirledEmail, 2, null);
        _whirledEmail.addStyleName("tipLabel");
        _whirledEmail.setChecked(_accountInfo.emailWhirledMail);

        table.setText(1, 0, _msgs.editAnnounceEmail(), 1, "rightLabel");
        table.setWidget(1, 1, _announceEmail = new CheckBox(_msgs.editAnnounceEmailTip()), 2, null);
        _announceEmail.addStyleName("tipLabel");
        _announceEmail.setChecked(_accountInfo.emailAnnouncements);

        _upeprefs = new Button(_cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updateEmailPrefs();
            }
        });
        table.setWidget(1, 2, _upeprefs);
        return table;
    }

    protected Widget makeRealNameSection ()
    {
        SmartTable table = new SmartTable(0, 10);
        table.setText(0, 0, _msgs.editRealName(), 1, "rightLabel");
        table.setWidget(0, 1, _rname = MsoyUI.createTextBox("", MemberName.MAX_REALNAME_LENGTH, -1));
        _rname.setText(_accountInfo.realName);
        _rname.addKeyboardListener(new DeferredKeyAdapter() {
            public void execute () {
                validateRealName();
            }
        });
        _uprname = new Button(_cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updateRealName();
            }
        });
        _uprname.setEnabled(false);
        table.setWidget(0, 2, _uprname);
        table.setHTML(1, 0, _msgs.editRealNameTip(), 3, "Tip");
        return table;
    }

    protected Widget makeChangePasswordSection ()
    {
        SmartTable table = new SmartTable(0, 10);
        table.setText(0, 0, _msgs.editPassword(), 1, "rightLabel");
        table.setWidget(0, 1, _password = new PasswordTextBox());
        _password.addKeyboardListener(new DeferredKeyAdapter() {
            public void execute () {
                validatePassword();
            }
        });
        _password.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _confirm.setFocus(true);
            }
        }));

        table.setText(1, 0, _msgs.editConfirm(), 1, "rightLabel");
        table.setWidget(1, 1, _confirm = new PasswordTextBox());
        _uppass = new Button(_cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updatePassword();
            }
        });
        _uppass.setEnabled(false);
        table.setWidget(1, 2, _uppass);
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
        charityTable.setText(1, 1, _msgs.defaultCharity(), 2, null);
        if (_accountInfo.charityMemberId == 0) {
            randomCharity.setChecked(true);
        }

        // Add charity info for each charity.
        int row = 2;
        Collections.sort(_accountInfo.charityNames, MemberName.BY_DISPLAY_NAME);
        for (MemberName name : _accountInfo.charityNames) {
            CharityInfo charity = _accountInfo.charities.get(name.getMemberId());
            MediaDesc photo = _accountInfo.charityPhotos.get(name.getMemberId());

            RadioButton charityButton = new RadioButton(CHARITY_RADIO_GROUP);
            if (_accountInfo.charityMemberId == name.getMemberId()) {
                charityButton.setChecked(true);
            }
            charityButtons.add(charityButton);
            charityTable.setWidget(row, 0, charityButton);
            charityTable.setWidget(
                row, 1, MediaUtil.createMediaView(photo, MediaDesc.THUMBNAIL_SIZE));
            charityTable.getFlexCellFormatter().setRowSpan(row, 1, 2);
            charityTable.getCellFormatter().setVerticalAlignment(
                row, 1, HasVerticalAlignment.ALIGN_TOP);
            charityTable.setText(row++, 2, name.getNormal());
            charityTable.setWidget(row++, 1,
                new HTML(charity.description), 1, "charityDescription");
        }

        charityTable.setWidget(row, 1, _upcharity = new Button(_cmsgs.update(), new ClickListener() {
            public void onClick (Widget sender) {
                // The index of the selected radio button in the list will be the index in the list
                // of charity names + 1 (the +1 for the random charity).
                int memberId = 0;
                for (int i = 1; i < charityButtons.size(); i++) {
                    if (charityButtons.get(i).isChecked()) {
                        memberId = _accountInfo.charityNames.get(i-1).getMemberId();
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
        table.setWidget(0, 0, MsoyUI.createHTML(_msgs.fbconnectWhy(), "Info"), 2, null);
        table.setText(1, 0, _msgs.fbconnectLink(), 1, "rightLabel");
        table.setWidget(1, 1, MsoyUI.createActionImage(FBCON_IMG, new ClickListener() {
            public void onClick (Widget sender) {
                // TODO: display a little circular "pending" icon; turn off clickability
                _fbconnect.requireSession(new MsoyCallback<String>() {
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
                CShell.creds.accountName = email;
                MsoyUI.infoNear(_msgs.emailUpdated(), _upemail);
            }
            public void onFailure (Throwable cause) {
                _upemail.setEnabled(true);
                MsoyUI.errorNear(CShell.serverError(cause), _upemail);
            }
        });
    }

    protected void updateEmailPrefs ()
    {
        _upeprefs.setEnabled(false);
        _usersvc.updateEmailPrefs(
            _whirledEmail.isChecked(), _announceEmail.isChecked(), new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
                _upeprefs.setEnabled(true);
                MsoyUI.infoNear(_msgs.eprefsUpdated(), _upeprefs);
            }
            public void onFailure (Throwable cause) {
                _upeprefs.setEnabled(true);
                MsoyUI.errorNear(CShell.serverError(cause), _upeprefs);
            }
        });
    }

    protected void updateCharity (final int newCharityId)
    {
        _upcharity.setEnabled(false);
        _usersvc.updateCharity(newCharityId, new MsoyCallback<Void>() {
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
                _perma.setText(0+1, 0, "");
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

    protected void validatePassword ()
    {
        String password = _password.getText().trim();
        _uppass.setEnabled(password.length() > 0);
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
        _usersvc.linkExternalAccount(creds, override, new MsoyCallback<Boolean>() {
            public void onSuccess (Boolean succeeded) {
                if (!succeeded) {
                    new PromptPopup(_msgs.fbconnectOverride(), new Command() {
                        public void execute () {
                            connectToFacebook(creds, true);
                        }
                    }).prompt();
                }
            }
        });
    }

    protected static abstract class DeferredKeyAdapter
        extends KeyboardListenerAdapter implements Command {
        public void onKeyPress (Widget sender, char keyCode, int modifiers) {
            // let the keypress go through, then validate our data
            DeferredCommand.addCommand(this);
        }
    };

    protected AccountInfo _accountInfo;
    protected SmartTable _perma;

    protected TextBox _email, _pname, _rname;
    protected CheckBox _whirledEmail, _announceEmail;
    protected PasswordTextBox _password, _confirm;
    protected Button _upemail, _upeprefs, _uppass, _uppname, _uprname, _upcharity;

    protected FBConnect _fbconnect = new FBConnect();

    protected static final String CHARITY_RADIO_GROUP = "selectCharity";
    protected static final String FBCON_IMG = "/images/account/fbconnect.png";

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final AccountMessages _msgs = GWT.create(AccountMessages.class);
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
