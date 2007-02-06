package
{
    /**
       Since ActionScript doesn't have built-in asserts, or even
       a preprocessor, we can try to fake it using this wrapper around Log.
    */

    public class Assert
    {
        /**
           If the /value/ is true, does nothing;
           otherwise sends the /message/ to system log.
        */
        public static function True (value : Boolean, message : String) : void
        {
            if (! value)
            {
                Log (message);
            }
        }

        /**
           If the /object/ is not null, does nothing;
           otherwise sends /message/ to system log.
        */
        public static function NotNull (obj : Object, message : String) : void
        {
            True (obj != null, message);
        }

        /**
           Always fails and sends the /message/ to system log.
        */
        public static function Fail (message : String) : void
        {
            log (message);
        }


        // PRIVATE FUNCTIONALITY

        /** Logging function */
        private static function log (line : String) : void
        {
            if (_log == null)
            {
                _log = Log.getLog("ASSERT");
            }

            _log.warning (line);
        }

        /** Log reference */
        private static var _log : Log = null;
    }
}



