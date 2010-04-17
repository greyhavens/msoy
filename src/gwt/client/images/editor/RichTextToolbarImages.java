//
// $Id$

package client.images.editor;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

/**
 * This {@link ClientBundle} is used for all the button icons. Using an image bundle allows all of
 * these images to be packed into a single image, which saves a lot of HTTP requests, drastically
 * improving startup time.
 */
public interface RichTextToolbarImages extends ClientBundle
{
    @Source("bold.png")
    ImageResource bold();

    @Source("createLink.png")
    ImageResource createLink();

    @Source("hr.png")
    ImageResource hr();

    @Source("indent.png")
    ImageResource indent();

    @Source("insertImage.png")
    ImageResource insertImage();

    @Source("italic.png")
    ImageResource italic();

    @Source("justifyCenter.png")
    ImageResource justifyCenter();

    @Source("justifyLeft.png")
    ImageResource justifyLeft();

    @Source("justifyRight.png")
    ImageResource justifyRight();

    @Source("ol.png")
    ImageResource ol();

    @Source("outdent.png")
    ImageResource outdent();

    @Source("removeFormat.png")
    ImageResource removeFormat();

    @Source("removeLink.png")
    ImageResource removeLink();

    @Source("strikeThrough.png")
    ImageResource strikeThrough();

    @Source("subscript.png")
    ImageResource subscript();

    @Source("superscript.png")
    ImageResource superscript();

    @Source("ul.png")
    ImageResource ul();

    @Source("underline.png")
    ImageResource underline();
}
