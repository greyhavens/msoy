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
    @Resource("bold.png")
    AbstractImagePrototype bold();

    @Resource("createLink.png")
    AbstractImagePrototype createLink();

    @Resource("hr.png")
    AbstractImagePrototype hr();

    @Resource("indent.png")
    AbstractImagePrototype indent();

    @Resource("insertImage.png")
    AbstractImagePrototype insertImage();

    @Resource("italic.png")
    AbstractImagePrototype italic();

    @Resource("justifyCenter.png")
    AbstractImagePrototype justifyCenter();

    @Resource("justifyLeft.png")
    AbstractImagePrototype justifyLeft();

    @Resource("justifyRight.png")
    AbstractImagePrototype justifyRight();

    @Resource("ol.png")
    AbstractImagePrototype ol();

    @Resource("outdent.png")
    AbstractImagePrototype outdent();

    @Resource("removeFormat.png")
    AbstractImagePrototype removeFormat();

    @Resource("removeLink.png")
    AbstractImagePrototype removeLink();

    @Resource("strikeThrough.png")
    AbstractImagePrototype strikeThrough();

    @Resource("subscript.png")
    AbstractImagePrototype subscript();

    @Resource("superscript.png")
    AbstractImagePrototype superscript();

    @Resource("ul.png")
    AbstractImagePrototype ul();

    @Resource("underline.png")
    AbstractImagePrototype underline();
}
