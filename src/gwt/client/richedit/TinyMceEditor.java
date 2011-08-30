//
// $Id$

package client.richedit;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.gwt.CssUtil;
import com.threerings.msoy.web.gwt.MessageUtil;

import client.util.JavaScriptUtil;

/**
 * An html message editor implemented using the tinymce library.
 * @see http://tinymce.moxiecode.com
 */
public class TinyMceEditor extends FlowPanel
    implements MessageEditor.Panel
{
    /**
     * Creates a new editor.
     */
    public TinyMceEditor (boolean enablePanelColor)
    {
        Map<String, Object> settings = Maps.newHashMap();
        settings.put("mode", "none");
        settings.put("theme", "advanced");
        settings.put("extended_valid_elements", "b/strong");
        settings.put("relative_urls", JavaScriptUtil.makeBoolean(false));
        settings.put("convert_urls", JavaScriptUtil.makeBoolean(false));
        settings.put("gecko_spellcheck", JavaScriptUtil.makeBoolean(true));
        settings.put("theme_advanced_buttons1", new CommaList()
            .add("bold")
            .add("italic")
            .add("underline")
            .add("sub")
            .add("sup")
            .add("justifyleft")
            .add("justifycenter")
            .add("justifyright")
            .add("strikethrough")
            .add("indent")
            .add("outdent")
            .add("hr")
            .add("numlist")
            .add("bullist")
            .add("removeformat")
            .add("link")
            .add("unlink")
            .add("image")
            .build());
        settings.put("theme_advanced_buttons2", new CommaList()
            .add("forecolor")
            .add("fontselect")
            .add("fontsizeselect")
            .add("formatselect")
            .add("blockquote")
            .add("code")
            .add("panelcolor") // no-op if we don't add the plugin below
            .build());
        settings.put("theme_advanced_buttons3", "");
        settings.put("theme_advanced_blockformats", new CommaList()
            .add("p")
            .add("div")
            .add("pre")
            .add("h1")
            .add("h2")
            .add("h3")
            .add("h4")
            .add("h5")
            .add("h6")
            .build());
        settings.put("theme_advanced_toolbar_location", "top");
        settings.put("theme_advanced_fonts", new PairList()
            .add("Times New Roman", "times")
            .add("Arial", "arial")
            .add("Monospace", "monospace")
            .add("Georgia", "georgia")
            .add("Trebuchet", "trebuchet")
            .add("Verdana", "verdana")
            .build());
        settings.put("content_css", CssUtil.GLOBAL_PATH);

        if (enablePanelColor) {
            settings.put("plugins", "panelcolor");
        }

        initializeTinyMCE(JavaScriptUtil.createDictionaryFromMap(settings));

        add(_text = new FlowPanel());
        _text.setWidth("100%");
        _text.setHeight("300px");
        _text.getElement().setId("msoy_tinymce");
    }

    @Override // from MessageEditor.Panel
    public String getHTML ()
    {
        return getHTML(getId());
    }

    @Override // from MessageEditor.Panel
    public void setHTML (String html)
    {
        // this can get called before the editor is attached, so postpone if necessary
        html = MessageUtil.preEditMessage(html);
        if (!_ready) {
            _queuedHTML = html;
        } else {
            setHTML(getId(), html);
        }
    }

    @Override // from MessageEditor.Panel
    public void setFocus (boolean focus)
    {
        // TODO
    }

    @Override // from MessageEditor.Panel
    public Widget asWidget ()
    {
        return this;
    }

    @Override // from MessageEditor.Panel
    public void selectAll ()
    {
        // TODO
    }

    @Override // from MessageEditor.Panel
    public Button getToggler ()
    {
        return null;
    }

    @Override // from MessageEditor.Panel
    public String getPanelColor ()
    {
        return _panelColor;
    }

    @Override // from MessageEditor.Panel
    public void setPanelColor (String color)
    {
        _panelColor = color;
        setPanelColor(getId(), color);
    }

    @Override // from Widget
    protected void onAttach ()
    {
        super.onAttach();
        attachEditor(getId());
    }

    @Override // from Widget
    protected void onDetach ()
    {
        super.onDetach();
        removeEditor(getId());
    }

    protected String getId ()
    {
        return _text.getElement().getId();
    }

    protected void editorReady ()
    {
        _ready = true;
        if (_queuedHTML != null) {
            setHTML(_queuedHTML);
            _queuedHTML = null;
        }
        if (_panelColor != null) {
            setPanelColor(getId(), _panelColor);
        }
    }

    protected void handlePanelColorChange (String color)
    {
        _panelColor = color;
    }

    protected native void initializeTinyMCE(JavaScriptObject settings) /*-{
        try {
            $wnd.tinyMCE.init(settings);
        } catch (e) {
            if ($wnd.console) {
                $wnd.console.info("Failed to initializeTinyMCE", e);
            }
        }
    }-*/;

    protected native boolean attachEditor(String itemId) /*-{
        try {
            var t = this;
            var onPanelColorChanged = function (ed, color) {
                t.@client.richedit.TinyMceEditor::handlePanelColorChange(Ljava/lang/String;)(color);
            };
            // NB "window.tinymce" is the package, "window.tinyMCE" is the singleton
            var ed = new $wnd.tinymce.Editor(itemId, $wnd.tinyMCE.settings);
            ed.onInit.add(function(ed) {
                t.@client.richedit.TinyMceEditor::editorReady()();
                if (ed.onPanelColorChanged) {
                    ed.onPanelColorChanged.add(onPanelColorChanged);
                }
            });
            ed.render();
            return true;
        } catch (e) {
            if ($wnd.console) {
                $wnd.console.info("Failed to attachEditor", e);
            }
            return false;
        }
    }-*/;

    protected native boolean removeEditor(String itemId) /*-{
        try {
            return $wnd.tinyMCE.execCommand("mceRemoveControl", false, itemId);
        } catch (e) {
            if ($wnd.console) {
                $wnd.console.info("Failed to removeEditor", e);
            }
            return false;
        }
    }-*/;

    protected native String getHTML (String id) /*-{
        try {
            return $wnd.tinyMCE.get(id).getContent();
        } catch (e) {
            if ($wnd.console) {
                $wnd.console.info("Failed to getHTML", e);
            }
            return "";
        }
    }-*/;

    protected native void setHTML (String id, String html) /*-{
        try {
            $wnd.tinyMCE.get(id).setContent(html);
        } catch (e) {
            if ($wnd.console) {
                $wnd.console.info("Failed to setHTML", e);
            }
        }
    }-*/;

    protected native void setPanelColor (String id, String color) /*-{
        try {
            $wnd.tinyMCE.get(id).execCommand("setPanelColor", false, color);
        } catch (e) {
            if ($wnd.console) {
                $wnd.console.info("Failed to setPanelColor", e);
            }
        }
    }-*/;

    protected static class StringList
    {
        public StringList (char sep)
        {
            _sep = sep;
        }

        public String build ()
        {
            return _list.toString();
        }

        protected StringBuilder newItem ()
        {
            if (_list.length() > 0) {
                _list.append(_sep);
            }
            return _list;
        }

        protected char _sep;
        protected StringBuilder _list = new StringBuilder();
    }

    protected static class CommaList extends StringList
    {
        public CommaList ()
        {
            super(',');
        }

        public CommaList add (String item)
        {
            newItem().append(item);
            return this;
        }
    }

    protected static class PairList extends StringList
    {
        public PairList ()
        {
            super(';');
        }

        public PairList add (String label, String style)
        {
            newItem().append(label).append('=').append(style);
            return this;
        }
    }

    protected FlowPanel _text;
    protected String _queuedHTML;
    protected boolean _ready;
    protected String _panelColor;
}
