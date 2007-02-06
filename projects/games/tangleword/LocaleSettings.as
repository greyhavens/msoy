package
{
    /**
       This enumeration enumerates supported language/culture settings.
    */
    public final class LocaleSettings
    {
        // CONSTANTS
        
        /** English language, US culture settings */
        public static const EN_US : LocaleSettings = new LocaleSettings ("en-US");

        /** Spanish language, Spain culture settings */
        public static const ES_SP : LocaleSettings = new LocaleSettings ("es-SP");


        // INSTANCE FUNCTIONALITY

        /**
           Constructor, accepts a string of the form "xx-yy", where xx
           is the ISO language code, and yy is the ISO country code.
        */
        public function LocaleSettings (locale : String)
        {
            Locale = locale;
        }

        /**
           Public locale description string (only useful for debugging).
        */
        public var Locale : String; 
        
    }
}
