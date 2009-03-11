//
// $Id$

package client.people;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextArea;
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
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.util.ClickCallback;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import client.ui.MsoyUI;
import client.ui.NowLoadingWidget;
import client.ui.ThumbBox;

/**
 * Panel for inviting friends to come and play a game, usually issued by a game API call that
 * includes a token and message.
 */
public class GameInvitePanel extends InvitePanel
{
    /**
     * Create a new invite panel with arguments from the game API. The arguments are assembled in
     * <code>as/com.threerings.msoy.game.client.GameDirector::viewSharePage</code>.
     */
    public GameInvitePanel (final Args args)
    {
        addStyleName("gameInvite");

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
        final String iargs = (gameType == 1) ? 
            Args.compose("game", "j", detail.gameId, CShell.getMemberId(), token, roomId) :
            Args.compose("game", "j", detail.gameId, CShell.getMemberId(), token);
        final String url = Pages.makeURL(Pages.WORLD, iargs);

        // game information
        SmartTable info = new SmartTable();
        info.setWidth("100%");
        info.setStyleName("gameInfo");
        info.setWidget(0, 0, new ThumbBox(detail.item.getThumbnailMedia(),
                                          MediaDesc.THUMBNAIL_SIZE), 1, "thumbnail");
        info.getFlexCellFormatter().setRowSpan(0, 0, 2);
        info.setText(0, 1, _msgs.gameInviteIntro(detail.item.name), 1, "Intro");
        info.setText(1, 0, _msgs.gameInviteTip(), 1, null);
        add(info);

        // buttons to invoke the various ways to invite
        addMethodButton("Email", new InviteMethodCreator() {
            public Widget create () {
                EmailListPanel panel = new EmailListPanel(false) {
                    protected void handleSend (
                        String from, String msg, final List<EmailContact> addrs) {
                        _invitesvc.sendGameInvites(addrs, detail.gameId, from, url, msg,
                                                   new MsoyCallback<InvitationResults>() {
                            public void onSuccess (InvitationResults ir) {
                                _addressList.clear();
                                // show the results (once we implement sending invites to
                                // registered users, it will just serve as an error dialog)
                                InviteUtils.showInviteResults(addrs, ir);
                            }
                        });
                    }
                };
                panel.setDefaultMessage(message);
                return panel;
            }
        });
        addMethodButton("IM", new InviteMethodCreator() {
            public Widget create () {
                return new IMPanel(url);
            }
        });
        addMethodButton("Whirled", new InviteMethodCreator() {
            public Widget create () {
                return new WhirledFriendsPanel(message, detail.gameId, detail.item.name, iargs);
            }
        });
        addMethodButton("Facebook", new InviteMethodCreator() {
            public Widget create () {
                showFBInvitePopup(detail.item.gameId, message, iargs);
                return null;
            }
        });
        addMethodButtons();
    }

    protected static void showFBInvitePopup (int gameId, String message, String acceptPath)
    {
        // TODO: pass along the default message too. This is complicated because the servlet must
        // convert it to javascript
        String popupURL = "/fbinvite/do?gameId=" + gameId + "&path=" + acceptPath;
        Window.open(popupURL, FBINVITE_WINDOW_NAME,
            "location=no,status=no,width=655,height=600,left=50,top=50");
    }

    /**
     * Invite method showing the in-Whirled friends of the user with check boxes.
     */
    protected static class WhirledFriendsPanel extends SmartTable
    {
        /**
         * Creates a new friends panel.
         */
        public WhirledFriendsPanel (final String defaultMessage, final int gameId,
            final String gameName, final String args)
        {
            super(0, 5);
            setStyleName("Whirled");
            setWidth("100%");

            int row = 0;

            // instructions
            setText(row++, 0, "Select the friends you want to invite", 1, "Bold");
            setText(row++, 0, "These are your most recently online friends. Select the ones you " +
                "want to invite by checking the boxes next to their names. Then click Send to " +
                "send a Whirled mail message to each selected friend.");

            // buttons to select and deselect all friends
            SmartTable selectors = new SmartTable();
            selectors.setWidget(0, 0, new Button("Select All", new ClickListener () {
                public void onClick (Widget source) {
                    selectAll(true);
                }
            }));
            selectors.setWidget(0, 1, new Button("Deselect All", new ClickListener () {
                public void onClick (Widget source) {
                    selectAll(false);
                }
            }));
            setWidget(row++, 0, selectors);

            // friends grid
            _grid = new SmartTable(0, 0);
            _grid.setWidth("100%");
            _grid.setText(0, 0, "Loading friends...");
            setWidget(row++, 0, _grid);

            // message label
            SmartTable messageLabel = new SmartTable();
            messageLabel.setText(0, 0, "Message", 1, "Bold");
            messageLabel.setText(0, 1, "(optional)", 1, "labelparen");
            setWidget(row++, 0, messageLabel);

            // custom message
            final TextArea message = MsoyUI.createTextArea(defaultMessage, 80, 4);
            message.setStyleName("message");
            message.addStyleName("input");
            setWidget(row++, 0, message);

            // send button
            PushButton send = MsoyUI.createButton("shortThin", "Send", null);
            new ClickCallback<Void>(send) {
                public boolean callService () {
                    Set<Integer> recipients = getRecipients();
                    if (recipients == null) {
                        return false;
                    }
                    String inviter = CShell.creds.name.toString();
                    String body = message.getText().length() == 0 ?
                        _msgs.gameInviteWhirledMailBodyNoMessage(inviter, gameName) :
                        _msgs.gameInviteWhirledMailBody(inviter, gameName, message.getText());
                    _invitesvc.sendWhirledMailGameInvites(recipients, gameId,
                        _msgs.gameInviteWhirledMailSubject(gameName), body, args, this);
                    return true;
                }

                public boolean gotResult (Void result) {
                    selectAll(false);
                    MsoyUI.info("Messages sent.");
                    return true;
                }
            };
            setWidget(row, 0, send, 2, null);
            getFlexCellFormatter().setHorizontalAlignment(
                row++, 0, HasHorizontalAlignment.ALIGN_RIGHT);

            // get friend list from the server; fill in our grid
            _invitesvc.getFriends(ROWS * COLS, new MsoyCallback<List<MemberCard>>() {
                public void onSuccess (List<MemberCard> result) {
                    _grid.clear();
                    for (int ii = 0; ii < result.size(); ++ii) {
                        SelectaFriend friend = new SelectaFriend(result.get(ii));
                        _grid.setWidget(ii / COLS, ii % COLS, friend);
                        _grid.getFlexCellFormatter().setWidth(ii / COLS, ii % COLS, COL_WIDTH);
                    }
                    _gridFilled = result.size() != 0;
                    if (!_gridFilled) {
                        _grid.setText(0, 0, "No Whirled friends found!");
                    }
                }
            });
        }

        /**
         * Sets the check box state of all friends in the grid.
         */
        protected void selectAll (boolean select)
        {
            if (!_gridFilled) {
                return;
            }
            for (int row = 0; row < _grid.getRowCount(); ++row) {
                for (int col = 0; col < _grid.getCellCount(row); ++col) {
                    ((SelectaFriend)_grid.getWidget(row, col)).select(select);
                }
            }
        }

        /**
         * Gets the recipients for a message. If there are no recipients, shows an error and
         * returns null.
         */
        protected Set<Integer> getRecipients ()
        {
            if (!_gridFilled) {
                MsoyUI.error("There are no friends to send to.");
                return null;
            }
            HashSet<Integer> selected = new HashSet<Integer>();
            for (int row = 0; row < _grid.getRowCount(); ++row) {
                for (int col = 0; col < _grid.getCellCount(row); ++col) {
                    SelectaFriend saf = (SelectaFriend)_grid.getWidget(row, col);
                    if (saf.isSelected()) {
                        selected.add(saf.getMemberId());
                    }
                }
            }
            if (selected.size() == 0) {
                MsoyUI.error("Select one or more friends by clicking the check boxes.");
                return null;
            }
            return selected;
        }

        protected SmartTable _grid;
        protected boolean _gridFilled;
        protected static final int ROWS = 12;
        protected static final int COLS = 3;
        protected static final String COL_WIDTH = "33%";
    }

    /**
     * Cell content for a friend.
     */
    protected static class SelectaFriend extends SmartTable
    {
        public SelectaFriend (MemberCard card)
        {
            super(0, 1);
            _card = card;
            setWidget(0, 0, _check = new CheckBox());
            setWidget(0, 1, new ThumbBox(card.photo, MediaDesc.QUARTER_THUMBNAIL_SIZE));
            setWidget(0, 2, MsoyUI.createLabel(card.name.toString(), "memberName"));
            getFlexCellFormatter().setWidth(0, 2, "100%");
        }

        public int getMemberId ()
        {
            return _card.name.getMemberId();
        }

        public boolean isSelected ()
        {
            return _check.isChecked();
        }

        public void select (boolean sel)
        {
            _check.setChecked(sel);
        }

        protected CheckBox _check;
        protected MemberCard _card;
    }

    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
    protected static final InviteServiceAsync _invitesvc = (InviteServiceAsync)
        ServiceUtil.bind(GWT.create(InviteService.class), InviteService.ENTRY_POINT);

    protected static final String FBINVITE_WINDOW_NAME = "_whirled_fbinvite";
}
