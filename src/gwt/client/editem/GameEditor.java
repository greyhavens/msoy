//
// $Id$

package client.editem;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.google.gwt.xml.client.XMLParser;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.DOMException;
import com.google.gwt.xml.client.Element;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * A class for creating and editing {@link Game} digital items.
 */
public class GameEditor extends ItemEditor
{
    /** Constants from com.threerings.parlor.game.data.GameConfig */
    public static int SEATED_GAME = 0;
    public static int SEATED_CONTINUOUS = 1;
    public static int PARTY = 2;

    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _game = (Game)item;

        try {
            _configXML = XMLParser.parse(_game.config);
        } catch (DOMException de) {
            CEditem.log("XML Parse Failed", de);
        } 
        if (_configXML == null) {
            _configXML = XMLParser.createDocument();
        }
        if (!_configXML.hasChildNodes()) {
            _configXML.appendChild(_configXML.createElement("game"));
        }
        NodeList matches = _configXML.getElementsByTagName("match");
        if (matches.getLength() > 0) {
            _match = (Element)matches.item(0);
            Node option = _match.getFirstChild();
            // TODO <start_seats>, also game_type might be merged with the "type" attributed on 
            // <match> - right now it merely refers to which type of table game we're playing
            while (option != null) {
                if (option.getNodeType() == Node.ELEMENT_NODE) {
                    if ("min_seats".equals(option.getNodeName())) {
                        _minPlayers.setText(option.getFirstChild().toString());;
                        _minPlayersXML = (Element)option;
                    } else if ("max_seats".equals(option.getNodeName())) {
                        _maxPlayers.setText(option.getFirstChild().toString());
                        _maxPlayersXML = (Element)option;
                    } else if ("unwatchable".equals(option.getNodeName())) {
                        _watchable.setChecked(false);
                        _unwatchableXML = (Element)option;
                    }
                }
                option = option.getNextSibling();
            }
            if (_match.hasAttribute("type")) {
                // this will be more sensible when SEATED_CONTINUOUS is re-instated as a game type
                _gameType.setSelectedIndex(("" + SEATED_GAME).equals(
                    _match.getAttribute("type")) ? 0 : 1);
            } else {
                _match.setAttribute("type", "" + SEATED_GAME);
                _gameType.setSelectedIndex(0);
            }
        } else {
            _match = _configXML.createElement("match");
            _match.setAttribute("type", "" + SEATED_GAME);
            _gameType.setSelectedIndex(0);
            _configXML.getFirstChild().appendChild(_match);
        }

        if (_minPlayersXML == null) {
            _minPlayers.setText("0");
            _minPlayersXML = _configXML.createElement("min_seats");
            _minPlayersXML.appendChild(_configXML.createTextNode(_minPlayers.getText()));
            _match.appendChild(_minPlayersXML);
        } 
        if (_maxPlayersXML == null) {
            _maxPlayers.setText("0");
            _maxPlayersXML = _configXML.createElement("max_seats");
            _maxPlayersXML.appendChild(_configXML.createTextNode(_maxPlayers.getText()));
            _match.appendChild(_maxPlayersXML);
        }
        if (_unwatchableXML == null) {
            // this only gets appended to _match is the watchable checkbox is unchecked
            _unwatchableXML = _configXML.createElement("unwatchable");
        }

        NodeList params = _configXML.getElementsByTagName("params");
        if (params.getLength() > 0) {
            _params = (Element)params.item(0);
            Node child = _params.getFirstChild();
            String childrenText = "";
            while (child != null) {
                // TODO make this create spiffy widgets for editing these parameters, rather than
                // the XML 
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    childrenText += child + "\n";
                }
                child = child.getNextSibling();
            }
            _gamedef.setText(childrenText);
        } else {
            _params = _configXML.createElement("params");
            _gamedef.setText("");
            _configXML.getFirstChild().appendChild(_params);
        }

        _game.config = _configXML.toString();
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        Game game = new Game();
        game.config = "";
        return game;
    }

    // @Override from ItemEditor
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        // configure the main uploader first
        tabs.add(createMainUploader(CEditem.emsgs.gameMainTitle(), new MediaUpdater() {
            public String updateMedia (MediaDesc desc, int width, int height) {
                // TODO: validate media type
                _game.gameMedia = desc;
                return null;
            }
        }), CEditem.emsgs.gameMainTab());

        FlexTable bits = new FlexTable();
        tabs.add(bits, CEditem.emsgs.gameConfigTab());

        // TODO: it'd be nice to force-format this text field for integers, or something.
        int row = 0;
        bits.setText(row, 0, CEditem.emsgs.gameMinPlayers());
        bits.setWidget(row++, 1, bind(_minPlayers = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                setOnlyChild(_minPlayersXML, _configXML.createTextNode(text));
                _game.config = _configXML.toString();
            }
        }));

        bits.setText(row, 0, CEditem.emsgs.gameMaxPlayers());
        bits.setWidget(row++, 1, bind(_maxPlayers = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                setOnlyChild(_maxPlayersXML, _configXML.createTextNode(text));
                _game.config = _configXML.toString();
            }
        }));

        // seated continuous games are disabled for now. 
        bits.setText(row, 0, CEditem.emsgs.gameGameType());
        bits.setWidget(row++, 1, bind(_gameType = new ListBox(), new Binder() {
            public void valueChanged () {
                // this will also do something more sensible when we're using SEATED_CONTINUOUS
                _match.setAttribute("type", _gameType.getSelectedIndex() == 0 ? 
                    "" + SEATED_GAME : "" + PARTY);
                _game.config = _configXML.toString();
            }
        }));
        _gameType.addItem(CEditem.dmsgs.getString("gameType0"));
        _gameType.addItem(CEditem.dmsgs.getString("gameType2"));

        bits.setText(row, 0, CEditem.emsgs.gameWatchable());
        _watchable = new CheckBox();
        _watchable.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                if (_watchable.isChecked()) {
                    _match.removeChild(_unwatchableXML);
                } else {
                    _match.appendChild(_unwatchableXML);
                }
                _game.config = _configXML.toString();
            }
        });
        // watchable defaults to true, and might not be specified in the XML
        _watchable.setChecked(true);
        bits.setWidget(row++, 1, _watchable);

        bits.setText(row++, 0, CEditem.emsgs.gameDefinition());
        bits.setWidget(row, 0, bind(_gamedef = new TextArea(), new Binder() {
            public void textUpdated (String text) {
                // this won't be so odd once we have widgets to make this XML for these options
                // for us
                try {
                    // need a valid document (single child element) for parsing to work
                    Document params = XMLParser.parse("<params>" + text + "</params>");
                    while (_params.hasChildNodes()) {
                        _params.removeChild(_params.getFirstChild());
                    }
                    if (params.getFirstChild() != null && params.getFirstChild().hasChildNodes()) {
                        Node param = params.getFirstChild().getFirstChild();
                        while (param != null) {
                            // only support elements as children of <params> - this strips out 
                            // whitespace and comments and random bits of text
                            if (param.getNodeType() == Node.ELEMENT_NODE) {
                                _params.appendChild(param.cloneNode(true));
                            }
                            param = param.getNextSibling();
                        }
                    }
                } catch (DOMException de) {
                    // this is nothing to be alarmed about - parsing will fail most of the time
                    // (hopefully not when they're done editing)
                } 
                _game.config = _configXML.toString();
            }
        }));
        bits.getFlexCellFormatter().setColSpan(row++, 0, 2);
        _gamedef.setCharacterWidth(80);
        _gamedef.setVisibleLines(5);

        super.createInterface(contents, tabs);
    }
    
    // mr. utility
    protected static short asShort (String s)
    {
        try {
            return (short) Integer.parseInt(s);
        } catch (Exception e) {
            return (short) 0;
        }
    }

    protected static void setOnlyChild (Node parent, Node child) 
    {
        while (parent.hasChildNodes()) {
            parent.removeChild(parent.getFirstChild());
        }
        parent.appendChild(child);
    }

    protected Game _game;

    protected TextBox _minPlayers, _maxPlayers;
    protected ListBox _gameType;
    protected CheckBox _watchable;
    protected TextArea _gamedef;

    protected Document _configXML;
    protected Element _match;
    protected Element _params;
    protected Element _minPlayersXML;
    protected Element _maxPlayersXML;
    protected Element _unwatchableXML;
}
