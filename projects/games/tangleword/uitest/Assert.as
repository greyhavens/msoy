package
{
    /**
       Experimental implementation of asserts.
       
       Since ActionScript doesn't have built-in asserts, or even
       a preprocessor, we can try to fake it using anonymous functions plus a logger.
    */

    public class Assert
    {
        public static function True (booleanFn : Function, message : String) : void
        {
            var result : Boolean = booleanFn();
            if (! result)
            {
                Log.getLog("Assert").warning (message);
            }
        }
    }
}



