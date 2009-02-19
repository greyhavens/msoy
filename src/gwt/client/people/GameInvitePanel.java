//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
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
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import client.ui.BorderedPopup;
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
        gameInfo.setWidth("100%");
        ThumbBox thumbnail = new ThumbBox(
            detail.item.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE);
        thumbnail.addStyleName("thumbnail");
        gameInfo.setWidget(0, 0, thumbnail);
        gameInfo.setText(0, 1, _msgs.gameInviteIntro(detail.item.name), 1, "intro");
        add(gameInfo);

        // buttons to invoke the various ways to invite
        _methodButtons = new SmartTable();
        _methodButtons.setWidth("100%");
        addMethodButton("Email", new InviteMethodCreator () {
            public Widget create () {
                return new EmailPanel(message);
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
         */
        public EmailPanel (String defaultMessage)
        {
            setStyleName("email");
            setWidth("100%");

            final InvitePanel.InviteList addressList = new InvitePanel.InviteList();

            // create our two control sets for getting email addresses
            final WebMailControls webmail = new WebMailControls(addressList);
            final ManualControls manual = new ManualControls(addressList);

            int row = 0;

            // entry method, default to webmail
            setWidget(row, 0, webmail, 2, null);

            // mark the coordinates of the method widget, just for clarity
            final int methodRow = row++;
            final int methodColumn = 0;

            // the address list
            setWidget(row++, 0, addressList, 2, null);

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

            // message label
            setText(row, 0, "Message", 1, "biglabel");
            setText(row++, 1, "(optional)", 1, "labelparen");

            final TextArea message = MsoyUI.createTextArea(defaultMessage, 80, 4);
            message.setStyleName("message");
            message.addStyleName("input");
            setWidget(row++, 0, message, 2, null);

            PushButton send = MsoyUI.createButton("shortThin", "Send", new ClickListener() {
                public void onClick (Widget sender) {
                    send(addressList, message);
                }
            });
            setWidget(row, 0, send, 2, null);
            getFlexCellFormatter().setHorizontalAlignment(
                row++, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        }

        /**
         * Sends the invite to all the addresses added so far.
         */
        void send (InvitePanel.InviteList addressList, TextArea message)
        {
            // TODO
            Window.alert("send");
        }
    }

    /**
     * Shows controls for importing webmail contacts into an address list.
     */
    protected static class WebMailControls extends VerticalPanel
    {
        public WebMailControls (InvitePanel.InviteList addressList)
        {
            setWidth("100%");

            // labels line
            SmartTable row = new SmartTable(0, 5);
            row.setWidth("100%");

            int col = 0;
            row.setText(0, col++, "Import Webmail Contacts", 1, "biglabel");
            row.setWidget(0, col++, new Image(
                "/people/invite/webmail_providers_small_horizontal.png"));
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
            TextBox account = new TextBox();
            account.setStyleName("input");
            row.setWidget(0, col++, account);
            row.setText(0, col++, "Password", 1, "smalllabel");
            TextBox password = new TextBox();
            password.setStyleName("input");
            row.setWidget(0, col++, password);
            PushButton doimport = MsoyUI.createButton("shortThin", "Import", new ClickListener() {
                public void onClick (Widget sender) {
                    // TODO: add the addresses to the invite list
                    Window.alert("import");
                }
            });
            row.setWidget(0, col++, doimport);
            add(row);

            // privacy soother
            row = new SmartTable(0, 5);
            row.setWidth("100%");
            row.setText(0, 0, "We do not store these contacts and only use them for this " +
                "invitation. We will never spam your friends.", 1, "privacy");
            add(row);
        }
    }

    /**
     * Shows controls for manually adding addresses to an address list.
     */
    protected static class ManualControls extends VerticalPanel
    {
        public ManualControls (InvitePanel.InviteList addressList)
        {
            setWidth("100%");

            SmartTable row = new SmartTable(0, 5);
            row.setWidth("100%");
            row.setText(0, 0, "Enter Names and Addresses ", 1, "biglabel");
            add(row);

            row = new SmartTable(0, 5);
            row.setWidth("100%");

            int col = 0;
            row.setText(0, col++, "Name", 1, "smalllabel");
            TextBox name = new TextBox();
            name.setStyleName("input");
            row.setWidget(0, col++, name);
            row.setText(0, col++, "Address", 1, "smalllabel");
            TextBox address = new TextBox();
            address.setStyleName("input");
            row.setWidget(0, col++, address);
            PushButton add = MsoyUI.createButton("shortThin", "Add", new ClickListener() {
                public void onClick (Widget sender) {
                    // TODO: add the address to the invite list
                    Window.alert("add");
                }
            });
            row.setWidget(0, col++, add);
            add(row);
        }
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
}
