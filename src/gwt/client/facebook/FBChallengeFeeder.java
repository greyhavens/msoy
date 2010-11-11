//
// $Id$

package client.facebook;

import java.util.Map;

import com.threerings.msoy.facebook.gwt.FacebookGame;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookService.StoryKey;
import com.threerings.msoy.facebook.gwt.FacebookService.StoryFields;

import client.facebookbase.FacebookUtil;
import client.facebookbase.StoryFeeder;
import client.shell.CShell;
import client.util.Link;

/**
 * Publishes a challenge feed story.
 */
public class FBChallengeFeeder extends StoryFeeder
{
    /**
     * Presents the user with a feed post dialog for challenging their friends to the given game.
     * This overload is for use when the fields have already been received from the server.
     */
    public static void publishChallenge (FacebookGame game, StoryFields fields)
    {
        new FBChallengeFeeder(game, fields).doPublish();
    }

    /**
     * Presents the user with a feed post dialog for challenging their friends to the given game.
     */
    public static void publishChallenge (FacebookGame game)
    {
        new FBChallengeFeeder(game, null).publish();
    }

    protected FBChallengeFeeder (FacebookGame game, StoryFields fields)
    {
        super(new StoryKey(CShell.getAppId(), FacebookService.CHALLENGE, game), PUB_IMAGES);
        _fields = fields;
    }

    @Override // from StoryFeeder
    protected void onComplete (boolean success)
    {
        Link.go(_key.game.getPlayPage(), _key.game.getPlayArgs());
    }

    @Override // from StoryFeeder
    protected void addMoreWildcards (Map<String, String> data)
    {
        data.put("game", _fields.name);
        data.put("game_desc", _fields.description);
    }

    // Handy JSON for pasting into Facebook's template editor
    /*
      {
          "game" : "Corpse Craft",
          "fbuid" : "loggedinuser",
          "game_desc" :
              "Build an army of corpses to destroy your foes in this puzzle-action hybrid.",
          "action_url": "http://www.whirled.com/go/games-d_827",
          "images" : [ {"src" :
              "http://mediacloud.whirled.com/240aa9267fa6dc8422588e6818862301fd658e6f.png",
              "href" : "http://www.whirled.com/go/games-d_827_t"}]}
    */

    /** THIS WILL NOT WORK WITH SIGNED URL's -- however, I don't think it's in use. */
    protected static final String[] PUB_IMAGES = {
        FacebookUtil.PUB_ROOT + "db21cc504a1f735f96eb051275c1dd9d394924d2.png",
        FacebookUtil.PUB_ROOT + "e59ac92a6c497f573fcee451dedb59ce03cadf68.png",
        FacebookUtil.PUB_ROOT + "af525e4e1b026a46beeaf98ff824aa65e30d800a.png" };
}
