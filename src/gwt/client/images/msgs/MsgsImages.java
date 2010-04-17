//
// $Id$

package client.images.msgs;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

/**
 * Contains images for our msgs services.
 */
public interface MsgsImages extends ClientBundle
{
    @Source("assign_issue.png")
    ImageResource assign_issue ();

    @Source("delete_post.png")
    ImageResource delete_post ();

    @Source("edit_post.png")
    ImageResource edit_post ();

    @Source("new_issue.png")
    ImageResource new_issue ();

    @Source("reply_post.png")
    ImageResource reply_post ();

    @Source("reply_post_quote.png")
    ImageResource reply_post_quote ();

    @Source("view_issue.png")
    ImageResource view_issue ();

    @Source("rss.png")
    ImageResource rss ();

    @Source("complain_post.png")
    ImageResource complain_post ();

    @Source("sendmail.png")
    ImageResource sendmail ();

    @Source("thdown_default.png")
    ImageResource thumb_down_default();

    @Source("thup_default.png")
    ImageResource thumb_up_default();

    @Source("thdown_over.png")
    ImageResource thumb_down_over();

    @Source("thup_over.png")
    ImageResource thumb_up_over();

}
