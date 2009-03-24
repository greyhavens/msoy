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
}
