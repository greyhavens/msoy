//
// $Id$

package client.games;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;


import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.game.gwt.GameThumbnail;
import com.threerings.msoy.game.gwt.GameThumbnail.Type;

import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.games.EditThumbsPanel.ThumbnailSet;
import client.games.EditorUtil.ConfigException;

/**
 * Panel for editing Facebook feed story thumbnails.
 */
public class EditGlobalFeedThumbsPanel extends FlowPanel
{
    public EditGlobalFeedThumbsPanel ()
    {
        setStyleName("editFeedThumbs");
        add(MsoyUI.createLabel(_msgs.editFeedThumbnailsTitle(), "Title"));
        add(MsoyUI.createLabel(_msgs.editFeedThumbnailsTip(), "Tip"));
        _gamesvc.loadThumbnails(0, new InfoCallback<List<GameThumbnail>>() {
            public void onSuccess (List<GameThumbnail> result) {
                init(result);
            }
        });
    }

    public void init (List<GameThumbnail> result)
    {
        ThumbnailSet thumbnails = new ThumbnailSet(result);
        addPanel(_msgs.editFeedThumbnailsTrophy(), Type.TROPHY, thumbnails);
        addPanel(_msgs.editFeedThumbnailsChallenge(), Type.CHALLENGE, thumbnails);
        addPanel(_msgs.editFeedThumbnailsLevelUp(), Type.LEVELUP, thumbnails);

        Button save = new Button(_msgs.editFeedThumbnailsSave());
        add(save);
        new ClickCallback<Void>(save) {
            @Override protected boolean callService () {
                try {
                    List<GameThumbnail> thumbnails = new ArrayList<GameThumbnail>();
                    for (EditThumbsPanel panel : _panels) {
                        thumbnails.addAll(panel.getThumbnails());
                    }
                    _gamesvc.updateThumbnails(0, thumbnails, this);

                } catch (ConfigException e) {
                    MsoyUI.error(e.getMessage());
                    return false;
                }
                return true;
            }

            @Override protected boolean gotResult (Void result) {
                MsoyUI.info(_msgs.editFeedThumbnailsSaved());
                return true;
            }
        };
    }

    public void addPanel (String typeName, Type type, ThumbnailSet thumbnails)
    {
        EditThumbsPanel panel = new EditThumbsPanel(typeName, type, thumbnails);
        add(panel);
        _panels.add(panel);
    }

    protected List<EditThumbsPanel> _panels = new ArrayList<EditThumbsPanel>();

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
}
