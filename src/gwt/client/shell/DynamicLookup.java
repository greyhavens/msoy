//
// $Id$

package client.shell;

import com.threerings.gwt.util.MessagesLookup;
import com.threerings.msoy.item.data.all.MsoyItemType;

/**
 * Provides dynamic translation message lookup for things that need it.
 */
@MessagesLookup.Lookup(using="client.shell.DynamicMessages")
public abstract class DynamicLookup extends MessagesLookup
{
    public String xlateItemType (MsoyItemType type)
    {
        return xlate("itemType" + type.toByte());
    }

    public String xlateItemsType (MsoyItemType type)
    {
        return xlate("pItemType" + type.toByte());
    }

    public String xlateCatalogIntro (MsoyItemType type)
    {
        return xlate("catIntro" + type.toByte());
    }

    public String xlateGetStuffBuy (MsoyItemType type)
    {
        return xlate("getStuffBuy" + type.toByte());
    }

    public String xlateGetStuffCreate (MsoyItemType type)
    {
        return xlate("getStuffCreate" + type.toByte());
    }

    public String xlateEditorWikiLink (MsoyItemType type)
    {
        return xlate("editorWikiLink" + type.toByte());
    }
    
}
