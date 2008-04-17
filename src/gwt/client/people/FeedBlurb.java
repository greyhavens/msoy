//
// $Id$

package client.people;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.client.ProfileService;

import client.msgs.FeedPanel;

/**
 * Displays a member feed on the member's profile
 */
public class FeedBlurb extends Blurb
{
    // @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return true;
    }

    // @Override // from Blurb
    public void init (final ProfileService.ProfileResult pdata)
    {
        super.init(pdata);

        setHeader(CPeople.msgs.feedTitle());
        String empty = CPeople.msgs.emptySelfFeed();
        FeedPanel feed = new FeedPanel(empty, false, new FeedPanel.FeedLoader() {
            public void loadFeed (int feedDays, AsyncCallback callback) {
                CPeople.profilesvc.loadSelfFeed(pdata.name.getMemberId(), feedDays, callback);
            }
        });
        feed.setFeed(pdata.feed, false);
        setContent(feed);
    }
}
