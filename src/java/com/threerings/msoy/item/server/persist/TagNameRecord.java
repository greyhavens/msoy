//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.threerings.io.Streamable;

/**
 * Maps a tag's id to the tag itself.
 */
@Entity
@Table
public class TagNameRecord
    implements Streamable
{
    public static final int SCHEMA_VERSION = 1;

    public static final String TAG_ID = "tagId";
    public static final String TAG = "tag";

    /** The ID of this tag. */
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    public int tagId;

    /** The actual tag. */
    @Column(nullable=false)
    public String tag;
}
