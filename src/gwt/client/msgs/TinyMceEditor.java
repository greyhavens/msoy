//
// $Id$

package client.msgs;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.gwt.MessageUtil;
import com.threerings.msoy.web.gwt.CssUtil;

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
    public TinyMceEditor ()
    {
        Map<String, Object> settings = new HashMap<String, Object>();
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
            .add("backcolor")
            .add("fontselect")
            .add("fontsizeselect")
            .add("formatselect")
            .add("code")
            .build());
        settings.put("theme_advanced_buttons3", "");
        settings.put("theme_advanced_blockformats", new CommaList()
            .add("p")
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
            .add("Normal", "")
            .add("Times New Roman", "times")
            .add("Arial", "arial")
            .add("Monospace", "monospace")
            .add("Georgia", "georgia")
            .add("Trebuchet", "trebuchet")
            .add("Verdana", "verdana")
            .build());
        settings.put("content_css", CssUtil.GLOBAL_PATH);

        initializeTinyMCE(JavaScriptUtil.createDictionaryFromMap(settings));

        add(_text = new FlowPanel());
        _text.setWidth("100%");
        _text.setHeight("300px");
        _text.getElement().setId("msoy_tinymce");
    }

    @Override // from MessageEditor
    public String getHTML ()
    {
        return getHTML(getId());
    }

    @Override // from MessageEditor
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

    @Override // from MessageEditor
    public void setFocus (boolean focus)
    {
        // TODO
    }

    @Override // from MessageEditor
    public Widget asWidget ()
    {
        return this;
    }

    @Override // from MessageEditor
    public void selectAll ()
    {
        // TODO
    }

    @Override // from MessageEditor
    public Button getToggler ()
    {
        return null;
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
            // NB "window.tinymce" is the package, "window.tinyMCE" is the singleton
            var ed = new $wnd.tinymce.Editor(itemId, $wnd.tinyMCE.settings);
            ed.onInit.add(function(ed) {
                t.@client.msgs.TinyMceEditor::editorReady()();
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
}
