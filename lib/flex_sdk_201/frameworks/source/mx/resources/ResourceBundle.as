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

import flash.utils.describeType;
import flash.system.ApplicationDomain;
import mx.core.mx_internal;
import mx.managers.ISystemManager;
import mx.utils.StringUtil;

use namespace mx_internal;

/**
 *  
 *  This class can be used to internationalize 
 *  the framework and applications.
 *  To localize an application, you can create one 
 *  properties file per class or per package or simply 
 *  a single properties file. This properties file 
 *  is exactly like a Java-based properties file 
 *  which has name-value pairs.
 *  The properties file name can be the same as 
 *  the class name, for example, MyAlert.properties corresponding 
 *  to MyAlert.as, or it can have a different name.
 *  Exisiting framework properties files can be found in 
 *  the frameworks/locale/en_US/ directory. The 
 *  frameworks/locale/en_US/SharedResources.properties file
 *  contains all shared keys and error strings across
 *  the framework.
 * <p>
 *  If you want to localize in a particular language, for example French, you would
 *  make a copy of frameworks/locale/en_US/ directory and 
 *  call it fr_FR/ and make the necessary changes to the
 *  framework properties files.
 *  You can compile your application kept in MyApp using
 *  <code>"..\bin\mxmlc.exe" -locale en_US -library-path=..\frameworks\libs
 *  -source-path="..\frameworks\locale\{locale},
 *  ..\myLocale\{locale}" main.mxml</code>
 *  </p>
 *  <p>Note that the source-path mentions the source
 *  for both frmaework properties files as well as 
 *  myApp's properties files. In this case, you would 
 *  place MyAlert.properties in the myLocale/en_US and 
 *  myLocale/fr_FR directories. The locale directory should be 
 *  part of the compc ActionScript classpath
 *  but should not be a part of the ActionScript
 *  classpath that is used for mxmlc when building 
 *  the main SWF file; for example, placing myLocale in the
 *  myApp directory would throw a compiler warning.
 *  The locale is settable through the xml config file,
 *  mxml compiler argument or through Flex builder.  However,
 *  locale is not settable through the Locale class at runtime.
 *  </p> 
 *  <p>The framework currently supports en_US and ja_JP only. However,
 *  you can create properties files for any language and keep
 *  them under a locale folder, such as fr_FR for French.
 *  </p>
 *  <p>While getting localizable text, you must 
 *  specify the filename in the ResourceBundle metadata; for example:</p> 
 *
 * <pre>
 * [ResourceBundle('MyAlert')] 
 * private static var rb:ResourceBundle;</pre>
 *
 * <p>
 * and then access it in ActionScript using <code>rb.getString('foo');</code>
 * or in MXML using <code>&#64;Resource(bundle='MyAlert', key='foo')</code>.
 * </p>
 *  @see mx.resources.Locale
 *
 *  @helpid 
 *  @tiptext 
 */
public class ResourceBundle
{
    include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class methods
	//
	//--------------------------------------------------------------------------

    /**
     * Gets a ResourceBundle when provided with a className.
     * @example
     * <pre>
     * private static var rb:ResourceBundle;
     * rb = getResourceBundle("fooBundle");
     * </pre>
     */
    public static function getResourceBundle(
								baseName:String,
								currentDomain:ApplicationDomain = null):
								ResourceBundle
    {
        if (currentDomain == null)
            currentDomain = ApplicationDomain.currentDomain;

    	var bundleClass:Class =
			Class(getDefinitionByName(baseName + "_properties", currentDomain));

        if (!bundleClass)
            bundleClass = Class(getDefinitionByName(String(baseName), currentDomain));

        if (bundleClass)
        {
            var bundleObj:Object = new bundleClass();
            if (bundleObj is ResourceBundle)
            {
                var bundle:ResourceBundle = ResourceBundle(bundleObj);
                bundle.initialize(String(baseName));
                return bundle;
            }
        }

        throw new Error("Could not find resource bundle " + baseName);
    }

    /**
     *  @private
     */
	private static function getDefinitionByName(name:String, domain:ApplicationDomain):Object
	{
        var definition:Object;

        if (domain.hasDefinition(name))
		{
			definition = domain.getDefinition(name);
		}

		return definition;
	}

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

    /**
	 *  Constructor.
	 */
    public function ResourceBundle()
    {
		super();
    }  

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
     */	
    private var content:Object;
	
    /**
	 *  @private
     */	    
	private var bundleName:String;

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
     */	   
    private function initialize(name:String):void
    {
        bundleName = name;
        
		content = getContent();
		if (!content)
        {
            throw new Error("No content found in resource bundle " +
							bundleName);
        }
    }

    /**
     * You can override this method to provide
     * your own keys and values for a ResourceBundle or
     * to add objects to your custom ResourceBundle.
     * The following example adds a jpg image to a
     * custom ResourceBundle.
     * First you create a class called myPic. You then make 
     * a custom ResourceBundle called myBundle.  
     * @example
     * <pre>
     * package {
     * [Embed(source='picture.jpg')]
     * public class myPic extends mx.core.SpriteAsset {}
     * }
     * package {
     * import mx.resources.ResourceBundle;
     * public class myBundle extends ResourceBundle {
     * public function myBundle() { super(); }
     * override protected static function getContent():Object {
     * var contentObj:Object = new Object(); 
     * content.push("myPic", myPic);
     * return contentObj; }}}
     * </pre>
     * @see #getObject()
     */
    protected function getContent():Object
    {
        return null;
    }

    /**
     * Gets a Boolean from a ResourceBundle.
     * @example
     * <pre>
     * [ResourceBundle("foo")]
     * private static var packageResources:ResourceBundle;
     * packageResources.getString("myBooleanKey");
     * </pre>
     */    
    public function getBoolean(key:String, defaultValue:Boolean = true):Boolean
    {
        var temp:String = getObject(key).toLowerCase();
		
		if (temp == "false")
			return false;
		else if (temp == "true")
			return true;
		else
			return defaultValue;
    }

    /**
     *  Gets a Number from a ResourceBundle.
     *  @example
     *  <pre>
     *  [ResourceBundle("foo")]
     *  private static var packageResources:ResourceBundle;
     *  packageResources.getString("myNumericKey");
     *  </pre>
     */        
    public function getNumber(key:String):Number
    {
    	return Number(getObject(key));
    }

    /**
	 *  Gets a string from a ResourceBundle.
	 *  @example
         * <pre>
         * [ResourceBundle("controls")]
	 * private static var packageResources:ResourceBundle;
	 * packageResources.getString("okLabel");
	 * </pre>
	 */        
    public function getString(key:String):String
    {
    	return String(getObject(key));
    }

    /**
     *  Gets an array from a ResourceBundle.
     *  @example
     * <pre>
     * [ResourceBundle("SharedResources")]
     * private static var sharedResources:ResourceBundle;
     * sharedResources.getStringArray("monthNames");
     * </pre>
     */    
    public function getStringArray(key:String):Array
    {
		var array:Array = getObject(key).split(",");
		
		var n:int = array.length;
		for (var i:int = 0; i < n; i++)
		{
			 array[i] = StringUtil.trim(array[i]);
		}  
    	
		return array;
    }

    /**
	 *  Gets an object from a ResourceBundle.
	 *  The folowing example  shows how to get a
	 *  jpg image with the <code>getObject()</code> method.
	 *  @example
         *  <pre>
	 *  [ResourceBundle("MyBundle")]
	 *  private static var rb:ResourceBundle;
	 *  var sprite:SpriteAsset = SpriteAsset(rb.getObject("myPic"));
	 *  </pre>
	 *  @see #getContent()
	 */        
    public function getObject(key:String):Object
    {
        var value:Object = content[key];
		if (!value)
        {
            throw new Error("Key " + key +
							" was not found in resource bundle " + bundleName);
        }
		return value;
    }
}

}
