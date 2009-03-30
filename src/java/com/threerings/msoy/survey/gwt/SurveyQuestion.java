//
// $Id$

package com.threerings.msoy.survey.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.samskivert.depot.ByteEnum;

/**
 * A single question on a survey.
 */
public class SurveyQuestion
    implements IsSerializable
{
    /** Types of questions we support. Do not reorder or remove items. */
    public enum Type
        implements ByteEnum, IsSerializable {

        /** Answer is true or false. */
        BOOLEAN,

        /** Answer is one of several options presented. */
        EXCLUSIVE_CHOICE,

        /** Answer specifies a subset of the options presented. */
        SUBSET_CHOICE,

        /** Answer is a number on a scale, for example, 1 to 5. */
        RATING,

        /** Answer is text. */
        FREE_FORM;

        /**
         * Translate a byte back into the Type instance- required by ByteEnum.
         */
        public static Type fromByte (byte value)
        {
            return values()[value];
        }

        // from ByteEnum
        public byte toByte ()
        {
            return (byte)ordinal();
        }
    }

    /** The type of this question. */
    public Type type;

    /** The question. */
    public String text;

    /** The choices, applies to SUBSET_CHOICE and EXCLUSIVE_CHOICE. */
    public String choices[];

    /** The maximum value, applies to RATING (min value is always 1). */
    public int maxValue;

    /** If this question may be omitted. */
    public boolean optional;

    /**
     * Returns the possible answers to a question suitable for use in the database. This is used by
     * the server and client to generate, aggregate and display responses. With the exception of
     * {@link Type#FREE_FORM} questions and questions whose choices or range values have changed,
     * all values in {@link SurveyResponse#response} should be contained in this array for their
     * corresponding questions.
     */
    public String[] getEncodedChoices()
    {
        switch (type) {
        case BOOLEAN:
            return _boolean;
        case EXCLUSIVE_CHOICE:
        case SUBSET_CHOICE:
            return getIntAnswers(0, choices.length - 1);
        case RATING:
            return getIntAnswers(1, maxValue);
        case FREE_FORM:
            return new String[0];
        default:
            throw new RuntimeException("Unimplemented question type " + type);
        }
    }

    /**
     * Generates all values from min to max, inclusive, as strings.
     */
    protected static String[] getIntAnswers(int min, int max)
    {
        if (max < min) {
            return new String[0];
        }
        String[] answers = new String[max - min + 1];
        for (int ii = 0; ii < answers.length; ++ii) {
            answers[ii] = String.valueOf(min + ii);
        }
        return answers;
    }

    /** Answers for boolean types. */
    protected static final String[] _boolean = new String[] {"false", "true"};
}
