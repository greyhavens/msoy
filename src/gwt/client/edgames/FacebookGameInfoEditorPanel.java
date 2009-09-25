//
// $Id$

package client.edgames;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.edgame.gwt.EditGameService;
import com.threerings.msoy.edgame.gwt.EditGameServiceAsync;
import com.threerings.msoy.facebook.gwt.FacebookInfo;
import com.threerings.msoy.game.gwt.GameThumbnail;
import com.threerings.msoy.game.gwt.GameThumbnail.Type;

import client.edgames.EditThumbsPanel.ThumbnailSet;
import client.edutil.EditorTable;
import client.edutil.EditorUtil.ConfigException;
import client.edutil.FacebookInfoEditorPanel;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.InfoCallback;

/**
 * Displays and allows editing of a game's Facebook info.
 */
public class FacebookGameInfoEditorPanel extends FlowPanel
{
    public FacebookGameInfoEditorPanel (final FacebookInfo info)
    {
        setStyleName("fie");
        add(new FacebookInfoEditorPanel(info) {
            @Override protected boolean showChromeless () {
                return true;
            }
            @Override protected void saveInfo (FacebookInfo info, AsyncCallback<Void> callback) {
                _gamesvc.updateFacebookInfo(info, callback);
            }
            @Override protected String getIntro () {
                return FacebookGameInfoEditorPanel._msgs.fieIntro();
            }
        });

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

    protected FlowPanel _thumbsPanels;
    protected int _viewRow;

    protected static final int THUMB_COLS = 2;
    protected static final EditGamesMessages _msgs = GWT.create(EditGamesMessages.class);
    protected static final EditGameServiceAsync _gamesvc = GWT.create(EditGameService.class);
}
