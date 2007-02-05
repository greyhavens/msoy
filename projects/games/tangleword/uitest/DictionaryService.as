package
{
    /** Temporary stub for a better dictionary service that will run
        on the server. For testing, I'm just baking this data into the SWF. */

    public class DictionaryService
    {
        /** Creates an array of /count/ strings. Language/culture settings are
            ignored in this test version. */
        public static function getLetterSet (language_culture : int, count : int) : Array
        {
            var set : Array = new Array (count);
            for (var i : int = 0; i < count; i++)
            {
                var string : String = String.fromCharCode (randomChar (language_culture));
                set[i] = string;
            }

            return set;
        }

        /** Checks if a word is in dictionary */
        public static function checkWord (language_culture : int, word : String) : Boolean
        {
            return (Math.random() > 0.5); // testing only
        }
        

        private static function randomChar (language_culture : int) : int
        {
          var minChar : int = 0x41;  // inclusive min
          var maxChar : int = 0x5a;  // inclusive max
            var charDelta : int = maxChar - minChar + 1;
            var randomInRange : int = int(Math.floor(Math.random() * charDelta));
            var result : int = minChar + randomInRange;
            return result;
        }

        
            
    }
    
} // package
