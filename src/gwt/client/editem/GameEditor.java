//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.xml.client.XMLParser;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.DOMException;
import com.google.gwt.xml.client.Element;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * A class for creating and editing {@link Game} digital items.
 */
public class GameEditor extends ItemEditor
{
    /** Constants from com.threerings.parlor.game.data.GameConfig */
    public static String SEATED_GAME = "0";
    public static String SEATED_CONTINUOUS = "1";
    public static String PARTY = "2";

    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _game = (Game)item;

        // read our configuration information out of the game's XML config data
        Document xml;
        try {
            xml = XMLParser.parse(_game.config);
        } catch (DOMException de) {
            CEditem.log("XML Parse Failed", de);
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

        Object[] bits = { "ident", _ident, "controller", _controller, "manager", _manager };
        for (int ii = 0; ii < bits.length; ii += 2) {
            NodeList elems = xml.getElementsByTagName((String)bits[ii]);
            if (elems.getLength() > 0) {
                Element elem = (Element)elems.item(0);
                ((TextBox)bits[ii+1]).setText(elem.getFirstChild().toString());
            }
        }

        NodeList lwjgl = xml.getElementsByTagName("lwjgl");
        if (lwjgl.getLength() > 0) {
            _lwjgl.setChecked(true);
        }
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
        int row = 0;

        // seated continuous games are disabled for now.
        bits.setText(row, 0, CEditem.emsgs.gameGameType());
        bits.setWidget(row++, 1, bind(_matchType = new ListBox(), new Binder() {
            public void valueChanged () {
                // TODO: disable or hide min/max players and watchable if this is a party game
            }
        }));
        _matchType.addItem(CEditem.dmsgs.getString("gameType0"));
        _matchType.addItem(CEditem.dmsgs.getString("gameType2"));

        // TODO: it'd be nice to force-format this text field for integers, or something.
        bits.setText(row, 0, CEditem.emsgs.gameMinPlayers());
        bits.setWidget(row++, 1, _minPlayers = new TextBox());
        _minPlayers.setText("1");
        _minPlayers.setVisibleLength(5);

        bits.setText(row, 0, CEditem.emsgs.gameMaxPlayers());
        bits.setWidget(row++, 1, _maxPlayers = new TextBox());
        _maxPlayers.setText("1");
        _maxPlayers.setVisibleLength(5);

        bits.setText(row, 0, CEditem.emsgs.gameWatchable());
        bits.setWidget(row++, 1, _watchable = new CheckBox());
        _watchable.setChecked(true);

        bits.setText(row++, 0, CEditem.emsgs.gameDefinition());
        bits.setWidget(row, 0, _extras = new TextArea());

        bits.getFlexCellFormatter().setColSpan(row++, 0, 2);
        _extras.setCharacterWidth(80);
        _extras.setVisibleLines(5);

        bits.setText(row, 0, CEditem.emsgs.gameJavaTip());
        bits.getFlexCellFormatter().setColSpan(row++, 0, 2);

        bits.setText(row, 0, CEditem.emsgs.gameIdent());
        bits.setWidget(row++, 1, _ident = new TextBox());

        bits.setText(row, 0, CEditem.emsgs.gameController());
        bits.setWidget(row++, 1, _controller = new TextBox());
        _controller.setVisibleLength(40);

        bits.setText(row, 0, CEditem.emsgs.gameManager());
        bits.setWidget(row++, 1, _manager = new TextBox());
        _manager.setVisibleLength(40);

        bits.setText(row, 0, CEditem.emsgs.gameLWJGL());
        bits.setWidget(row++, 1, _lwjgl = new CheckBox());

        super.createInterface(contents, tabs);
    }

    // @Override // from ItemEditor
    protected void prepareItem ()
        throws Exception
    {
        super.prepareItem();

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

        Object[] bits = { "ident", _ident, "controller", _controller, "manager", _manager };
        for (int ii = 0; ii < bits.length; ii += 2) {
            String text = ((TextBox)bits[ii+1]).getText();
            if (text.length() > 0) {
                Element elem = xml.createElement((String)bits[ii]);
                elem.appendChild(xml.createTextNode(text));
                xml.getFirstChild().appendChild(elem);
            }
        }

        if (_lwjgl.isChecked()) {
            xml.getFirstChild().appendChild(xml.createElement("lwjgl"));
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
                throw new Exception(CEditem.emsgs.gameDefinitionError(de.getMessage()));
            }
        }

        _game.config = xml.toString();
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
    protected ListBox _matchType;
    protected CheckBox _watchable;
    protected TextBox _ident, _controller, _manager;
    protected CheckBox _lwjgl;
    protected TextArea _extras;
}
