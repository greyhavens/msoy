//
// $Id$

package client.games;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.xml.client.DOMException;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameCode;
import com.threerings.msoy.item.data.all.Item;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.NumberTextBox;
import client.util.ClickCallback;
import client.util.StringUtil;

/**
 * Displays an interface for editing a game's code and configuration.
 */
public class CodeEditorPanel extends BaseEditorPanel
{
    public CodeEditorPanel (final GameCode code)
    {
        // parse the XML configuration into a friendly POJO
        final XMLConfig config = parseConfig(code.config);

        final ListBox tbox = new ListBox();
        for (GameType type : GameType.values()) {
            tbox.addItem(type.getText());
            if (type == config.type) {
                tbox.setSelectedIndex(tbox.getItemCount()-1);
            }
        }
        tbox.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent event) {
                boolean isSeated = (tbox.getSelectedIndex() == GameType.SEATED.ordinal());
                getRowFormatter().setVisible(_minRow, isSeated);
                getRowFormatter().setVisible(_maxRow, isSeated);
                getRowFormatter().setVisible(_watchRow, isSeated);
            }
        });
        addRow(_msgs.egGameType(), tbox, new Command() {
            public void execute () {
                config.type = GameType.values()[tbox.getSelectedIndex()];
            }
        });

        final NumberTextBox minPlayers = new NumberTextBox(false, 5);
        minPlayers.setNumber(config.minPlayers);
        _minRow = addRow(_msgs.egMinPlayers(), minPlayers, new Command() {
            public void execute () {
                config.minPlayers = minPlayers.getNumber().intValue();
            }
        });

        final NumberTextBox maxPlayers = new NumberTextBox(false, 5);
        maxPlayers.setNumber(config.maxPlayers);
        _maxRow = addRow(_msgs.egMaxPlayers(), maxPlayers, new Command() {
            public void execute () {
                config.maxPlayers = maxPlayers.getNumber().intValue();
            }
        });

        final CheckBox watchable = new CheckBox();
        watchable.setValue(config.watchable);
        _watchRow = addRow(_msgs.egWatchable(), watchable, new Command() {
            public void execute () {
                config.watchable = watchable.getValue();
            }
        });

        addSpacer();

        final CodeBox ccbox = new CodeBox(Item.MAIN_MEDIA, code.clientMedia);
        addRow(_msgs.egClientCode(), _msgs.egClientCodeTip(), ccbox, new Command() {
            public void execute () {
                code.clientMedia = checkClientMedia(ccbox.getMedia());
            }
        });

        final CheckBox noprogress = new CheckBox(_msgs.egNoProgressText());
        noprogress.setValue(config.noprogress);
        _watchRow = addRow(_msgs.egNoProgress(), noprogress, new Command() {
            public void execute () {
                config.noprogress = noprogress.getValue();
            }
        });
        addTip(_msgs.egNoProgressTip());

        addSpacer();

        final CodeBox scbox = new CodeBox(GameCode.SERVER_CODE_MEDIA, code.serverMedia);
        addRow(_msgs.egServerCode(), _msgs.egServerCodeTip(), scbox, new Command() {
            public void execute () {
                code.serverMedia = checkServerMedia(scbox.getMedia());
            }
        });
        addTip(_msgs.egServerCodeNote());

        final TextBox sclass = MsoyUI.createTextBox(config.serverClass, 255, 40);
        addRow(_msgs.egServerClass(), sclass, new Command() {
            public void execute () {
                config.serverClass = sclass.getText().trim();
            }
        });
        addTip(_msgs.egServerClassTip());

        final CheckBox mponly = new CheckBox(_msgs.egServerMPOnlyText());
        _watchRow = addRow(_msgs.egServerMPOnly(), mponly, new Command() {
            public void execute () {
                config.agentMPOnly = mponly.getValue();
            }
        });

        addSpacer();

        final MediaBox spbox = new MediaBox(
            MediaDesc.GAME_SHOT_SIZE, GameCode.SPLASH_MEDIA, code.splashMedia) {
            public void setMedia (MediaDesc media) {
                if (media != null) {
                    // we do some fakery here to keep the splash media sanely scaled
                    media.constraint = MediaDesc.HORIZONTALLY_CONSTRAINED;
                }
                super.setMedia(media);
            }
        };
        addRow(_msgs.egSplash(), _msgs.egSplashTip(), spbox, new Command() {
            public void execute () {
                code.splashMedia = checkImageMedia(_msgs.egSplash(), spbox.getMedia());
            }
        });

        // add a special binder that recreates our XML configuration from the POJO
        _binders.add(new Command() {
            public void execute () {
                // convert our configuration information back to an XML document
                code.config = formatConfig(config);
            }
        });

        addSpacer();

        // add our confirmation ui and update interface
        Button save = addSaveRow();

        // wire up saving the code on click
        new ClickCallback<Void>(save) {
            protected boolean callService () {
                if (!bindChanges()) {
                    return false;
                }
                _gamesvc.updateGameCode(code, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                MsoyUI.info(_msgs.egGameCodeUpdated());
                return true;
            }
        };
    }

    protected XMLConfig parseConfig (String xmlConfig)
    {
        XMLConfig config = new XMLConfig();
        Document xml;
        try {
            xml = XMLParser.parse(xmlConfig);
        } catch (DOMException de) {
            CShell.log("XML Parse Failed", de);
            return config; // leave everything at defaults
        }

        NodeList matches = xml.getElementsByTagName("match");
        if (matches.getLength() > 0) {
            Element match = (Element)matches.item(0);
            Node option = match.getFirstChild();
            // TODO <start_seats>, also game_type might be merged with the "type" attributed on
            // <match> - right now it merely refers to which type of table game we're playing
            while (option != null) {
                if (option.getNodeType() == Node.ELEMENT_NODE) {
                    final String name = option.getNodeName();
                    if ("min_seats".equals(name)) {
                        config.minPlayers = Integer.valueOf(option.getFirstChild().toString());
                    } else if ("max_seats".equals(name)) {
                        config.maxPlayers = Integer.valueOf(option.getFirstChild().toString());
                    } else if ("unwatchable".equals(name)) {
                        config.watchable = false;
                    }
                }
                option = option.getNextSibling();
            }
            if (match.hasAttribute("type")) {
                for (GameType gtype : GameType.values()) {
                    if (match.getAttribute("type").equals(gtype.getMatchType())) {
                        config.type = gtype;
                    }
                }
            }
        }

        // avrg overrides the match element, it is not a real match type
        if (xml.getElementsByTagName("avrg").getLength() > 0) {
            config.type = GameType.AVRG;
        }

        // determine the server class
        NodeList elems = xml.getElementsByTagName("serverclass");
        if (elems.getLength() > 0) {
            Element elem = (Element)elems.item(0);
            config.serverClass = elem.getFirstChild().toString();
        }

        // determine our agent-only-on-multiplayer mode
        config.agentMPOnly = (xml.getElementsByTagName("agentmponly").getLength() > 0);

        return config;
    }

    protected String formatConfig (XMLConfig config)
    {
        Document xml = XMLParser.createDocument();
        xml.appendChild(xml.createElement("game"));

        // create our match configuration
        Element match = xml.createElement("match");
        match.setAttribute("type", config.type.getMatchType());
        xml.getFirstChild().appendChild(match);
        Element minSeats = xml.createElement("min_seats");
        minSeats.appendChild(xml.createTextNode(""+config.minPlayers));
        match.appendChild(minSeats);
        Element maxSeats = xml.createElement("max_seats");
        maxSeats.appendChild(xml.createTextNode(""+config.maxPlayers));
        match.appendChild(maxSeats);
        if (!config.watchable) {
            match.appendChild(xml.createElement("unwatchable"));
        }

        // add our server class if we have one
        if (!StringUtil.isBlank(config.serverClass)) {
            Element elem = xml.createElement("serverclass");
            elem.appendChild(xml.createTextNode(config.serverClass));
            xml.getFirstChild().appendChild(elem);
        }

        // add some boolean bits
        if (config.type == GameType.AVRG) {
            xml.getFirstChild().appendChild(xml.createElement("avrg"));
        }
        if (config.noprogress) {
            xml.getFirstChild().appendChild(xml.createElement("noprogress"));
        }
        if (config.agentMPOnly) {
            xml.getFirstChild().appendChild(xml.createElement("agentmponly"));
        }

        // add the custom parameters, if any
        if (!StringUtil.isBlank(config.params)) {
            Document params;
            try {
                // need a valid document (single child element) for parsing to work
                params = XMLParser.parse("<params>" + config.params + "</params>");
            } catch (DOMException de) {
                throw new ConfigException(_msgs.errInvalidDefinition(de.getMessage()));
            }

            Element pelem = xml.createElement("params");
            if (params.getFirstChild() != null && params.getFirstChild().hasChildNodes()) {
                Node param = params.getFirstChild().getFirstChild();
                while (param != null) {
                    // only support elements as children of <params> - this strips out whitespace
                    // and comments and random bits of text
                    if (param.getNodeType() == Node.ELEMENT_NODE) {
                        pelem.appendChild(param.cloneNode(true));
                    }
                    param = param.getNextSibling();
                }
            }
            if (pelem.getFirstChild() != null) {
                xml.getFirstChild().appendChild(pelem);
            }
        }

        return xml.toString();
    }

    protected enum GameType
    {
        // the match type constants are from parlor.game.data.GameConfig
        SEATED("gameType0", "0"),
        // seated continuous games are disabled for now
        // SEATED_CONT("gameType1", "1"),
        PARTY("gameType2", "2"),
        AVRG("gameType3", "2"); // AVRGs are sort of like party games

        GameType (String lookup, String matchType) {
            _lookup = lookup;
            _matchType = matchType;
        }

        public String getText () {
            return _dmsgs.xlate(_lookup);
        }

        public String getMatchType () {
            return _matchType;
        }

        protected String _lookup;
        protected String _matchType;
    }

    protected static class XMLConfig
    {
        public int minPlayers = 1;
        public int maxPlayers = 1;
        public boolean watchable = true;
        public GameType type;
        public String serverClass;
        public boolean agentMPOnly;
        public boolean noprogress;
        public String params;
    }

    protected int _minRow, _maxRow, _watchRow;
}
