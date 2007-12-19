////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.binding
{

[ExcludeClass]

/**
 *  @private
 */
public class BindingManager
{
    include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class methods
	//
	//--------------------------------------------------------------------------

    /**
     *  Store a Binding for the destination relative to the passed in document.
	 *  We don't hold a list of bindings per destination
	 *  even though it is possible to have multiple.
     *  The reason is that when we refresh the last binding will
	 *  always win anyway, so why execute the ones that will lose.
     *
     *  @param document The document that this binding relates to.
	 *
     *  @param destStr The destination field of this binding.
	 *
     *  @param b The binding itself.
     */
    public static function addBinding(document:Object, destStr:String,
									  b:Binding):void
    {
        if (!document._bindingsByDestination)
        {
            document._bindingsByDestination = {};
            document._bindingsBeginWithWord = {};
        }

        document._bindingsByDestination[destStr] = b;
        document._bindingsBeginWithWord[getFirstWord(destStr)] = true;
    }

    /**
     *  Execute all bindings that bind into the specified object.
     *
     *  @param document The document that this binding relates to.
	 *
     *  @param destStr The destination field that needs to be refreshed.
	 *
     *  @param destObj The actual destination object
	 *  (used for RepeatableBinding).
     */
    public static function executeBindings(document:Object,
                                           destStr:String,
										   destObj:Object):void
    {
        // Bail if this method is accidentally called with an empty or
        // null destination string. Otherwise, all bindings will
        // be executed!
        if (!destStr || (destStr == ""))
            return;

        if (document &&
            document._bindingsByDestination &&
            document._bindingsBeginWithWord &&
            document._bindingsBeginWithWord[getFirstWord(destStr)])
        {
            for (var binding:String in document._bindingsByDestination)
            {
                // If we have been told to execute bindings into a UIComponent
                // or Repeater with id "a", we want to execute all bindings
                // whose destination strings look like "a", "a.b", "a.b.c",
                // "a['b']", etc. but not "aa.bb", "b.a", "b.a.c", "b['a']",
                // etc.

                // Currently, the only way that this method is used by the
                // framework is when the destStr passed in is the id of a
                // UIComponent or Repeater.

                // However, advanced users can call
                // BindingManager.executeBindings() and pass a compound
                // destStr like "a.b.c". This has a gotcha: If they have
                // written <mx:Binding> tags with destination attributes
                // like "a['b'].c.d" rather than "a.b.c.d", these will
                // not get executed. They should pass the same form of
                // destStr to executeBindings() as they used in their
                // Binding tags.

                if (binding.charAt(0) == destStr.charAt(0))
                {
                    if (binding.indexOf(destStr + ".") == 0 ||
                        binding.indexOf(destStr + "[") == 0 ||
                        binding == destStr)
                    {
                        // If this is a RepeatableBindings, execute it on just the
                        // specified object, not on all its repeated siblings.  For
                        // example, if we are instantiating o[2][3], we don't want to also
                        // refresh o[0][0], o[0][1], etc.
                        document._bindingsByDestination[binding].execute(destObj);
                    }
                }
            }
        }
    }

    public static function getUIComponentWatcherForDestination(
										document:Object, destStr:String):int
    {
        if (document._bindingsByDestination)
        {
            for (var binding:String in document._bindingsByDestination)
            {
                if (binding == destStr &&
					document._bindingsByDestination[binding].uiComponentWatcher != -1)
                {
                    return document._bindingsByDestination[binding].uiComponentWatcher;
                }
            }
        }
        return -1;
    }

    /**
	 *  @private
	 */
	private static function getFirstWord(destStr:String):String
    {
        // indexPeriod and indexBracket will be equal only if they
        // both are -1.
        var indexPeriod:int = destStr.indexOf(".");
        var indexBracket:int = destStr.indexOf("[");
        if (indexPeriod == indexBracket)
            return destStr;

        // Get the characters leading up to the first period or
        // bracket.
        var minIndex:int = Math.min(indexPeriod, indexBracket);
        if (minIndex == -1)
            minIndex = Math.max(indexPeriod, indexBracket);

        return destStr.substr(0, minIndex);
    }

    /**
     *  @private
     */
    internal static var debugDestinationStrings:Object = {};

    /**
     *  Enables debugging output for the Binding or Bindings with a matching
     *  destination string.
     *
     *  @param destinationString The Binding's destination string.
     */
    public static function debugBinding(destinationString:String):void
    {
        debugDestinationStrings[destinationString] = true;
    }

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 *  BindingManager has only static methods.
	 *  We don't create instances of BindingManager.
	 */
	public function BindingManager()
	{
		super();
	}
}

}
