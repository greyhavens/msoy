//
// $Id$

package client.frame;

import client.shell.CShell;
import client.util.events.FlashEvents;
import client.util.events.TrophyEvent;

/**
 * Handles publishing events to external feeds like Facebook or OpenSocial.
 */
public class ExternalFeeder
{
    public ExternalFeeder ()
    {
        FlashEvents.addListener(new TrophyEvent.Listener() {
            public void trophyEarned (TrophyEvent event) {
                publishTrophyToFacebook(event);
            }
        });
    }

    protected void publishTrophyToFacebook (TrophyEvent event)
    {
        CShell.log("Want to publish " + event.getName() + " " + event.getGameId());
    }

    protected static native void testFBConnect () /*-{
        var ids = new Array();
        var data = eval({
            'trophy':'Stellar',
            'game':'Dictionary Attack',
            'descrip':'Score 30 points or higher in 5 games in a row in a single session.',
            'game_id':'8',
            'images':[{'src':'http://mediacloud.whirled.com/f3c8aee41c9bc3eb4d371bd0bd0703f5a1d2f309.png','href':'http://www.whirled.com/#games-d_8_t'}],
        });
        $wnd.FB_PostStory(41625542934, data, ids, "");
    }-*/;
}
