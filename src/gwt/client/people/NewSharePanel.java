//
// $Id$

package client.people;

import java.util.ArrayList;
import java.util.List;

import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.Page;
import client.shell.ShellMessages;
import client.ui.CreatorLabel;
import client.ui.MsoyUI;
import client.ui.RollupBox;
import client.ui.ThumbBox;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameDetail;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.person.gwt.InviteService;
import com.threerings.msoy.person.gwt.InviteServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.EmailContact;
import com.threerings.msoy.web.gwt.Pages;

public class NewSharePanel extends VerticalPanel
{
    public NewSharePanel (PeoplePage page)
    {
        setStylePrimaryName("sharePanel-wrapper");
        VerticalPanel subPanel = new VerticalPanel();
        subPanel.setStylePrimaryName("sharePanel");
        SimplePanel banner = new SimplePanel();
        banner.setStylePrimaryName("sharePanel-banner");
        add(banner);
        add(subPanel);

        // Top area
        final HorizontalPanel topPanel = new HorizontalPanel();
        topPanel.setStylePrimaryName("sharePanel-top");
        VerticalPanel leftSide = new VerticalPanel();
        leftSide.add(MsoyUI.createHTML("If your friends join Whirled through this " +
        		"link, you can <a class='external' href='http://wiki.whirled.com/Affiliate'>earn " +
        		"money</a> from them!  You can copy, paste, and IM it to your buddies!",
        		"sharePanel-url-description"));
        leftSide.add(MsoyUI.createLabel("Copy and paste this code to embed your home " +
        		"on your MySpace, blog, or website!", "sharePanel-embed-description"));
        topPanel.add(leftSide);
        subPanel.add(topPanel);

        // TODO: Add this in once we support this feature.
        /*Label testLabel = new Label("Test");
        RollupBox postGame = new RollupBox("Post this game to...", "postGameRollup", testLabel);
        add(postGame);*/

        RollupBox emailBox = new RollupBox("Email Whirled to Your Friends", "emailRollup",
            new InviteEmailPanel(page, ""));
        emailBox.setOpen(true);
        subPanel.add(emailBox);

        RollupBox imageLinksBox = new RollupBox("Image Links to Whirled", "imageLinksRollup",
            new ImageLinkPanel());
        subPanel.add(imageLinksBox);

        RollupBox findFriendsBox = new RollupBox("Find Friends Already on Whirled",
            "findFriendsRollup", new FindFriendsPanel());
        subPanel.add(findFriendsBox);

        _invitesvc.getHomeSceneId(new MsoyCallback<Integer>() {
            public void onSuccess (Integer result) {
                topPanel.add(new ShareURLBox("Your Whirled Invite URL", "Embed Your Home", result));
            }
        });
    }

    public NewSharePanel (final PeoplePage page, int gameId, final String gameToken, final String
        gameType, String message)
    {
        setStylePrimaryName("sharePanel");

        // Top area
        final HorizontalPanel topPanel = new HorizontalPanel();
        topPanel.setStylePrimaryName("sharePanel-top");
        topPanel.add(new ShareURLBox("Your Game URL", "Embed Game", gameId,
            gameToken, gameType));
        add(topPanel);

        // TODO: Add this in once we support this feature.
        /*Label testLabel = new Label("Test");
        RollupBox postGame = new RollupBox("Post this game to...", "postGameRollup", testLabel);
        add(postGame);*/

        RollupBox emailBox = new RollupBox("Email This Game to Your Friends", "emailRollup",
            new InviteEmailPanel(page, message));
        emailBox.setOpen(true);
        add(emailBox);

        _gamesvc.loadGameDetail(gameId, new MsoyCallback<GameDetail>() {
            public void onSuccess (GameDetail result) {
                topPanel.insert(new GameInfoPanel(result), 0);
                RollupBox imageLinksBox = new RollupBox("Image Links to this Game",
                    "imageLinksRollup", new GameLinksPanel(result, gameToken, gameType));
                add(imageLinksBox);
            }
        });
    }

    protected static class FindFriendsPanel extends VerticalPanel
    {
        public FindFriendsPanel ()
        {
            setStylePrimaryName("findFriendsPanel");

            // Search by name panel
            add(MsoyUI.createLabel("Search by real name, email address, or Whirled name.",
                "importContacts-field"));
            HorizontalPanel searchBar = new HorizontalPanel();
            searchBar.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
            TextBox search = new TextBox();
            search.setStylePrimaryName("findFriendsPanel-searchBox");
            searchBar.add(search);
            searchBar.add(MsoyUI.createButton("shortThin", "search", new ClickListener() {
                public void onClick (Widget sender) {
                    // TODO
                    Window.alert("search");
                }
            }));
            add(searchBar);
            add(MsoyUI.createLabel("We can search your contacts to see which of your friends " +
            		"are already on Whirled.", "importContacts-field"));
            ImportContactsPanel importContacts = new ImportContactsPanel();
            importContacts.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    // TODO
                    Window.alert("search webmail");
                }
            });
            add(importContacts);
        }
    }

    protected static class ImageLinkPanel extends VerticalPanel
    {
        public ImageLinkPanel ()
        {
            setStylePrimaryName("imageLinkPanel");
            add(MsoyUI.createHTML("<a href='#people-invites_links'>Click here</a> " +
                "for tons of cool images you can use to link to Whirled.com on your MySpace, " +
                "blog, or website!  <a href='http://wiki.whirled.com/Affiliate'>Earn " +
                "money</a> whenever someone joins Whirled by clicking on your image.", null));
            FlowPanel panel = new FlowPanel();
            Image icons = new Image();
            icons.setStylePrimaryName("imageLinkPanel-icons");
            panel.add(icons);
            Widget moreLink = Link.create("more", Pages.PEOPLE, "invites_links");
            moreLink.setStylePrimaryName("imageLinkPanel-more");
            panel.add(moreLink);
            add(panel);
        }
    }

    protected static class ImportContactsPanel extends SmartTable
        implements SourcesClickEvents
    {
        public ImportContactsPanel ()
        {
            setText(0, 0, "Your email", 1, "importContacts-field");
            TextBox emailText = new TextBox();
            emailText.setStylePrimaryName("importContacts-textBox");
            setWidget(0, 1, emailText);
            setText(0, 2, "@", 1, "importContacts-field");
            ListBox webmailList = new ListBox();
            webmailList.setStylePrimaryName("importContacts-webmailList");
            webmailList.addItem("MSN Hotmail", "hotmail.com");
            webmailList.addItem("Gmail", "gmail.com");
            webmailList.addItem("AOL", "aol.com");
            webmailList.addItem("Lycos", "lycos.com");
            webmailList.addItem("RediffMail", "rediffmail.com");
            webmailList.addItem(".Mac", "mac.com");
            webmailList.addItem("mail.com", "mail.com");
            webmailList.addItem("FastMail.FM", "fastmail.fm");
            webmailList.addItem("Web.de", "web.de");
            webmailList.addItem("MyNet.com", "mynet.com");
            setWidget(0, 3, webmailList);
            setText(1, 0, "Password", 1, "importContacts-field");
            TextBox passwordText = new TextBox();
            passwordText.setStylePrimaryName("importContacts-textBox");
            setWidget(1, 1, passwordText);
            setText(2, 0, "We will never spam your friends, and we will only email " +
                    "with your permission.", 2, "importContacts-note");
            _sendWebmail = MsoyUI.createButton("shortThin", "continue", null);
            setWidget(2, 2, _sendWebmail);
            getFlexCellFormatter().setHorizontalAlignment(3, 2,
                HasHorizontalAlignment.ALIGN_RIGHT);
        }

        public void addClickListener (ClickListener listener)
        {
            _sendWebmail.addClickListener(listener);
        }

        public void removeClickListener (ClickListener listener)
        {
            _sendWebmail.removeClickListener(listener);
        }

        protected final PushButton _sendWebmail;
    }

    protected static class InviteEmailPanel extends SmartTable
    {
        public InviteEmailPanel (final Page page, String defaultMessage)
        {
            setStylePrimaryName("inviteEmailPanel");
            setText(0, 0, "Invite a friend by email and get 1000 coins when they join!", 1, "description");

            // Side panel
            VerticalPanel sidePanel = new VerticalPanel();
            Hyperlink link = new Hyperlink();
            link.setStylePrimaryName("sideLink");
            link.setText("Import Email Addresses");
            link.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    // TODO
                    Window.alert("Import email addresses.");
                }
            });
            sidePanel.add(link);
            Label label = new Label("from Yahoo, Hotmail, Gmail, MSN, AOL, Lycos, .mac, and more!");
            label.setStylePrimaryName("sideLabel");
            sidePanel.add(label);
            SimplePanel image = new SimplePanel();
            image.setStylePrimaryName("importImage");
            sidePanel.add(image);
            link = new Hyperlink();
            link.setStylePrimaryName("sideLink");
            link.setText("View All Invitees");
            link.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    // TODO
                    Window.alert("View all invitees");
                }
            });
            sidePanel.add(link);
            label = new Label("See everyone you've ever invited to Whirled.");
            label.setStylePrimaryName("sideLabel");
            sidePanel.add(label);
            setWidget(0, 1, sidePanel);
            getFlexCellFormatter().setRowSpan(0, 1, 2);

            // Email tab
            TabPanel tabs = new TabPanel();
            SmartTable emailPanel = new SmartTable("emailTab", 0, 0);
            emailPanel.setText(0, 0, "To", 1, "emailTab-field");
            emailPanel.setText(0, 1, "(use commas to separate emails)", 1, "emailTab-description");
            TextArea toLine = new TextArea();
            toLine.setStylePrimaryName("emailTab-input");
            emailPanel.setWidget(1, 0, toLine, 2, null);
            emailPanel.setText(2, 0, "Message", 1, "emailTab-field");
            emailPanel.setText(2, 1, "(optional)", 1, "emailTab-description");
            TextArea messageLine = new TextArea();
            messageLine.setText(defaultMessage);
            messageLine.setStylePrimaryName("emailTab-input");
            emailPanel.setWidget(3, 0, messageLine, 2, null);
            PushButton send = MsoyUI.createButton("shortThin", "send", new ClickListener() {
                public void onClick (Widget sender) {
                    // TODO
                    Window.alert("send");
                }
            });
            emailPanel.setWidget(4, 0, send, 2, null);
            emailPanel.getFlexCellFormatter().setHorizontalAlignment(4, 0,
                HasHorizontalAlignment.ALIGN_RIGHT);
            tabs.add(emailPanel, "E-mail");

            // All your friends tab
            VerticalPanel allPanel = new VerticalPanel();
            allPanel.setStylePrimaryName("allTab");
            Label description = new Label("We can import your contacts from your email.");
            description.setStylePrimaryName("importContacts-field");
            allPanel.add(description);
            ImportContactsPanel importContacts = new ImportContactsPanel();
            int row = importContacts.insertRow(0);
            importContacts.setText(row, 0, "Message", 1, "importContacts-field");
            TextArea importMessage = new TextArea();
            importMessage.setText(defaultMessage);
            importMessage.setStylePrimaryName("allTab-message");
            importContacts.setWidget(row, 1, importMessage, 3, null);
            importContacts.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    // TODO: Replace fake list with real one.
                    List<EmailContact> contacts = new ArrayList<EmailContact>();
                    contacts.add(new EmailContact("Daniel James", "daniel@threerings.net"));
                    contacts.add(new EmailContact("Robert Zubek", "robert@threerings.net"));
                    contacts.add(new EmailContact("Annie Shao", "annie@threerings.net"));
                    contacts.add(new EmailContact("Kyle Sampson", "kyle@threerings.net"));
                    page.setContent(new ImportContactListPanel(contacts));
                }
            });
            allPanel.add(importContacts);
            tabs.add(allPanel, "All Your Friends");

            tabs.selectTab(0);
            setWidget(1, 0, tabs);
        }
    }

    protected static class SizePanel extends FocusPanel
    {
        public SizePanel (String size, String sizeStyle)
        {
            setStylePrimaryName("sizePanel");
            VerticalPanel panel = new VerticalPanel();
            panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            Label sizeLabel = new Label(size);
            sizeLabel.setStylePrimaryName("sizeLabel");
            panel.add(sizeLabel);
            _box = new SimplePanel();
            _box.setStylePrimaryName("sizeBox");
            _box.addStyleName(sizeStyle + "Box");
            panel.add(_box);
            add(panel);

            addMouseListener(new MouseListenerAdapter() {
                @Override public void onMouseEnter (Widget sender) {
                    setStylePrimaryName("sizePanel-mouseOver");
                }
                @Override public void onMouseLeave (Widget sender) {
                    setStylePrimaryName("sizePanel");
                }
            });
        }

        public void setSelected (boolean selected)
        {
            if (selected) {
                _box.setStylePrimaryName("sizeBox-selected");
            } else {
                _box.setStylePrimaryName("sizeBox");
            }
        }

        protected final SimplePanel _box;
    }

    protected static class ShareURLBox extends SimplePanel
    {
        public ShareURLBox (String urlDesc, String embedDesc, int homeSceneId)
        {
            this(urlDesc, embedDesc, createAffiliateLandingUrl(Pages.LANDING), homeSceneId, false);
        }

        public ShareURLBox (String urlDesc, String embedDesc, final int gameId, String gameToken,
            String type)
        {
            this(urlDesc, embedDesc, createGameURL(gameId, gameToken, type), gameId, true);
        }

        public ShareURLBox (String urlDesc, String embedDesc, String url, final int placeId,
            final boolean isGame)
        {
            setStylePrimaryName("shareURLBox");
            addStyleName("shareURLBox-closed");
            SmartTable table = new SmartTable();
            table.setStylePrimaryName("urlPanel");
            table.setText(0, 0, urlDesc, 2, null);
            SimplePanel icon = new SimplePanel();
            icon.setStylePrimaryName("shareURLBox-urlIcon");
            table.setWidget(1, 0, icon);
            final TextBox urlText = new TextBox();
            urlText.setText(url);
            urlText.addFocusListener(new FocusListener() {
                public void onFocus (Widget sender) {
                    // Select the text on focus.
                    urlText.selectAll();
                }
                public void onLostFocus (Widget sender) {
                    // Nothing needed.
                }
            });
            table.setWidget(1, 1, urlText);
            table.setText(2, 0, embedDesc, 2, null);
            icon = new SimplePanel();
            icon.setStylePrimaryName("shareURLBox-embedIcon");
            table.setWidget(3, 0, icon);
            final TextBox embedText = new TextBox();
            embedText.setText(createEmbedCode(placeId, FULL_WIDTH, FULL_HEIGHT, isGame));
            embedText.addFocusListener(new FocusListener() {
                public void onFocus (Widget sender) {
                    // Select the text on focus.
                    embedText.selectAll();
                }
                public void onLostFocus (Widget sender) {
                    // Nothing needed.
                }
            });
            table.setWidget(3, 1, embedText);

            // Options panel
            _optionsPanel = new SmartTable();
            _optionsPanel.setStylePrimaryName("optionsPanel");
            Hyperlink options = new Hyperlink();
            options.setText("options");
            options.setStylePrimaryName("shareURLBox-optionsLink");
            options.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    setOptionsOpen(!_open);
                }
            });
            _optionsPanel.setWidget(0, 0, options, 4, null);
            _optionsPanel.getFlexCellFormatter().setHorizontalAlignment(0, 0,
                HasHorizontalAlignment.ALIGN_RIGHT);
            _smallPanel = new SizePanel("350x200", "small");
            _mediumPanel = new SizePanel("400x415", "medium");
            _largePanel = new SizePanel("700x575", "large");
            _fullPanel = new SizePanel("100%x575", "full");
            _smallPanel.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    embedText.setText(createEmbedCode(placeId, SMALL_WIDTH, SMALL_HEIGHT, isGame));
                    _smallPanel.setSelected(true);
                    _mediumPanel.setSelected(false);
                    _largePanel.setSelected(false);
                    _fullPanel.setSelected(false);
                }
            });
            _mediumPanel.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    embedText.setText(createEmbedCode(placeId, MEDIUM_WIDTH, MEDIUM_HEIGHT, isGame));
                    _smallPanel.setSelected(false);
                    _mediumPanel.setSelected(true);
                    _largePanel.setSelected(false);
                    _fullPanel.setSelected(false);
                }
            });
            _largePanel.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    embedText.setText(createEmbedCode(placeId, LARGE_WIDTH, LARGE_HEIGHT, isGame));
                    _smallPanel.setSelected(false);
                    _mediumPanel.setSelected(false);
                    _largePanel.setSelected(true);
                    _fullPanel.setSelected(false);
                }
            });
            _fullPanel.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    embedText.setText(createEmbedCode(placeId, FULL_WIDTH, FULL_HEIGHT, isGame));
                    _smallPanel.setSelected(false);
                    _mediumPanel.setSelected(false);
                    _largePanel.setSelected(false);
                    _fullPanel.setSelected(true);
                }
            });
            _fullPanel.setSelected(true);
            table.setWidget(4, 0, _optionsPanel, 2, null);
            add(table);
            _open = false;
        }

        public void setOptionsOpen (boolean open)
        {
            if (open == _open) {
                return;
            }

            if (open) {
                _optionsPanel.setWidget(1, 0, _smallPanel);
                _optionsPanel.setWidget(1, 1, _mediumPanel);
                _optionsPanel.setWidget(1, 2, _largePanel);
                _optionsPanel.setWidget(1, 3, _fullPanel);
                addStyleName("shareURLBox-open");
                removeStyleName("shareURLBox-closed");
            } else {
                _optionsPanel.removeRow(1);
                addStyleName("shareURLBox-closed");
                removeStyleName("shareURLBox-open");
            }
            _open = open;
        }

        protected static String createAffiliateLandingUrl (Pages page, Object ...args)
        {
            String path = DeploymentConfig.serverURL + "welcome/" + CShell.creds.getMemberId();
            if (page != Pages.LANDING) {
                path += "/" + Pages.makeToken(page, Args.compose(args));
            }
            return path;
        }

        protected final static String SMALL_WIDTH = "350";
        protected final static String MEDIUM_WIDTH = "400";
        protected final static String LARGE_WIDTH = "700";
        protected final static String FULL_WIDTH = "100%";
        protected final static String SMALL_HEIGHT = "200";
        protected final static String MEDIUM_HEIGHT = "415";
        protected final static String LARGE_HEIGHT = "575";
        protected final static String FULL_HEIGHT = "575";

        protected final SmartTable _optionsPanel;
        protected final SizePanel _smallPanel;
        protected final SizePanel _mediumPanel;
        protected final SizePanel _largePanel;
        protected final SizePanel _fullPanel;
        protected boolean _open;
    }

    protected static class GameInfoPanel extends HorizontalPanel
    {
        public GameInfoPanel (GameDetail detail)
        {
            setStylePrimaryName("shareGameInfo");
            Game game = detail.item;
            ThumbBox thumbnail = new ThumbBox(game.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE);
            thumbnail.addStyleName("thumbnail");
            add(thumbnail);
            VerticalPanel infoPanel = new VerticalPanel();
            FlowPanel nameInfoPanel = new FlowPanel();
            Widget gameLink = Link.create(game.name, Pages.GAMES, Args.compose("d", game.gameId));
            gameLink.addStyleName("inline");
            gameLink.addStyleName("name");
            nameInfoPanel.add(gameLink);
            CreatorLabel creatorLabel = new CreatorLabel(detail.creator);
            creatorLabel.addStyleName("inline");
            nameInfoPanel.add(creatorLabel);
            infoPanel.add(nameInfoPanel);
            infoPanel.add(MsoyUI.createLabel(_dmsgs.xlate("genre" + game.genre), "genre"));
            infoPanel.add(WidgetUtil.makeShim(5, 5));
            infoPanel.add(MsoyUI.createLabel(game.description, "description"));
            add(infoPanel);
        }
    }

    protected static class GameLinksPanel extends VerticalPanel
    {
        public GameLinksPanel (final GameDetail detail, final String gameToken, final String type)
        {
            setStylePrimaryName("gameLinks");
            add(new Label("Click the image you want to include on your blog or website, then " +
            		"copy and paste the html code that appears in the box below.  Your id is " +
            		"stored in the code, and when new users join Whirled through your link you " +
            		"earn coins, bars and bling."));
            final TextArea htmlCode = new TextArea();
            htmlCode.setStylePrimaryName("gameLinks-htmlCode");
            htmlCode.setReadOnly(true);
            htmlCode.setText("Click an image below...");
            add(htmlCode);
            setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

            HorizontalPanel icons = new HorizontalPanel();
            icons.setStylePrimaryName("gameLinks-icons");
            final Game game = detail.item;
            Widget thumbnail = MediaUtil.createMediaView(game.getShotMedia(), MediaDesc.THUMBNAIL_SIZE,
                new ClickListener() {
                    public void onClick (Widget sender) {
                        htmlCode.setText(createLink(game.getShotMedia().getMediaPath(),
                            detail.gameId, gameToken, type));
                    }
            });
            icons.add(thumbnail);
            thumbnail = MediaUtil.createMediaView(game.getThumbnailMedia(),
                MediaDesc.THUMBNAIL_SIZE, new ClickListener() {
                    public void onClick (Widget sender) {
                        htmlCode.setText(createLink(game.getThumbnailMedia().getMediaPath(),
                            detail.gameId, gameToken, type));
                    }
            });
            icons.add(thumbnail);
            add(icons);
        }

        protected String createLink (String imageUrl, int gameId, String gameToken, String type)
        {
            return "<a href=\"" + createGameURL(gameId, gameToken, type) +
                "\"><img src=\"" + imageUrl + "\"></a>";
        }
    }

    protected static String createGameURL (int gameId, String gameToken, String type)
    {
        boolean isAVRG = type.startsWith("avrg");
        return DeploymentConfig.serverURL + "#world-" + (isAVRG ?
            "s" + type.substring(4) + "_world_" : "") + "game_t_" + gameId + "_" +
            CShell.creds.getMemberId() + "_" + gameToken;
    }

    protected static String createEmbedCode (int placeId, String width, String height,
        boolean isGame)
    {
        // Flash args.
        StringBuilder args = new StringBuilder();
        if (isGame) {
            args.append("gameLobby=").append(placeId).append("&vec=e.whirled.games.")
                .append(placeId);
        } else {
            args.append("sceneId=").append(placeId).append("&vec=e.whirled.rooms.")
            .append(placeId);
        }
        if (!CShell.isGuest()) {
            args.append("&aff=").append(CShell.getMemberId());
        }

        // URL to the swf.
        String hostOnlyUrl = DeploymentConfig.serverURL.replaceFirst("(http:\\/\\/[^\\/]*).*",
            "$1/");
        String swfUrl = hostOnlyUrl + "clients/world-client.swf";

        // URL to Whirled.
        String fullUrl = hostOnlyUrl + (CShell.isGuest() ? "#" :
            "welcome/" + CShell.getMemberId() + '/');

        return _cmsgs.embed(args.toString(), swfUrl, width, height, fullUrl);
    }

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
    protected static final InviteServiceAsync _invitesvc = (InviteServiceAsync)
        ServiceUtil.bind(GWT.create(InviteService.class), InviteService.ENTRY_POINT);
}
