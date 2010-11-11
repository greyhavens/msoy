//
// $Id$

package client.frame;

import java.util.Map;

import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookService.StoryKey;

import client.facebookbase.FacebookUtil;
import client.facebookbase.StoryFeeder;
import client.shell.CShell;

/**
 * Publishes a level up feed story.
 */
public class LevelUpFeeder extends StoryFeeder
{
    /**
     * Presents the user with a feed post dialog for telling their friends about advancing to the
     * given level.
     */
    public static void publishLevelup (int level)
    {
        new LevelUpFeeder(level).publish();
    }

    /**
     * Creates a new feeder to publish a levelup story for the given level.
     */
    protected LevelUpFeeder (int level)
    {
        super(new StoryKey(CShell.getAppId(), FacebookService.LEVELUP), PUB_IMAGES);
        _level = level;
    }

    @Override // from StoryFeeder
    protected void addMoreWildcards (Map<String, String> data)
    {
        data.put("level", "" + _level);
    }

    // Handy JSON for pasting into Facebook's template editor
    /*
      {
          "action_url": "http://apps.whirled.com/whirled/",
          "level": "19",
          "fbuid" : "loggedinuser",
          "images" : [ {"src" :
              "http://mediacloud.whirled.com/240aa9267fa6dc8422588e6818862301fd658e6f.png",
              "href" : "http://apps.whirled.com/whirled/"}]}
    */

    protected int _level;

    /** THIS WILL NOT WORK WITH SIGNED URL's -- however, I don't think it's in use. */
    protected static final String[] PUB_IMAGES = {
        FacebookUtil.PUB_ROOT + "5e96a1dd2244f0679834e4d5e6ba886bffabd9ff.png",
        FacebookUtil.PUB_ROOT + "d540aa4605b1b778f826262209723359d81fa06e.png",
        FacebookUtil.PUB_ROOT + "7bb4a311f008782516ab77c994ac232357f99547.png" };
}
