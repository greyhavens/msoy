//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.xml.client.DOMException;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameDetail;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Game;

import client.shell.CShell;
import client.shell.DynamicMessages;

/**
 * A class for creating and editing {@link Game} digital items.
 */
public class GameEditor extends ItemEditor
{
    /** Constants from com.threerings.parlor.game.data.GameConfig */
    public static String SEATED_GAME = "0";
    public static String SEATED_CONTINUOUS = "1";
    public static String PARTY = "2";

    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _game = (Game)item;
        setUploaderMedia(Item.MAIN_MEDIA, _game.gameMedia);
        setUploaderMedia(Item.AUX_MEDIA, _game.shotMedia);
        setUploaderMedia(Game.SERVER_CODE_MEDIA, _game.serverMedia);

        // if we have no game configuration, leave everything as default
        if (_game.config == null || _game.config.length() == 0) {
            return;
        }

        // configure our genre
        for (int ii = 0; ii < Game.GENRES.length; ii++) {
            if (Game.GENRES[ii] == _game.genre) {
                _genre.setSelectedIndex(ii);
                break;
            }
        }

        // read our configuration information out of the game's XML config data
        Document xml;
        try {
            xml = XMLParser.parse(_game.config);
        } catch (DOMException de) {
            CShell.log("XML Parse Failed", de);
            return; // leave everything at defaults
        }

        NodeList matches = xml.getElementsByTagName("match");
        if (matches.getLength() > 0) {
            Element match = (Element)matches.item(0);
            Node option = match.getFirstChild();
            // TODO <start_seats>, also game_type might be merged with the "type" attributed on
            // <match> - right now it merely refers to which type of table game we're playing
            while (option != null) {
                if (option.getNodeType() == Node.ELEMENT_NODE) {
                    if ("min_seats".equals(option.getNodeName())) {
                        _minPlayers.setText(option.getFirstChild().toString());
                    } else if ("max_seats".equals(option.getNodeName())) {
                        _maxPlayers.setText(option.getFirstChild().toString());
                    } else if ("unwatchable".equals(option.getNodeName())) {
                        _watchable.setChecked(false);
                    }
                }
                option = option.getNextSibling();
            }
            if (match.hasAttribute("type")) {
                // this will be more sensible when SEATED_CONTINUOUS is re-instated as a game type
                _matchType.setSelectedIndex(SEATED_GAME.equals(match.getAttribute("type")) ? 0 : 1);
            }
        }

        NodeList params = xml.getElementsByTagName("params");
        if (params.getLength() > 0) {
            Element param = (Element)params.item(0);
            Node child = param.getFirstChild();
            String childrenText = "";
            while (child != null) {
                // TODO make this create spiffy widgets for editing these parameters, rather than
                // the XML
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    childrenText += child + "\n";
                }
                child = child.getNextSibling();
            }
            _extras.setText(childrenText);
        }

        Object[] bits = { "serverclass", _serverClass };
        for (int ii = 0; ii < bits.length; ii += 2) {
            NodeList elems = xml.getElementsByTagName((String)bits[ii]);
            if (elems.getLength() > 0) {
                Element elem = (Element)elems.item(0);
                ((TextBox)bits[ii+1]).setText(elem.getFirstChild().toString());
            }
        }

        NodeList avrg = xml.getElementsByTagName("avrg");
        if (avrg.getLength() > 0) {
            _avrg.setChecked(true);
        }
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        Game game = new Game();
        game.config = "";
        return game;
    }

    @Override // from ItemEditor
    protected void addExtras ()
    {
        _genre = new ListBox();
        for (byte genre : Game.GENRES) {
            _genre.addItem(_dmsgs.getString("genre" + genre));
        }
        addRow(_emsgs.gameGenre(), _genre);

        // seated continuous games are disabled for
        addRow(_emsgs.gameGameType(), bind(_matchType = new ListBox(), new Binder() {
            public void valueChanged () {
                // TODO: disable or hide min/max players and watchable if this is a party game
            }
        }));
        _matchType.addItem(_dmsgs.getString("gameType0"));
        _matchType.addItem(_dmsgs.getString("gameType2"));

        // TODO: it'd be nice to force-format this text field for integers, or something
        addRow(_emsgs.gameMinPlayers(), _minPlayers = new TextBox());
        _minPlayers.setText("1");
        _minPlayers.setVisibleLength(5);

        addRow(_emsgs.gameMaxPlayers(), _maxPlayers = new TextBox());
        _maxPlayers.setText("1");
        _maxPlayers.setVisibleLength(5);

        addRow(_emsgs.gameWatchable(), _watchable = new CheckBox());
        _watchable.setChecked(true);

        // add a tab for uploading the game media
        addSpacer();
        addRow(_emsgs.gameLabel(), createMainUploader(TYPE_CODE, false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!isValidGameMedia(desc)) {
                    return _emsgs.errGameNotFlash();
                }
                _game.gameMedia = desc;
                return null;
            }
        }), _emsgs.gameTip());

        // add a tab for uploading the game screenshot
        ItemMediaUploader shotter = createAuxUploader(TYPE_IMAGE, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (width != GameDetail.SHOT_WIDTH || height != GameDetail.SHOT_HEIGHT || !desc.isImage()) {
                    return _emsgs.errInvalidShot(""+GameDetail.SHOT_WIDTH, ""+GameDetail.SHOT_HEIGHT);
                }
                _game.shotMedia = desc;
                return null;
            }
        });
        shotter.setHint(_emsgs.gameShotHint(""+GameDetail.SHOT_WIDTH, ""+GameDetail.SHOT_HEIGHT));
        addSpacer();
        addRow(_emsgs.gameShotTab(), shotter, _emsgs.gameShotTitle());

        super.addExtras();

        addSpacer();
        addRow(_emsgs.gameDefinition(), _extras = new TextArea());
        _extras.setCharacterWidth(60);
        _extras.setVisibleLines(5);
        addRow(_emsgs.gameAVRG(), _avrg = new CheckBox());
        addSpacer();

        // add a tab for uploading the game server code
        addSpacer();
        addTip(_emsgs.gameServerHeadingTip());
        MediaUpdater serverMediaUpdater = new MediaUpdater() {
            public String updateMedia (
                String name,
                MediaDesc desc,
                int width,
                int height) {
                // TODO: validate media type
                _game.serverMedia = desc;
                return null;
            }
        };
        ItemMediaUploader serverMediaUploader = createUploader(
            Game.SERVER_CODE_MEDIA,
            TYPE_CODE,
            ItemMediaUploader.NORMAL,
            serverMediaUpdater);
        addRow(
            _emsgs.gameServerMediaLabel(),
            serverMediaUploader,
            _emsgs.gameServerMediaTip());
        addRow(
            _emsgs.gameServerClass(),
            _serverClass = new TextBox(),
            _emsgs.gameServerClassTip());
        _serverClass.setVisibleLength(40);
    }

    @Override // from ItemEditor
    protected void prepareItem ()
        throws Exception
    {
        super.prepareItem();

        // configure our genre
        _game.genre = Game.GENRES[_genre.getSelectedIndex()];

        // convert our configuration information back to an XML document
        Document xml = XMLParser.createDocument();
        xml.appendChild(xml.createElement("game"));

        Element match = xml.createElement("match");
        // this will need to be more sensible when we're using SEATED_CONTINUOUS
        String type = _matchType.getSelectedIndex() == 0 ? SEATED_GAME : PARTY;
        match.setAttribute("type", type);
        xml.getFirstChild().appendChild(match);

        Element minSeats = xml.createElement("min_seats");
        minSeats.appendChild(xml.createTextNode(_minPlayers.getText()));
        match.appendChild(minSeats);

        Element maxSeats = xml.createElement("max_seats");
        maxSeats.appendChild(xml.createTextNode(_maxPlayers.getText()));
        match.appendChild(maxSeats);

        if (!_watchable.isChecked()) {
            match.appendChild(xml.createElement("unwatchable"));
        }

        Object[] bits = { "serverclass", _serverClass };
        for (int ii = 0; ii < bits.length; ii += 2) {
            String text = ((TextBox)bits[ii+1]).getText();
            if (text.length() > 0) {
                Element elem = xml.createElement((String)bits[ii]);
                elem.appendChild(xml.createTextNode(text));
                xml.getFirstChild().appendChild(elem);
            }
        }

        if (_avrg.isChecked()) {
            xml.getFirstChild().appendChild(xml.createElement("avrg"));
        }

        String extras = _extras.getText();
        if (extras.length() > 0) {
            try {
                Element pelem = xml.createElement("params");
                // need a valid document (single child element) for parsing to work
                Document params = XMLParser.parse("<params>" + extras + "</params>");
                if (params.getFirstChild() != null && params.getFirstChild().hasChildNodes()) {
                    Node param = params.getFirstChild().getFirstChild();
                    while (param != null) {
                        // only support elements as children of <params> - this strips out
                        // whitespace and comments and random bits of text
                        if (param.getNodeType() == Node.ELEMENT_NODE) {
                            pelem.appendChild(param.cloneNode(true));
                        }
                        param = param.getNextSibling();
                    }
                }
                if (pelem.getFirstChild() != null) {
                    xml.getFirstChild().appendChild(pelem);
                }

            } catch (DOMException de) {
                throw new Exception(_emsgs.gameDefinitionError(de.getMessage()));
            }
        }

        _game.config = xml.toString();
    }

    protected static void setOnlyChild (Node parent, Node child)
    {
        while (parent.hasChildNodes()) {
            parent.removeChild(parent.getFirstChild());
        }
        parent.appendChild(child);
    }

    /** Is the specified MediaDesc a valid game media? */
    protected boolean isValidGameMedia (MediaDesc desc)
    {
        // game media must be swfs. maybe we'll want remixable in the future?
        return desc.isSWF();
    }

    protected Game _game;

    protected ListBox _genre;
    protected TextBox _minPlayers, _maxPlayers;
    protected ListBox _matchType;
    protected CheckBox _watchable;
    protected TextBox _serverClass;
    protected CheckBox _avrg;
    protected TextArea _extras;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
}
