////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.resources
{

import mx.managers.ISystemManager;

/**
 *  The Locale class can be used to parse a locale string such as "en_US_MAC"
 *  into its three parts: a language code, a country code, and a variant.
 *
 *  <p>The localization APIs in the IResourceManager and IResourceBundle
 *  interfaces use locale strings rather than Locale instances,
 *  so this class is seldom used in an application.</p>
 *
 *  @see mx.resources.IResourceBundle
 *  @see mx.resources.IResourceManager
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

    [Deprecated(replacement="ResourceManager.localeChain", since="3.0")]
    
    /**
     *  If you compiled your application for a single locale,
     *  this method returns a Locale object representing that locale.
     *  Otherwise, it returns null.
     *  
     *  <p>This method has been deprecated because the Flex framework
     *  now supports having resource bundles for multiple locales
     *  in the same application.
     *  You can use the <code>getLocale()</code> method of IResourceManager
     *  to find out which locales the ResourceManager has resource bundles for.
     *  You can use the <code>localeChain</code> property of IResourceManager
     *  to determine which locales the ResourceManager searches for
     *  resources.</p>
     */
    public static function getCurrent(sm:ISystemManager):Locale
    {
        if (!currentLocale)
        {
            var compiledLocales:Array = sm.info()["compiledLocales"];
            if (compiledLocales != null && compiledLocales.length == 1)
                currentLocale = new Locale(compiledLocales[0]);
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
     *
     *  @param localeString A 1-, 2-, or 3-part locale String,
     *  such as "en", "en_US", or "en_US_MAC".
     *  The parts are separated by an underscore.
     *  The first part should be a 2-letter lowercase language code
     *  as defined by ISO-639, such as "en" for English.
     *  The second part should be a 2-letter uppercase country code
     *  as defined by ISO-3166, such as "US" for the United States.
     *  The third part is a variant string which can be used as you wish
     *  to distinguish multiple locales for the same language and country.
     *  It is sometimes used to indicate the operating system
     *  that the locale should be used with,
     *  such as "MAC", "WIN", or "UNIX".
     */
    public function Locale(localeString:String)
    {
        super();

        this.localeString = localeString;
        
        var parts:Array = localeString.split("_");
        
        if (parts.length > 0)
            _language = parts[0];
        
        if (parts.length > 1)
            _country = parts[1];
        
        if (parts.length > 2)
            _variant = parts.slice(2).join("_");
    }
    
    //--------------------------------------------------------------------------
    //
    //  Variables
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    private var localeString:String;

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
     *  The language code of this Locale instance. [Read-Only]
     *
     *  @example
     *  <pre>
     *  var locale:Locale = new Locale("en_US_MAC");
     *  trace(locale.language); // outputs "en"
     *  </pre>
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
     *  The country code of this Locale instance. [Read-Only]
     *
     *  @example
     *  <pre>
     *  var locale:Locale = new Locale("en_US_MAC");
     *  trace(locale.country); // outputs "US"
     *  </pre>
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
     *  The variant part of this Locale instance. [Read-Only]
     *
     *  @example
     *  <pre>
     *  var locale:Locale = new Locale("en_US_MAC");
     *  trace(locale.variant); // outputs "MAC"
     *  </pre>
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
     *  Returns the locale String that was used to construct
     *  this Locale instance.
     *
     *  @example
     *  <pre>
     *  var locale:Locale = new Locale("en_US_MAC");
     *  trace(locale.toString()); // outputs "en_US_MAC"
     *  </pre>
     */   
    public function toString():String
    {
        return localeString;
    }
}

}
