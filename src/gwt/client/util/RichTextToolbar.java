/*
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.gwt.ui.WidgetUtil;

import client.shell.ShellMessages;
import client.images.editor.RichTextToolbarImages;

/**
 * A sample toolbar for use with {@link RichTextArea}. It provides a simple UI
 * for all rich text formatting, dynamically displayed only for the available
 * functionality.
 */
public class RichTextToolbar extends Composite
{
    /**
     * This {@link Constants} interface is used to make the toolbar's strings
     * internationalizable.
     */
    public interface Strings extends Constants
    {
        String black ();

        String blue ();

        String bold ();

        String color ();

        String createLink ();

        String font ();

        String green ();

        String hr ();

        String indent ();

        String insertImage ();

        String italic ();

        String justifyCenter ();

        String justifyLeft ();

        String justifyRight ();

        String large ();

        String medium ();

        String normal ();

        String ol ();

        String outdent ();

        String red ();

        String removeFormat ();

        String removeLink ();

        String size ();

        String small ();

        String strikeThrough ();

        String subscript ();

        String superscript ();

        String ul ();

        String underline ();

        String white ();

        String xlarge ();

        String xsmall ();

        String xxlarge ();

        String xxsmall ();

        String yellow ();
    }

    /**
     * Creates a new toolbar that drives the given rich text area.
     *
     * @param richText the rich text area to be controlled
     */
    public RichTextToolbar (RichTextArea richText, boolean allowPanelEdit)
    {
        this.richText = richText;
        this.basic = richText.getBasicFormatter();
        this.extended = richText.getExtendedFormatter();

        outer.add(topPanel);
        outer.add(bottomPanel);
        topPanel.setWidth("100%");
        bottomPanel.setWidth("100%");

        initWidget(outer);
        setStyleName("gwt-RichTextToolbar");

        if (basic != null) {
            topPanel.add(bold = createToggleButton(images.bold(), strings.bold()));
            topPanel.add(italic = createToggleButton(images.italic(), strings.italic()));
            topPanel.add(underline = createToggleButton(images.underline(),
                                                        strings.underline()));
            topPanel.add(subscript = createToggleButton(images.subscript(),
                                                        strings.subscript()));
            topPanel.add(superscript = createToggleButton(images.superscript(),
                                                          strings.superscript()));
            topPanel.add(justifyLeft = createPushButton(images.justifyLeft(),
                                                        strings.justifyLeft()));
            topPanel.add(justifyCenter = createPushButton(images.justifyCenter(),
                                                          strings.justifyCenter()));
            topPanel.add(justifyRight = createPushButton(images.justifyRight(),
                                                         strings.justifyRight()));
        }

        if (extended != null) {
            topPanel.add(strikethrough = createToggleButton(images.strikeThrough(),
                                                            strings.strikeThrough()));
            topPanel.add(indent = createPushButton(images.indent(), strings.indent()));
            topPanel.add(outdent = createPushButton(images.outdent(), strings.outdent()));
            topPanel.add(hr = createPushButton(images.hr(), strings.hr()));
            topPanel.add(ol = createPushButton(images.ol(), strings.ol()));
            topPanel.add(ul = createPushButton(images.ul(), strings.ul()));
            topPanel.add(insertImage = createPushButton(images.insertImage(),
                                                        strings.insertImage()));
            topPanel.add(createLink = createPushButton(images.createLink(),
                                                       strings.createLink()));
            topPanel.add(removeLink = createPushButton(images.removeLink(),
                                                       strings.removeLink()));
            topPanel.add(removeFormat = createPushButton(images.removeFormat(),
                                                         strings.removeFormat()));
        }

        if (basic != null) {
            bottomPanel.add(new Label("Text:"));
            bottomPanel.add(foreColors = createColorList("Foreground"));
            bottomPanel.add(fonts = createFontList());
            bottomPanel.add(fontSizes = createFontSizes());
            bottomPanel.add(blockFormats = createBlockFormats());

            if (allowPanelEdit) {
                bottomPanel.add(new Button("Panel Colors", new ClickListener() {
                    public void onClick (Widget sender) {
                        showPanelColorsPopup();
                    }
                }), HasAlignment.ALIGN_MIDDLE);
            }

            // We only use these listeners for updating status, so don't hook them up
            // unless at least basic editing is supported.
            richText.addKeyboardListener(listener);
            richText.addClickListener(listener);
        }
    }

    public String getTextColor ()
    {
        return _tcolor;
    }

    public String getBackgroundColor ()
    {
        return _bgcolor;
    }

    public void setPanelColors (String tcolor, String bgcolor)
    {
        _tcolor = tcolor;
        _bgcolor = bgcolor;

        // this may be called before we're added to the DOM, so we need to wait until our inner
        // iframe is created before trying to set its background color, etc.
        DeferredCommand.addCommand(new Command() {
            public void execute () {
                setPanelColorsImpl(richText.getElement(), (_tcolor == null) ? "" : _tcolor,
                                   (_bgcolor == null) ? "none" : _bgcolor);
            }
        });
    }

    public void setBlockFormat (String format)
    {
        setBlockFormatImpl(richText.getElement(), format);
    }

    @Override // from Widget
    protected void onAttach ()
    {
        super.onAttach();
        DeferredCommand.addCommand(new Command() {
            public void execute () {
                configureIFrame(richText.getElement());
            }
        });
    }

    protected ListBox createColorList (String caption)
    {
        ListBox lb = new ListBox();
        lb.addChangeListener(listener);
        lb.setVisibleItemCount(1);

        lb.addItem(caption);
        lb.addItem(strings.white(), "white");
        lb.addItem(strings.black(), "black");
        lb.addItem(strings.red(), "red");
        lb.addItem(strings.green(), "green");
        lb.addItem(strings.yellow(), "yellow");
        lb.addItem(strings.blue(), "blue");
        return lb;
    }

    protected ListBox createFontList ()
    {
        ListBox lb = new ListBox();
        lb.addChangeListener(listener);
        lb.setVisibleItemCount(1);

        lb.addItem(strings.font(), "");
        lb.addItem(strings.normal(), "");
        lb.addItem("Times New Roman", "Times New Roman");
        lb.addItem("Arial", "Arial");
        lb.addItem("Courier New", "Courier New");
        lb.addItem("Georgia", "Georgia");
        lb.addItem("Trebuchet", "Trebuchet");
        lb.addItem("Verdana", "Verdana");
        return lb;
    }

    protected ListBox createFontSizes ()
    {
        ListBox lb = new ListBox();
        lb.addChangeListener(listener);
        lb.setVisibleItemCount(1);

        lb.addItem(strings.size());
        lb.addItem(strings.xxsmall());
        lb.addItem(strings.xsmall());
        lb.addItem(strings.small());
        lb.addItem(strings.medium());
        lb.addItem(strings.large());
        lb.addItem(strings.xlarge());
        lb.addItem(strings.xxlarge());
        return lb;
    }

    protected ListBox createBlockFormats ()
    {
        ListBox lb = new ListBox();
        lb.addChangeListener(listener);
        lb.setVisibleItemCount(1);

        lb.addItem("Format");
        lb.addItem("Normal");
        lb.addItem("Code");
        lb.addItem("Header 1");
        lb.addItem("Header 2");
        lb.addItem("Header 3");
        lb.addItem("Header 4");
        lb.addItem("Header 5");
        lb.addItem("Header 6");
        return lb;
    }

    protected PushButton createPushButton (AbstractImagePrototype img, String tip)
    {
        PushButton pb = new PushButton(img.createImage());
        pb.addClickListener(listener);
        pb.setTitle(tip);
        return pb;
    }

    protected ToggleButton createToggleButton (AbstractImagePrototype img, String tip)
    {
        ToggleButton tb = new ToggleButton(img.createImage());
        tb.addClickListener(listener);
        tb.setTitle(tip);
        return tb;
    }

    /**
     * Updates the status of all the stateful buttons.
     */
    protected void updateStatus ()
    {
        if (basic != null) {
            bold.setDown(basic.isBold());
            italic.setDown(basic.isItalic());
            underline.setDown(basic.isUnderlined());
            subscript.setDown(basic.isSubscript());
            superscript.setDown(basic.isSuperscript());
        }

        if (extended != null) {
            strikethrough.setDown(extended.isStrikethrough());
        }
    }

    protected void showPanelColorsPopup ()
    {
        final BorderedPopup popup = new BorderedPopup();
        FlexTable contents = new FlexTable();
        contents.setCellSpacing(5);
        contents.setCellPadding(0);
        contents.setText(0, 0, "Enter panel colors (in hex ASCII format, e.g. #FFCC99):");
        contents.getFlexCellFormatter().setColSpan(0, 0, 2);

        contents.setText(1, 0, "Text color:");
        final TextBox tcolor = MsoyUI.createTextBox(_tcolor, 7, 7);
        contents.setWidget(1, 1, tcolor);

        contents.setText(2, 0, "Background color:");
        final TextBox bgcolor = MsoyUI.createTextBox(_bgcolor, 7, 7);
        contents.setWidget(2, 1, bgcolor);

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.add(new Button(_cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                popup.hide();
            }
        }));
        buttons.add(WidgetUtil.makeShim(5, 5));
        buttons.add(new Button(_cmsgs.update(), new ClickListener() {
            public void onClick (Widget sender) {
                setPanelColors(tcolor.getText().trim().toLowerCase(),
                               bgcolor.getText().trim().toLowerCase());
                popup.hide();
            }
        }));
        contents.setWidget(3, 0, buttons);
        contents.getFlexCellFormatter().setColSpan(3, 0, 2);
        contents.getFlexCellFormatter().setHorizontalAlignment(3, 0, HasAlignment.ALIGN_RIGHT);

        popup.setWidget(contents);
        popup.show();
    }

    protected static native void configureIFrame (Element elem) /*-{
        var ss = elem.contentWindow.document.createElement("link");
        ss.type = "text/css";
        ss.href = "http://" + $wnd.location.hostname + ":" + $wnd.location.port + "/css/global.css";
        ss.rel = "stylesheet";
        elem.contentWindow.document.getElementsByTagName("head")[0].appendChild(ss);
        ss = elem.contentWindow.document.createElement("link");
        ss.type = "text/css";
        ss.href = "http://" + $wnd.location.hostname + ":" + $wnd.location.port + "/css/editor.css";
        ss.rel = "stylesheet";
        elem.contentWindow.document.getElementsByTagName("head")[0].appendChild(ss);
    }-*/;

    protected static native void setPanelColorsImpl (
        Element elem, String tcolor, String bgcolor) /*-{
        elem.contentWindow.document.body.style['color'] = tcolor;
        elem.contentWindow.document.body.style['background'] = bgcolor;
    }-*/;

    protected static native void setBlockFormatImpl (Element elem, String format) /*-{
        elem.contentWindow.document.execCommand("FormatBlock", false, format);
    }-*/;

    /**
     * We use an inner EventListener class to avoid exposing event methods on the
     * RichTextToolbar itself.
     */
    protected class EventListener implements ClickListener, ChangeListener, KeyboardListener
    {
        public void onChange (Widget sender) {
            if (sender == foreColors) {
                basic.setForeColor(foreColors.getValue(foreColors.getSelectedIndex()));
                foreColors.setSelectedIndex(0);
            } else if (sender == fonts) {
                basic.setFontName(fonts.getValue(fonts.getSelectedIndex()));
                fonts.setSelectedIndex(0);
            } else if (sender == fontSizes) {
                basic.setFontSize(fontSizesConstants[fontSizes.getSelectedIndex() - 1]);
                fontSizes.setSelectedIndex(0);
            } else if (sender == blockFormats) {
                setBlockFormat(blockFormatConstants[blockFormats.getSelectedIndex() - 1]);
                blockFormats.setSelectedIndex(0);
            }
        }

        public void onClick (Widget sender) {
            if (sender == bold) {
                basic.toggleBold();
            } else if (sender == italic) {
                basic.toggleItalic();
            } else if (sender == underline) {
                basic.toggleUnderline();
            } else if (sender == subscript) {
                basic.toggleSubscript();
            } else if (sender == superscript) {
                basic.toggleSuperscript();
            } else if (sender == strikethrough) {
                extended.toggleStrikethrough();
            } else if (sender == indent) {
                extended.rightIndent();
            } else if (sender == outdent) {
                extended.leftIndent();
            } else if (sender == justifyLeft) {
                basic.setJustification(RichTextArea.Justification.LEFT);
            } else if (sender == justifyCenter) {
                basic.setJustification(RichTextArea.Justification.CENTER);
            } else if (sender == justifyRight) {
                basic.setJustification(RichTextArea.Justification.RIGHT);
            } else if (sender == insertImage) {
                ImageChooserPopup.displayImageChooser(false, new AsyncCallback<MediaDesc>() {
                    public void onSuccess (MediaDesc image) {
                        extended.insertImage(image.getMediaPath());
                    }
                    public void onFailure (Throwable t) {
                        // not used
                    }
                });
            } else if (sender == createLink) {
                String url = Window.prompt("Enter a link URL:", "http://");
                if (url != null) {
                    extended.createLink(url);
                }
            } else if (sender == removeLink) {
                extended.removeLink();
            } else if (sender == hr) {
                extended.insertHorizontalRule();
            } else if (sender == ol) {
                extended.insertOrderedList();
            } else if (sender == ul) {
                extended.insertUnorderedList();
            } else if (sender == removeFormat) {
                extended.removeFormat();
            } else if (sender == richText) {
                // We use the RichTextArea's onKeyUp event to update the toolbar status.
                // This will catch any cases where the user moves the cursur using the
                // keyboard, or uses one of the browser's built-in keyboard shortcuts.
                updateStatus();
            }
        }

        public void onKeyDown (Widget sender, char keyCode, int modifiers) {
        }

        public void onKeyPress (Widget sender, char keyCode, int modifiers) {
        }

        public void onKeyUp (Widget sender, char keyCode, int modifiers) {
            if (sender == richText) {
                // We use the RichTextArea's onKeyUp event to update the toolbar status.
                // This will catch any cases where the user moves the cursur using the
                // keyboard, or uses one of the browser's built-in keyboard shortcuts.
                updateStatus();
            }
        }
    }

    protected RichTextToolbarImages images = (RichTextToolbarImages)
        GWT.create(RichTextToolbarImages.class);
    protected Strings strings = (Strings) GWT.create(Strings.class);
    protected EventListener listener = new EventListener();

    protected RichTextArea richText;
    protected RichTextArea.BasicFormatter basic;
    protected RichTextArea.ExtendedFormatter extended;

    protected VerticalPanel outer = new VerticalPanel();
    protected HorizontalPanel topPanel = new HorizontalPanel();
    protected RowPanel bottomPanel = new RowPanel();
    protected ToggleButton bold;
    protected ToggleButton italic;
    protected ToggleButton underline;
    protected ToggleButton subscript;
    protected ToggleButton superscript;
    protected ToggleButton strikethrough;
    protected PushButton indent;
    protected PushButton outdent;
    protected PushButton justifyLeft;
    protected PushButton justifyCenter;
    protected PushButton justifyRight;
    protected PushButton hr;
    protected PushButton ol;
    protected PushButton ul;
    protected PushButton insertImage;
    protected PushButton createLink;
    protected PushButton removeLink;
    protected PushButton removeFormat;

    protected ListBox foreColors;
    protected ListBox fonts;
    protected ListBox fontSizes;
    protected ListBox blockFormats;

    protected String _tcolor, _bgcolor;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);

    protected static final RichTextArea.FontSize[] fontSizesConstants =
        new RichTextArea.FontSize[] {
        RichTextArea.FontSize.XX_SMALL, RichTextArea.FontSize.X_SMALL,
        RichTextArea.FontSize.SMALL, RichTextArea.FontSize.MEDIUM,
        RichTextArea.FontSize.LARGE, RichTextArea.FontSize.X_LARGE,
        RichTextArea.FontSize.XX_LARGE};

    protected static final String[] blockFormatConstants = new String[] {
        "<p>", "<pre>", "<h1>", "<h2>", "<h3>", "<h4>", "<h5>", "<h6>"
    };
}
