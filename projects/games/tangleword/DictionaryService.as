package
{
    /**
       Temporary stub for an online dictionary service that will run
       on the server. For testing, I'm just baking this data into the SWF. */

    public class DictionaryService
    {
        /**
           Creates an array of /count/ random letters or multi-letter elements
           (as appropriate for the specific /locale/). Each element in the array
           is a string.

           In this stub version, locale settings are ignored.
        */
        public static function getLetterSet (locale : LocaleSettings,
                                             count : int) : Array
        {
            var set : Array = new Array (count);
            for (var i : int = 0; i < count; i++)
            {
                var string : String = String.fromCharCode (randomChar ());
                set[i] = string;
            }

            return set;
        }

        /**
           Returns true if the /word/ is in the dictionary for the specific /locale/.
        */
        public static function checkWord (locale : LocaleSettings, word : String) : Boolean
        {
            return true; // testing only
        }
        

        // Helper function for the stub
        private static function randomChar () : int
        {
          var minChar : int = 0x41;  // inclusive min
          var maxChar : int = 0x5a;  // inclusive max
            var charDelta : int = maxChar - minChar + 1;
            var randomInRange : int = int (Math.floor (Math.random() * charDelta));
            var result : int = minChar + randomInRange;
            return result;
        }

        
            
    }
    
} // package
