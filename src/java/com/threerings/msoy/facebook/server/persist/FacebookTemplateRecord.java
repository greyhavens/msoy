//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import com.google.common.collect.Maps;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.facebook.gwt.FacebookTemplate;
import com.threerings.msoy.facebook.gwt.FacebookService.Gender;

/**
 * Describes a story template entered into the Facebook template editor for use at runtime by the
 * msoy server.
 */
@Entity
public class FacebookTemplateRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FacebookTemplateRecord> _R = FacebookTemplateRecord.class;
    public static final ColumnExp<Integer> APP_ID = colexp(_R, "appId");
    public static final ColumnExp<String> CODE = colexp(_R, "code");
    public static final ColumnExp<String> VARIANT = colexp(_R, "variant");
    public static final ColumnExp<Boolean> ENABLED = colexp(_R, "enabled");
    public static final ColumnExp<Long> BUNDLE_ID = colexp(_R, "bundleId");
    public static final ColumnExp<String> CAPTION_MALE = colexp(_R, "captionMale");
    public static final ColumnExp<String> CAPTION_FEMALE = colexp(_R, "captionFemale");
    public static final ColumnExp<String> CAPTION_NEUTRAL = colexp(_R, "captionNeutral");
    public static final ColumnExp<String> DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp<String> PROMPT = colexp(_R, "prompt");
    public static final ColumnExp<String> LINK_TEXT = colexp(_R, "linkText");
    // AUTO-GENERATED: FIELDS END

    /** Determines compatible schema versions. */
    public static final int SCHEMA_VERSION = 5;

    /** The id of the application defining this template. */
    @Id public int appId;

    /** Used by msoy to reference the functionality of this template. */
    @Id public String code;

    /** Distinguish between functionally equivalent templates. */
    @Id public String variant;

    /** Whether this template is considered when a template is requested. */
    public boolean enabled;

    /** Passed to Facebook when creating a new story. */
    public long bundleId;

    /** The caption for passing to publishStream for a user with a penis. */
    @Column(nullable=true)
    public String captionMale;

    /** The caption for passing to publishStream for a user with a vagina. */
    @Column(nullable=true)
    public String captionFemale;

    /** The caption for passing to publishStream for a user with undisclosed genitalia. */
    @Column(nullable=true)
    public String captionNeutral;

    /** The description for passing to publishStream */
    public String description;

    /** The prompt for passing to publishStream */
    public String prompt;

    /** The text of the link to pass to publishStream */
    public String linkText;

    /**
     * Creates a new template to be filled in with data from the database.
     */
    public FacebookTemplateRecord ()
    {
    }

    /**
     * Creates a new template matching the given runtime template.
     */
    public FacebookTemplateRecord (int appId, FacebookTemplate template)
    {
        this.appId = appId;
        code = template.key.code;
        bundleId = template.bundleId;
        variant = template.key.variant;
        captionMale = template.captions.get(Gender.MALE);
        captionFemale = template.captions.get(Gender.FEMALE);
        captionNeutral = template.captions.get(Gender.NEUTRAL);
        description = template.description;
        prompt = template.prompt;
        linkText = template.linkText;
        enabled = template.enabled;
    }

    /**
     * Creates and returns a runtime template matching this one, including all genders in the
     * {@link FacebookTemplate#captions} member.
     */
    public FacebookTemplate toTemplate ()
    {
        FacebookTemplate templ = toTemplateBase();
        templ.captions = Maps.newHashMap();
        templ.captions.put(Gender.MALE, StringUtil.deNull(captionMale));
        templ.captions.put(Gender.FEMALE, StringUtil.deNull(captionFemale));
        templ.captions.put(Gender.NEUTRAL, StringUtil.deNull(captionNeutral));
        return templ;
    }

    /**
     * Creates and returns a runtime template matching this one, including the appropiate gender-
     * specific value in {@link FacebookTemplate#caption}.
     */
    public FacebookTemplate toTemplate (Gender gender)
    {
        FacebookTemplate templ = toTemplateBase();
        switch (gender) {
        case MALE:
            templ.caption = StringUtil.getOr(captionMale, captionNeutral);
            break;
        case FEMALE:
            templ.caption = StringUtil.getOr(captionFemale, captionNeutral);
            break;
        case NEUTRAL:
            templ.caption = captionNeutral;
            break;
        }
        return templ;
    }

    /**
     * Get the entry vector associated with this template. This is so we can track the popularity
     * of variants.
     */
    public String toEntryVector ()
    {
        return FacebookTemplate.toEntryVector(code, variant);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FacebookTemplateRecord}
     * with the supplied key values.
     */
    public static Key<FacebookTemplateRecord> getKey (int appId, String code, String variant)
    {
        return newKey(_R, appId, code, variant);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(APP_ID, CODE, VARIANT); }
    // AUTO-GENERATED: METHODS END

    protected FacebookTemplate toTemplateBase ()
    {
        FacebookTemplate templ = new FacebookTemplate(code, variant, bundleId);
        templ.description = description;
        templ.prompt = prompt;
        templ.linkText = linkText;
        templ.enabled = enabled;
        return templ;
    }
}
