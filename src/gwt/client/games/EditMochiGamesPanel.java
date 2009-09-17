//
// $Id$

package client.games;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.StringUtil;

import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.game.gwt.MochiGameInfo;
import com.threerings.msoy.game.gwt.GameService.MochiGameBucket;

import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.InfoCallback;

/**
 * A panel for editing the mochi games featured on the facebook portal.
 */
public class EditMochiGamesPanel extends FlowPanel
{
    public static final int BUCKET_COUNT = 5;

    /**
     * Creates a new panel.
     */
    public EditMochiGamesPanel ()
    {
        setStyleName("editMochiGames");

        // The new style:
        // mochi games are kept in a series of "buckets". each bucket has a list of games and a
        // currently selected game. each day or other time period defined on the server, the
        // current game for each bucket is moved to the next game in the list. there are 5 buckets,
        // the current bucket 1 game is shown on the front page of the portal, bucket 2 second and
        // and so on

        // open the page with bucket 1
        setBucket(1, "");
    }

    protected void setBucket (final int bucket, String tags)
    {
        clear();

        // a list box to change the displayed bucket
        HorizontalPanel topBits = new HorizontalPanel();
        add(topBits);

        final ListBox buckets = new ListBox();
        topBits.setStyleName("Buckets");
        topBits.add(MsoyUI.createLabel(_msgs.emgBucketLabel(), null));
        topBits.add(WidgetUtil.makeShim(10, 10));
        topBits.add(buckets);

        for (int ii = 1; ii <= BUCKET_COUNT; ++ii) {
            buckets.addItem(_msgs.emgBucketName(String.valueOf(ii)));
            if (ii == bucket) {
                buckets.setSelectedIndex(buckets.getItemCount() - 1);
            }
        }

        buckets.addChangeHandler(new ChangeHandler() {
            @Override public void onChange (ChangeEvent event) {
                setBucket(buckets.getSelectedIndex() + 1, "");
            }
        });

        // a table of the currently selected bucket contents (downloaded asynchronously)
        add(MsoyUI.createLabel(_msgs.emgGamesTitle(""+bucket), "Title"));
        add(MsoyUI.createLabel(_msgs.emgGamesTip(), "Tip"));
        add(_games = new SmartTable("Games", 0, 5));
        _gamesvc.getMochiBucket(bucket, new InfoCallback<MochiGameBucket>() {
            @Override public void onSuccess (MochiGameBucket result) {
                setBucket(result);
            }
        });

        // an interface for uploading a new list of tags for the currently selected bucket
        add(MsoyUI.createLabel(_msgs.emgUpdateBucketTitle(""+bucket), "Title"));
        add(MsoyUI.createLabel(_msgs.emgUpdateBucketTip(), "Tip"));
        final TextArea mochiTags = MsoyUI.createTextArea(tags, 17, 8);
        final Button update = new Button(_msgs.emgUpdateBucketBtn());
        new ClickCallback<List<String>>(update) {
            @Override protected boolean callService () {
                String[] tags = mochiTags.getText().split("(\r|\n| |\t)+");
                if (tags.length == 0) {
                    MsoyUI.error(_msgs.emgNoGamesFoundErr());
                    return false;
                }
                for (String tag : tags) {
                    if (!tag.matches("[0-9a-fA-F]{16}")) {
                        MsoyUI.error(_msgs.emgGameTagFormatErr(tag));
                        return false;
                    }
                }
                _gamesvc.setMochiBucketTags(bucket, tags, this);
                return true;
            }

            @Override protected boolean gotResult (List<String> result) {
                if (result.size() == 0) {
                    MsoyUI.info(_msgs.emgBucketUpdated());
                    setBucket(bucket, "");
                } else {
                    MsoyUI.info(_msgs.emgBucketImportErr());
                    setBucket(bucket, StringUtil.join(result, "\n"));
                }
                return true;
            }
        };
        add(mochiTags);
        add(update);
    }

    protected void setBucket (MochiGameBucket bucket)
    {
        if (bucket.games.size() == 0) {
            _games.setText(0, 0, _msgs.emgNoGames());
            return;
        }

        // show the game names with index an tag, top to bottom on the left, then right
        _games.setText(0, 0, _msgs.emgIdxHeader());
        _games.setText(0, 1, _msgs.emgNameHeader());
        _games.setText(0, 2, _msgs.emgTagHeader());
        _games.setText(0, 3, _msgs.emgIdxHeader());
        _games.setText(0, 4, _msgs.emgNameHeader());
        _games.setText(0, 5, _msgs.emgTagHeader());
        _games.getRowFormatter().setStyleName(0, "Header");
        int row = 1, col = 0, idx = 1, half = bucket.games.size() / 2 + 1;
        for (MochiGameInfo game : bucket.games) {
            boolean current = game.tag.equals(bucket.currentTag);
            _games.setText(row, col, (idx++) + (current ? "*" : ""));
            _games.setText(row, col + 1, game.name);
            _games.setText(row++, col + 2, game.tag);
            if (col == 0 && row > half) {
                row = 1;
                col = 3;
            }
        }
    }

    protected SmartTable _games;
    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
}
