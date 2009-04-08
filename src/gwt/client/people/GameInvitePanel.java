//
// $Id$

package client.people;

import java.util.ArrayList;
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
import client.shell.ShellMessages;
import client.util.ClickCallback;
import client.util.InfoCallback;
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
        _gamesvc.loadGameDetail(gameId, new InfoCallback<GameDetail>() {
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
        final String url = Pages.makeAffiliateURL(CShell.getMemberId(), Pages.WORLD, iargs);

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
                                                   new InfoCallback<InvitationResults>() {
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
        // holding off on new features until after EC2 deployment
        public static final boolean FRESH_BLOOD = DeploymentConfig.devDeployment;

        /**
         * Creates a new friends panel.
         */
        public WhirledFriendsPanel (final String defaultMessage, final int gameId,
            final String gameName, final String args)
        {
            super(0, 5);
            _gameId = gameId;
            setStyleName("Whirled");
            setWidth("100%");

            int row = 0;

            // instructions
            setText(row++, 0, _msgs.gameInviteWhirledTitle(), 1, "Bold");
            setText(row++, 0, FRESH_BLOOD ? _msgs.gameInviteWhirledDetail(gameName) :
                _msgs.gameInviteWhirledDetail_Old());

            // buttons to select and deselect all friends
            SmartTable selectors = new SmartTable();
            selectors.setWidget(0, 0, new Button(_cmsgs.selectAll(), new ClickListener () {
                public void onClick (Widget source) {
                    selectAll(true);
                }
            }));
            selectors.setWidget(0, 1, new Button(_cmsgs.deselectAll(), new ClickListener () {
                public void onClick (Widget source) {
                    selectAll(false);
                }
            }));
            if (FRESH_BLOOD) {
                _showAll = new CheckBox(_msgs.gameInviteWhirledShowAll());
                selectors.setWidget(0, 2, _showAll);
                _showAll.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        downloadFriends();
                    }
                });
            }
            setWidget(row++, 0, selectors);

            // friends grid
            _grid = new SmartTable(0, 0);
            _grid.setWidth("100%");
            _grid.setText(0, 0, _msgs.gameInviteWhirledLoading());
            setWidget(row++, 0, _grid);

            // message label
            SmartTable messageLabel = new SmartTable();
            messageLabel.setText(0, 0, _msgs.gameInviteWhirledMessageLabel(), 1, "Bold");
            messageLabel.setText(0, 1, _msgs.gameInviteWhirledOptional(), 1, "labelparen");
            setWidget(row++, 0, messageLabel);

            // custom message
            final TextArea message = MsoyUI.createTextArea(defaultMessage, 80, 4);
            message.setStyleName("message");
            message.addStyleName("input");
            setWidget(row++, 0, message);

            // send button
            _send = MsoyUI.createButton("shortThin", _cmsgs.send(), null);
            new ClickCallback<Void>(_send) {
                public boolean callService () {
                    Set<Integer> recipients = getRecipients();
                    if (recipients.size() == 0) {
                        // this should not happen, but check anyway
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
                    MsoyUI.info(_msgs.gameInviteWhirledSent());
                    return true;
                }
            };
            _send.setEnabled(false);
            setWidget(row, 0, _send, 2, null);
            getFlexCellFormatter().setHorizontalAlignment(
                row++, 0, HasHorizontalAlignment.ALIGN_RIGHT);
            downloadFriends();
        }

        protected void downloadFriends ()
        {
            // pass a zero game id to include people who have already played
            int gameId = FRESH_BLOOD ? (_showAll.isChecked() ? 0 : _gameId) : 0;

            // get friend list from the server; fill in our grid
            _invitesvc.getFriends(gameId, ROWS * COLS, new InfoCallback<List<MemberCard>>() {
                public void onSuccess (List<MemberCard> result) {
                    _grid.clear();
                    for (int ii = 0; ii < result.size(); ++ii) {
                        SelectaFriend friend = new SelectaFriend(result.get(ii), _updateSendButton);
                        _friends.add(friend);
                        _grid.setWidget(ii / COLS, ii % COLS, friend);
                        _grid.getFlexCellFormatter().setWidth(ii / COLS, ii % COLS, COL_WIDTH);
                    }
                    if (_friends.size() == 0) {
                        _grid.setText(0, 0, _msgs.gameInviteWhirledNoFriends());
                    }
                }
            });
        }

        /**
         * Sets the check box state of all friends in the grid.
         */
        protected void selectAll (boolean select)
        {
            for (SelectaFriend friend : _friends) {
                friend.select(select);
            }
            new Timer() {
                public void run () {
                    updateSendButton();
                }
            }.schedule(1);
        }

        /**
         * Gets the recipients for a message.
         */
        protected Set<Integer> getRecipients ()
        {
            HashSet<Integer> selected = new HashSet<Integer>();
            for (SelectaFriend saf : _friends) {
                if (saf.isSelected()) {
                    selected.add(saf.getMemberId());
                }
            }
            return selected;
        }

        protected void updateSendButton ()
        {
            boolean enable = false;
            for (SelectaFriend saf : _friends) {
                if (saf.isSelected()) {
                    enable = true;
                    break;
                }
            }
            _send.setEnabled(enable);
        }

        protected ClickListener _updateSendButton = new ClickListener() {
            public void onClick (Widget sender) {
                updateSendButton();
            }
        };

        protected int _gameId;
        protected SmartTable _grid;
        protected PushButton _send;
        protected CheckBox _showAll;
        protected List<SelectaFriend> _friends = new ArrayList<SelectaFriend>();
        protected static final int ROWS = 12;
        protected static final int COLS = 3;
        protected static final String COL_WIDTH = "33%";
    }

    /**
     * Cell content for a friend.
     */
    protected static class SelectaFriend extends SmartTable
    {
        public SelectaFriend (MemberCard card, ClickListener listener)
        {
            super(0, 1);
            _card = card;
            setWidget(0, 0, _check = new CheckBox());
            setWidget(0, 1, new ThumbBox(card.photo, MediaDesc.QUARTER_THUMBNAIL_SIZE));
            setWidget(0, 2, MsoyUI.createLabel(card.name.toString(), "memberName"));
            getFlexCellFormatter().setWidth(0, 2, "100%");
            _check.addClickListener(listener);
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
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);

    protected static final String FBINVITE_WINDOW_NAME = "_whirled_fbinvite";
}
