//
// $Id$

package client.util.images;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * This {@link ImageBundle} is used for all the button icons. Using an image bundle allows all of
 * these images to be packed into a single image, which saves a lot of HTTP requests, drastically
 * improving startup time.
 */
public interface RichTextToolbarImages extends ImageBundle
{
    /**
     * @gwt.resource bold.gif
     */
    AbstractImagePrototype bold();

    /**
     * @gwt.resource createLink.gif
     */
    AbstractImagePrototype createLink();

    /**
     * @gwt.resource hr.gif
     */
    AbstractImagePrototype hr();

    /**
     * @gwt.resource indent.gif
     */
    AbstractImagePrototype indent();

    /**
     * @gwt.resource insertImage.gif
     */
    AbstractImagePrototype insertImage();

    /**
     * @gwt.resource italic.gif
     */
    AbstractImagePrototype italic();

    /**
     * @gwt.resource justifyCenter.gif
     */
    AbstractImagePrototype justifyCenter();

    /**
     * @gwt.resource justifyLeft.gif
     */
    AbstractImagePrototype justifyLeft();

    /**
     * @gwt.resource justifyRight.gif
     */
    AbstractImagePrototype justifyRight();

    /**
     * @gwt.resource ol.gif
     */
    AbstractImagePrototype ol();

    /**
     * @gwt.resource outdent.gif
     */
    AbstractImagePrototype outdent();

    /**
     * @gwt.resource removeFormat.gif
     */
    AbstractImagePrototype removeFormat();

    /**
     * @gwt.resource removeLink.gif
     */
    AbstractImagePrototype removeLink();

    /**
     * @gwt.resource strikeThrough.gif
     */
    AbstractImagePrototype strikeThrough();

    /**
     * @gwt.resource subscript.gif
     */
    AbstractImagePrototype subscript();

    /**
     * @gwt.resource superscript.gif
     */
    AbstractImagePrototype superscript();

    /**
     * @gwt.resource ul.gif
     */
    AbstractImagePrototype ul();

    /**
     * @gwt.resource underline.gif
     */
    AbstractImagePrototype underline();
}
