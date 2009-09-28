//
// $Id$

package client.edgames;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.edgame.gwt.EditGameService;
import com.threerings.msoy.edgame.gwt.EditGameServiceAsync;
import com.threerings.msoy.facebook.gwt.FeedThumbnail;

import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.edutil.ThumbnailSet;
import client.edutil.EditorUtil.ConfigException;

/**
 * Panel for editing Facebook feed story thumbnails.
 */
public class EditGlobalFeedThumbsPanel extends FlowPanel
{
    // TODO: move these somewhere shared
    public static final String TROPHY = "trophy";
    public static final String CHALLENGE = "challenge";
    public static final String LEVELUP = "levelup";

    public EditGlobalFeedThumbsPanel ()
    {
        setStyleName("editFeedThumbs");
        add(MsoyUI.createLabel(_msgs.editFeedThumbnailsTitle(), "Title"));
        add(MsoyUI.createLabel(_msgs.editFeedThumbnailsTip(), "Tip"));
        _gamesvc.loadFeedThumbnails(0, new InfoCallback<List<FeedThumbnail>>() {
            public void onSuccess (List<FeedThumbnail> result) {
                init(result);
            }
        });
    }

    public void init (List<FeedThumbnail> result)
    {
        ThumbnailSet thumbnails = new ThumbnailSet(result);
        addPanel(_msgs.editFeedThumbnailsTrophy(), TROPHY, thumbnails);
        addPanel(_msgs.editFeedThumbnailsChallenge(), CHALLENGE, thumbnails);
        addPanel(_msgs.editFeedThumbnailsLevelUp(), LEVELUP, thumbnails);

        Button save = new Button(_msgs.editFeedThumbnailsSave());
        add(save);
        new ClickCallback<Void>(save) {
            @Override protected boolean callService () {
                try {
                    List<FeedThumbnail> thumbnails = new ArrayList<FeedThumbnail>();
                    for (EditThumbsPanel panel : _panels) {
                        thumbnails.addAll(panel.getThumbnails());
                    }
                    _gamesvc.updateFeedThumbnails(0, thumbnails, this);

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

    public void addPanel (String typeName, String code, ThumbnailSet thumbnails)
    {
        EditThumbsPanel panel = new EditThumbsPanel(typeName, code, thumbnails);
        add(panel);
        _panels.add(panel);
    }

    protected List<EditThumbsPanel> _panels = new ArrayList<EditThumbsPanel>();

    protected static final EditGameServiceAsync _gamesvc = GWT.create(EditGameService.class);
    protected static final EditGamesMessages _msgs = GWT.create(EditGamesMessages.class);
}
