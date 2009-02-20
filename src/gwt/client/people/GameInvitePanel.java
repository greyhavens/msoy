//
// $Id$

package client.people;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameDetail;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.person.gwt.InvitationResults;
import com.threerings.msoy.person.gwt.InviteService;
import com.threerings.msoy.person.gwt.InviteServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.EmailContact;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import client.ui.BorderedPopup;
import client.ui.DefaultTextListener;
import client.ui.MsoyUI;
import client.ui.NowLoadingWidget;
import client.ui.ThumbBox;

/**
 * Panel for inviting friends to come and play a game, usually issued by a game API call that
 * includes a token and message.
 * TODO: i18n
 */
public class GameInvitePanel extends VerticalPanel
{
    /**
     * Create a new invite panel with arguments from the game API. The arguments are assembled in
     * <code>as/com.threerings.msoy.game.client.GameDirector::viewSharePage</code>.
     */
    public GameInvitePanel (final Args args)
    {
        setStyleName("gameInvitePanel");
        setWidth("100%");
        setSpacing(10);

        int gameId = args.get(2, 0);

        // show the loading widget
        final NowLoadingWidget loading = new NowLoadingWidget();
        loading.center();

        // load the game detail, then initialize
        _gamesvc.loadGameDetail(gameId, new MsoyCallback<GameDetail>() {
            public void onSuccess (GameDetail result) {
                init(loading, args, result);
            }
        });
    }

    /**
     * Sets up the invite panel after the game detail is available.
     */
    protected void init (final NowLoadingWidget loading, final Args args, final GameDetail detail)
    {
        // update the loading widget
        if (loading != null) {
            loading.finishing(new Timer() {
                public void run () {
                    init(null, args, detail);
                    loading.hide();
                }
            });
            return;
        }

        // extract arguments
        final String message = args.get(3, "");
        String token = args.get(4, "");
        int gameType = args.get(5, 0);
        int roomId = args.get(6, 0);

        // build the invite url; this will be a play now link for lobbied games (type 0) or a
        // start-in-room link for avrgs (type 1)
        String pathToGame = Pages.makeLink(Pages.WORLD, gameType == 1 ? 
            Args.compose("game", "s", detail.gameId, CShell.getMemberId(), token, roomId) :
            Args.compose("game", "t", detail.gameId, CShell.getMemberId(), token));
        final String url = DeploymentConfig.serverURL + pathToGame.substring(1);

        // game information
        SmartTable gameInfo = new SmartTable();
        gameInfo.setWidth("100%");
        gameInfo.setStyleName("gameInfo");
        ThumbBox thumbnail = new ThumbBox(
            detail.item.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE);
        gameInfo.setWidget(0, 0, thumbnail, 1, "thumbnail");
        gameInfo.getFlexCellFormatter().setHorizontalAlignment(
            0, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        gameInfo.setText(0, 1, _msgs.gameInviteIntro(detail.item.name), 1, "intro");
        add(gameInfo);

        // buttons to invoke the various ways to invite
        _methodButtons = new SmartTable();
        _methodButtons.setWidth("100%");
        addMethodButton("Email", new InviteMethodCreator () {
            public Widget create () {
                return new EmailPanel(message, detail.gameId, url);
            }
        });
        addMethodButton("IM",
            new InviteMethodCreator () {
                public Widget create () {
                    return new IMPanel(url);
                }
            });
        add(_methodButtons);

        // method panel will on the bottom row
        _methodRow = this.getWidgetCount();
    }

    /**
     * Adds a button to the button bar that invokes the given creator on click.
     */
    protected void addMethodButton (String label, final InviteMethodCreator creator)
    {
        PushButton button = MsoyUI.createButton(MsoyUI.LONG_THICK, label, new ClickListener() {
            public void onClick (Widget sender) {
                setMethod(creator.create()); //new SendURLPanel();
            }
        });
        button.addStyleName("methodButton");
        int col = _methodButtons.getRowCount() == 0 ? 0 : _methodButtons.getCellCount(0);
        _methodButtons.setWidget(0, col, button);
        _methodButtons.getFlexCellFormatter().setHorizontalAlignment(
            0, col, HasHorizontalAlignment.ALIGN_CENTER);
    }

    /**
     * Removes the most recent invite method, if any and sets this to be the new invite method.
     */
    protected void setMethod (Widget panel)
    {
        if (getWidgetCount() > _methodRow) {
            remove(_methodRow);
        }
        if (panel != null) {
            add(panel);
        }
    }

    /**
     * Allows various invite methods to be hooked up to click listeners.
     */
    interface InviteMethodCreator
    {
        /**
         * Creates the widget that will display this invite method.
         */
        Widget create ();
    }

    /**
     * Invite method consisting of a panel with lots of email controls.
     */
    protected static class EmailPanel extends SmartTable
    {
        /**
         * Creates a new email panel.
         * TODO: support a more advanced address list that shows whether each of your contacts is
         * a non-member, a member non-friend or a friend. 
         */
        public EmailPanel (String defaultMessage, int gameId, String url)
        {
            _gameId = gameId;
            _url = url;
            setStyleName("email");
            setWidth("100%");

            _addressList = new InviteList();

            // create our two control sets for getting email addresses
            final WebMailControls webmail = new WebMailControls(_addressList);
            final ManualControls manual = new ManualControls(_addressList);

            int row = 0;

            // entry method, default to webmail
            setWidget(row, 0, webmail, 2, null);

            // mark the coordinates of the method widget, just for clarity
            final int methodRow = row++;
            final int methodColumn = 0;

            // the address list
            setWidget(row++, 0, _addressList, 2, null);

            // method toggle
            Label toggle = MsoyUI.createActionLabel("Enter More Addresses...", "toggle",
                new ClickListener () {
                    public void onClick (Widget sender) {
                        if (getWidget(methodRow, methodColumn) == webmail) {
                            setWidget(methodRow, methodColumn, manual, 2, null);
                            ((Label)sender).setText("Import From Webmail...");
    
                        } else {
                            setWidget(methodRow, methodColumn, webmail, 2, null);
                            ((Label)sender).setText("Enter More Addresses...");
                        }
                    }
                });
            setWidget(row++, 0, toggle, 2, "toggle");

            // from
            setText(row, 0, "From:", 1, "biglabel");
            _from = MsoyUI.createTextBox(
                CShell.creds.name.toString(), InviteUtils.MAX_NAME_LENGTH, 25);
            _from.addStyleName("input");
            setWidget(row++, 1, _from);

            // message label
            setText(row, 0, "Message", 1, "biglabel");
            setText(row++, 1, "(optional)", 1, "labelparen");

            _message = MsoyUI.createTextArea(defaultMessage, 80, 4);
            _message.setStyleName("message");
            _message.addStyleName("input");
            setWidget(row++, 0, _message, 2, null);

            PushButton send = MsoyUI.createButton("shortThin", "Send", new ClickListener() {
                public void onClick (Widget sender) {
                    send();
                }
            });
            setWidget(row, 0, send, 2, null);
            getFlexCellFormatter().setHorizontalAlignment(
                row++, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        }

        /**
         * Sends the invite to all the addresses added so far.
         */
        protected void send ()
        {
            final List<EmailContact> contacts = InviteUtils.getValidUniqueAddresses(_addressList);

            String from = _from.getText().trim();
            if (from.length() == 0) {
                MsoyUI.error(_msgs.inviteEmptyFromField());
                _from.setFocus(true);
                return;
            }
            String msg = _message.getText().trim();
            _invitesvc.sendGameInvites(contacts, _gameId, from, _url, msg,
                new MsoyCallback<InvitationResults>() {
                    public void onSuccess (InvitationResults ir) {
                        _addressList.clear();
                        // show the results (once we implement sending invites to registered users,
                        // it will just serve as an error dialog)
                        InviteUtils.showInviteResults(contacts, ir);
                    }
                });
        }

        int _gameId;
        String _url;
        InviteList _addressList;
        TextArea _message;
        TextBox _from;
    }

    /**
     * Shows controls for importing webmail contacts into an address list.
     */
    protected static class WebMailControls extends VerticalPanel
    {
        public WebMailControls (InviteList addressList)
        {
            setWidth("100%");

            // labels line
            SmartTable row = new SmartTable(0, 5);
            row.setWidth("100%");

            int col = 0;
            row.setText(0, col++, "Import Webmail Contacts", 1, "biglabel");
            row.setWidget(0, col++, new Image(
                "/images/people/invite/webmail_providers_small_horizontal.png"));
            Widget showSupported = MsoyUI.createActionLabel("Show Supported Accounts",
                "ImportSupportLink", new ClickListener() {
                    public void onClick (Widget widget) {
                        BorderedPopup popup = new BorderedPopup(true);
                        popup.setWidget(MsoyUI.createHTML(
                            _msgs.inviteSupportedList(), "importSupportList"));
                        popup.show();
                    }
                });
            row.setWidget(0, col, showSupported);
            row.getFlexCellFormatter().setHorizontalAlignment(0, col++,
                HasHorizontalAlignment.ALIGN_RIGHT);
            add(row);

            // account entry line
            row = new SmartTable(0, 5);
            row.setWidth("100%");

            col = 0;
            row.setText(0, col++, "Account", 1, "smalllabel");
            TextBox account = MsoyUI.createTextBox("", InviteUtils.MAX_MAIL_LENGTH, 25);
            account.setStyleName("input");
            DefaultTextListener.configure(account, _msgs.inviteWebAddress());
            row.setWidget(0, col++, account);
            row.setText(0, col++, "Password", 1, "smalllabel");
            TextBox password = new PasswordTextBox();
            password.setStyleName("input");
            row.setWidget(0, col++, password);
            PushButton doimport = MsoyUI.createButton("shortThin", "Import", null);
            new InviteUtils.WebmailImporter(doimport, account, password, addressList, false);
            row.setWidget(0, col++, doimport);
            add(row);

            // privacy soother
            row = new SmartTable(0, 5);
            row.setWidth("100%");
            row.setText(0, 0, "We will not store any of your contacts and will only use them " +
                "for this invitation. We will never spam your friends.", 1, "privacy");
            add(row);
        }
    }

    /**
     * Shows controls for manually adding addresses to an address list.
     */
    protected static class ManualControls extends VerticalPanel
    {
        public ManualControls (InviteList addressList)
        {
            _list = addressList;

            setWidth("100%");

            SmartTable row = new SmartTable(0, 5);
            row.setWidth("100%");
            row.setText(0, 0, "Enter Names and Addresses ", 1, "biglabel");
            add(row);

            row = new SmartTable(0, 5);
            row.setWidth("100%");

            int col = 0;
            row.setText(0, col++, "Name", 1, "smalllabel");
            _name = MsoyUI.createTextBox("", InviteUtils.MAX_NAME_LENGTH, 25);
            _name.setStyleName("input");
            DefaultTextListener.configure(_name, _msgs.inviteFriendName());
            row.setWidget(0, col++, _name);
            row.setText(0, col++, "Address", 1, "smalllabel");
            _address = MsoyUI.createTextBox("", InviteUtils.MAX_MAIL_LENGTH, 25);
            _address.setStyleName("input");
            DefaultTextListener.configure(_address, _msgs.inviteFriendEmail());
            row.setWidget(0, col++, _address);
            PushButton add = MsoyUI.createButton("shortThin", "Add", new ClickListener() {
                public void onClick (Widget sender) {
                    InviteUtils.addEmailIfValid(_name, _address, _list);
                }
            });
            row.setWidget(0, col++, add);
            add(row);
        }

        InviteList _list;
        TextBox _name;
        TextBox _address;
    }

    /**
     * Invite method consisting of a text area to copy a URL from.
     */
    protected static class IMPanel extends SmartTable
    {
        public IMPanel (String url)
        {
            setStyleName("im");
            setWidth("100%");

            // basic instructions
            setText(0, 0, "Copy This URL", 1, "biglabel");

            // a little detail
            setText(1, 0, "Paste it somewhere your friends can open it and come join you in " +
                "Whirled.", 1, "intro");

            // link
            TextArea link = MsoyUI.createTextArea(url, 60, 3);
            MsoyUI.selectAllOnFocus(link);
            link.setStyleName("urlBox");
            setWidget(2, 0, link);
        }
    }

    /** The row where the invite method is. */
    protected int _methodRow;

    /** The buttons for the various invite methods. */
    SmartTable _methodButtons;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
    protected static final InviteServiceAsync _invitesvc = (InviteServiceAsync)
        ServiceUtil.bind(GWT.create(InviteService.class), InviteService.ENTRY_POINT);
}
