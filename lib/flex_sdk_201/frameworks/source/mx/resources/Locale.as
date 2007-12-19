////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.resources
{

import mx.managers.ISystemManager;

/**
 *  You can use this class to query the current Locale.
 *  The locale is settlable through the XML configuration file,an
 *  mxmlc compiler argument or through Flex builder.  However,
 *  locale is not settable through the Locale class at run time.
 *  The framework currently supports en_US and ja_JP only. However,
 *  you can create properties files for any language and keep
 *  them under a locale folder, such as fr_FR for French.
 *  eg: <code>Locale.getCurrent(Application.application.systemManager).country</code> 
 *  returns the current country.
 *
 *  @see mx.resources.ResourceBundle
 *
 *  @helpid 
 *  @tiptext 
 */
public class Locale
{
    include "../core/Version.as";

    //--------------------------------------------------------------------------
    //
    //  Class variables
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    private static var currentLocale:Locale;

	//--------------------------------------------------------------------------
	//
	//  Class methods
	//
	//--------------------------------------------------------------------------

    /**
     *  Gets the current locale.
     *  @example
     *  <pre>
     *  Locale.getCurrent(Application.application.systemManager).country
     *  </pre>
     */
    public static function getCurrent(sm:ISystemManager):Locale
    {
        if (!currentLocale)
        {
            var cls:Class =
				Class(sm.getDefinitionByName("mx.generated.GeneratedLocale"));
            if (cls)
                currentLocale = new cls();
        }

        return currentLocale;
    }

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

    /**
	 *  Constructor.
	 */
    public function Locale(localeStr:String)
    {
        super();

        this.localeStr = localeStr;
        
		var parts:Array = localeStr.split("_");
        
		if (parts.length > 0)
            _language = parts[0];
        
		if (parts.length > 1)
            _country = parts[1];
        
		if (parts.length > 2)
            _variant = parts[2];
    }
	
	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
	 */
    private var localeStr:String;

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  language
	//----------------------------------
			
    /**
	 *  @private
	 *  Storage for the language property.
	 */
	private var _language:String;

    [Inspectable(category="General", defaultValue="null")]
	
    /**
	 *  The language of current locale [Read-Only].
	 *  @example
     *  <pre>
     *  Locale.getCurrent(Application.application.systemManager).language
     *  </pre>
	 *  
	 *  @default "en"
	 */	     
    public function get language():String
    {
		return _language;
    }
       
	//----------------------------------
	//  country
	//----------------------------------
			
    /**
	 *  @private
	 *  Storage for the country property.
	 */
	private var _country:String;

    [Inspectable(category="General", defaultValue="null")]

    /**
	 *  The country of current locale [Read-Only].
	 *  @example
     *  <pre>
     *  Locale.getCurrent(Application.application.systemManager).country
     *  </pre>
	 *
	 *  @default "EN"
	 */	     
    public function get country():String
    {
		return _country
    }

	//----------------------------------
	//  variant
	//----------------------------------
	    		
    /**
	 *  @private
	 *  Storage for the variant property.
	 */
	private var _variant:String;

    [Inspectable(category="General", defaultValue="null")]
	
    /**
	 *  The variant of current locale [Read-Only].
	 *  @example
     *  <pre>
     *  Locale.getCurrent(Application.application.systemManager).variant
     *  </pre>
	 *  
	 *  @default ""
	 */	     
    public function get variant():String
    {
		return _variant;
    }
    
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

    /**
     *  Returns locale in the form language_country_variant 
     *  if variant is present else language_country
     *  @example
     *  <pre>
     *  Locale.getCurrent(Application.application.systemManager).toString()
     *  </pre>
     */	  
    public function toString():String
    {
    	return localeStr;
    }
}

}
