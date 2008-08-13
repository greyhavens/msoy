//
// $Id$

package client.images.editor;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * This {@link ImageBundle} is used for all the button icons. Using an image bundle allows all of
 * these images to be packed into a single image, which saves a lot of HTTP requests, drastically
 * improving startup time.
 */
public interface RichTextToolbarImages extends ImageBundle
{
    @Resource("bold.gif")
    AbstractImagePrototype bold();

    @Resource("createLink.gif")
    AbstractImagePrototype createLink();

    @Resource("hr.gif")
    AbstractImagePrototype hr();

    @Resource("indent.gif")
    AbstractImagePrototype indent();

    @Resource("insertImage.gif")
    AbstractImagePrototype insertImage();

    @Resource("italic.gif")
    AbstractImagePrototype italic();

    @Resource("justifyCenter.gif")
    AbstractImagePrototype justifyCenter();

    @Resource("justifyLeft.gif")
    AbstractImagePrototype justifyLeft();

    @Resource("justifyRight.gif")
    AbstractImagePrototype justifyRight();

    @Resource("ol.gif")
    AbstractImagePrototype ol();

    @Resource("outdent.gif")
    AbstractImagePrototype outdent();

    @Resource("removeFormat.gif")
    AbstractImagePrototype removeFormat();

    @Resource("removeLink.gif")
    AbstractImagePrototype removeLink();

    @Resource("strikeThrough.gif")
    AbstractImagePrototype strikeThrough();

    @Resource("subscript.gif")
    AbstractImagePrototype subscript();

    @Resource("superscript.gif")
    AbstractImagePrototype superscript();

    @Resource("ul.gif")
    AbstractImagePrototype ul();

    @Resource("underline.gif")
    AbstractImagePrototype underline();
}
