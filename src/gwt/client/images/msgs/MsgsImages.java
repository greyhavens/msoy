//
// $Id$

package client.images.msgs;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * Contains images for our msgs services.
 */
public interface MsgsImages extends ImageBundle
{
    @Resource("assign_issue.png")
    AbstractImagePrototype assign_issue ();

    @Resource("delete_post.png")
    AbstractImagePrototype delete_post ();

    @Resource("edit_post.png")
    AbstractImagePrototype edit_post ();

    @Resource("new_issue.png")
    AbstractImagePrototype new_issue ();

    @Resource("reply_post.png")
    AbstractImagePrototype reply_post ();

    @Resource("reply_post_quote.png")
    AbstractImagePrototype reply_post_quote ();

    @Resource("view_issue.png")
    AbstractImagePrototype view_issue ();

    @Resource("rss.png")
    AbstractImagePrototype rss ();

    @Resource("complain_post.png")
    AbstractImagePrototype complain_post ();
}
