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

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.StringUtil;

import com.threerings.msoy.facebook.gwt.FacebookInfo;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.game.gwt.GameThumbnail;
import com.threerings.msoy.game.gwt.GameThumbnail.Type;

import client.games.EditThumbsPanel.ThumbnailSet;
import client.games.EditorUtil.ConfigException;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.InfoCallback;

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

        final EditorTable thumbsEditor = new EditorTable(); // just for the "Save" button

        _gamesvc.loadThumbnails(info.gameId, new InfoCallback<List<GameThumbnail>>() {
            @Override public void onSuccess (List<GameThumbnail> result) {
                Runnable onMediaModified = new Runnable () {
                    public void run () {
                        thumbsEditor.mediaModified();
                    }
                };
                ThumbnailSet thumbnails = new ThumbnailSet(result);
                _thumbsPanels.add(new EditThumbsPanel(_msgs.editFeedThumbnailsTrophy(),
                    Type.TROPHY, thumbnails, onMediaModified));
                _thumbsPanels.add(new EditThumbsPanel(_msgs.editFeedThumbnailsChallenge(),
                    Type.CHALLENGE, thumbnails, onMediaModified));
            }
        });

        add(WidgetUtil.makeShim(1, 20));
        add(MsoyUI.createLabel(_msgs.fieThumbnailsTitle(), "Title"));
        add(MsoyUI.createLabel(_msgs.fieThumbnailsTip(), "Tip"));
        add(_thumbsPanels = new FlowPanel());

        add(thumbsEditor);
        Button saveThumbs = thumbsEditor.addSaveRow();
        new ClickCallback<Void>(saveThumbs) {
            protected boolean callService () {
                try {
                    _gamesvc.updateThumbnails(info.gameId, getThumbnails(), this);
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
        List<GameThumbnail> thumbnails = new ArrayList<GameThumbnail>();
        for (int ii = 0, ll = _thumbsPanels.getWidgetCount(); ii < ll; ++ii) {
            EditThumbsPanel panel = (EditThumbsPanel)_thumbsPanels.getWidget(ii);
            thumbnails.addAll(panel.getThumbnails());
        }
        return thumbnails;
    }

    protected void updateAppLink (FacebookInfo info)
    {
        _mainFields.setWidget(_viewRow, 1, MsoyUI.createHTML(_msgs.fieViewApp(info.apiKey), null));
        _mainFields.getRowFormatter().setVisible(_viewRow, !StringUtil.isBlank(info.apiKey));
    }

    protected EditorTable _mainFields;
    FlowPanel _thumbsPanels;
    protected int _viewRow;

    protected static final int THUMB_COLS = 2;
    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
