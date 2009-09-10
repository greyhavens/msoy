//
// $Id$

package client.games;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.FacebookInfo;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.game.gwt.GameThumbnail;

import client.games.EditorUtil.ConfigException;
import client.games.EditorUtil.MediaBox;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.util.StringUtil;

/**
 * Displays and allows editing of a game's Facebook info.
 */
public class FacebookInfoEditorPanel extends FlowPanel
{
    public FacebookInfoEditorPanel (final FacebookInfo info)
    {
        setStyleName("fie");
        add(_mainFields = new EditorTable());
        _mainFields.addWidget(MsoyUI.createHTML(_msgs.fieIntro(), null), 2);

        _mainFields.addSpacer();

        _viewRow = _mainFields.addRow("", MsoyUI.createHTML("", null), null);
        updateAppLink(info);

        final TextBox key = MsoyUI.createTextBox(
            info.apiKey, FacebookInfo.KEY_LENGTH, FacebookInfo.KEY_LENGTH);
        _mainFields.addRow(_msgs.fieKey(), key, new Command() {
            public void execute () {
                info.apiKey = key.getText().trim();
            }
        });

        final TextBox secret = MsoyUI.createTextBox(
            info.appSecret, FacebookInfo.SECRET_LENGTH, FacebookInfo.SECRET_LENGTH);
        _mainFields.addRow(_msgs.fieSecret(), secret, new Command() {
            public void execute () {
                info.appSecret = secret.getText().trim();
            }
        });

        final TextBox canvasName = MsoyUI.createTextBox(
            info.canvasName, FacebookInfo.CANVAS_NAME_LENGTH, FacebookInfo.CANVAS_NAME_LENGTH);
        _mainFields.addRow(_msgs.fieCanvasName(), canvasName, new Command() {
            public void execute () {
                info.canvasName = canvasName.getText().trim();
            }
        });

        final CheckBox chromeless = new CheckBox(_msgs.fieChromelessText());
        chromeless.setValue(info.chromeless);
        _mainFields.addRow(_msgs.fieChromeless(), chromeless, new Command() {
            public void execute () {
                info.chromeless = chromeless.getValue();
            }
        });

        _mainFields.addSpacer();

        Button save = _mainFields.addSaveRow();
        new ClickCallback<Void>(save) {
            protected boolean callService () {
                if (!_mainFields.bindChanges()) {
                    return false;
                }
                _gamesvc.updateFacebookInfo(info, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                MsoyUI.info(_msgs.fieInfoUpdated());
                updateAppLink(info);
                return true;
            }
        };

        _gamesvc.loadAdditionalThumbnails(info.gameId, new InfoCallback<List<GameThumbnail>>() {
            @Override public void onSuccess (List<GameThumbnail> result) {
                setThumbnails(result);
            }
        });

        add(WidgetUtil.makeShim(1, 20));
        add(MsoyUI.createLabel(_msgs.fieThumbnailsTitle(), "Title"));
        add(MsoyUI.createLabel(_msgs.fieThumbnailsTip(), "Tip"));
        add(_thumbsTable = new SmartTable("thumbnails", 0, 10));
        add(_thumbsEditor = new EditorTable());
        Button saveThumbs = _thumbsEditor.addSaveRow();
        new ClickCallback<Void>(saveThumbs) {
            protected boolean callService () {
                try {
                    _gamesvc.updateAdditionalThumbnails(info.gameId, getThumbnails(), this);
                } catch (ConfigException e) {
                    MsoyUI.error(e.getMessage());
                    return false;
                }
                return true;
            }
            protected boolean gotResult (Void result) {
                MsoyUI.info(_msgs.fieThumbnailsUpdated());
                return true;
            }
        };
    }

    protected List<GameThumbnail> getThumbnails ()
    {
        List<GameThumbnail> thumbs = new ArrayList<GameThumbnail>();
        byte pos = 0;
        // TODO: variants?
        String variant = "";
        GameThumbnail.Type type = GameThumbnail.Type.GAME;
        for (MediaBox box : _thumbBoxes) {
            if (box.getMedia() != null) {
                thumbs.add(new GameThumbnail(EditorUtil.checkImageMedia(
                    _msgs.egShot(), box.getMedia()), type, variant, pos++));
            }
        }
        return thumbs;
    }

    protected void setThumbnails (List<GameThumbnail> thumbnails)
    {
        _thumbsTable.clear();
        final int size = MediaDesc.FB_FEED_SIZE;
        for (int ii = 0; ii < GameThumbnail.Type.GAME.count; ++ii) {
            EditorUtil.MediaBox box = new EditorUtil.MediaBox(size, "thumb" + ii,
                ii < thumbnails.size() ? thumbnails.get(ii).media : null) {
                @Override public void mediaUploaded (String name, MediaDesc desc, int w, int h) {
                    int targetW = MediaDesc.getWidth(size), targetH = MediaDesc.getHeight(size);
                    if (w != targetW || h != targetH) {
                        MsoyUI.error(_msgs.errInvalidShot(
                            String.valueOf(targetW), String.valueOf(targetH)));
                    } else {
                        super.mediaUploaded(name, desc, w, h);
                    }
                }
                @Override protected void mediaModified () {
                    _thumbsEditor.mediaModified();
                }
            };
            _thumbsTable.setWidget(ii / THUMB_COLS, ii % THUMB_COLS, box);
            _thumbBoxes.add(box);
        }
    }

    protected void updateAppLink (FacebookInfo info)
    {
        _mainFields.setWidget(_viewRow, 1, MsoyUI.createHTML(_msgs.fieViewApp(info.apiKey), null));
        _mainFields.getRowFormatter().setVisible(_viewRow, !StringUtil.isBlank(info.apiKey));
    }

    protected EditorTable _mainFields;
    protected SmartTable _thumbsTable;
    protected EditorTable _thumbsEditor; // just used for the "Save" button
    protected List<MediaBox> _thumbBoxes = new ArrayList<MediaBox>();
    protected int _viewRow;

    protected static final int THUMB_COLS = 2;
    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
