//
// $Id$

package com.threerings.msoy.admin.data;

import javax.annotation.Generated;

import com.threerings.admin.data.ConfigObject;

/**
 * Contains runtime configurable money configuration.
 */
@com.threerings.util.ActionScript(omit=true)
public class MoneyConfigObject extends ConfigObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>hourlyGameFlowRate</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String HOURLY_GAME_FLOW_RATE = "hourlyGameFlowRate";

    /** The field name of the <code>hourlyAVRGameFlowRate</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String HOURLY_AVRGAME_FLOW_RATE = "hourlyAVRGameFlowRate";

    /** The field name of the <code>hourlyMusicFlowRate</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String HOURLY_MUSIC_FLOW_RATE = "hourlyMusicFlowRate";

    /** The field name of the <code>partyGameBonusFactor</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String PARTY_GAME_BONUS_FACTOR = "partyGameBonusFactor";

    /** The field name of the <code>partyMaxBonusPopulation</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String PARTY_MAX_BONUS_POPULATION = "partyMaxBonusPopulation";

    /** The field name of the <code>payoutFactorReassessment</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String PAYOUT_FACTOR_REASSESSMENT = "payoutFactorReassessment";

    /** The field name of the <code>creatorPercentage</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String CREATOR_PERCENTAGE = "creatorPercentage";

    /** The field name of the <code>affiliatePercentage</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String AFFILIATE_PERCENTAGE = "affiliatePercentage";

    /** The field name of the <code>charityPercentage</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String CHARITY_PERCENTAGE = "charityPercentage";

    /** The field name of the <code>blingPoolSize</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String BLING_POOL_SIZE = "blingPoolSize";

    /** The field name of the <code>blingWorth</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String BLING_WORTH = "blingWorth";

    /** The field name of the <code>minimumBlingCashOut</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String MINIMUM_BLING_CASH_OUT = "minimumBlingCashOut";

    /** The field name of the <code>barPoolSize</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String BAR_POOL_SIZE = "barPoolSize";

    /** The field name of the <code>targetExchangeRate</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String TARGET_EXCHANGE_RATE = "targetExchangeRate";

    /** The field name of the <code>barCost</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String BAR_COST = "barCost";
    // AUTO-GENERATED: FIELDS END

    /** The amount of flow per hour that a game can award a player. */
    public int hourlyGameFlowRate = 3000;

    /** The amount of flow per hour that an avr game can award a player. */
    public int hourlyAVRGameFlowRate = 3000;

    /** The amount of flow (heh) per hour that the DJ minigame should award players. */
    public int hourlyMusicFlowRate = 3000;

    /** This number is raised to the power of the party size (minus 1) to calc a payout bonus. */
    public float partyGameBonusFactor = 1.015f;

    /** The maximum number of players considered for the party bonus.
     * Parties over this size are given a bonus equal to parties of this size. */
    public int partyMaxBonusPopulation = 20;

    /** The number of player minutes between reassessments of a game's payout factor. */
    public int payoutFactorReassessment = 240;

    /** The percentage of the purchase price that will be awarded to the creator of an item when it
     * is bought. */
    public float creatorPercentage = 0.3f;

    /** The percentage of the purchase price that will be awarded to the affiliate of the user who
     * bought the item. */
    public float affiliatePercentage = 0.3f;

    /** Percentage of the purchase price that will be awarded to the charity the user has chosen. */
    public float charityPercentage = 0.1f;

    /** The amount of bling (NOT centibling) to grant daily to game creators. */
    public int blingPoolSize = 403;

    /** The amount of USD cents each bling (NOT centibling) is worth. */
    public int blingWorth = 9;

    /** The minimum amount of bling (NOT centibling) that can be cashed out at a time. */
    public int minimumBlingCashOut = 500;

    /** The size of the bar pool, used to manage the exchange. */
    public int barPoolSize = 100000;

    /** The target bar/coin exchange rate. */
    public float targetExchangeRate = 10000;

    /** The amount of USD cents each bar currently costs (for display purposes). */
    public int barCost = 9;

    /**
     * The percentage of the purchase price that disappears, notionally going into the system
     * purse.
     */
    public float getSystemPercentage ()
    {
        return 1.0f - creatorPercentage - affiliatePercentage - charityPercentage;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>hourlyGameFlowRate</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setHourlyGameFlowRate (int value)
    {
        int ovalue = this.hourlyGameFlowRate;
        requestAttributeChange(
            HOURLY_GAME_FLOW_RATE, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.hourlyGameFlowRate = value;
    }

    /**
     * Requests that the <code>hourlyAVRGameFlowRate</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setHourlyAVRGameFlowRate (int value)
    {
        int ovalue = this.hourlyAVRGameFlowRate;
        requestAttributeChange(
            HOURLY_AVRGAME_FLOW_RATE, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.hourlyAVRGameFlowRate = value;
    }

    /**
     * Requests that the <code>hourlyMusicFlowRate</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setHourlyMusicFlowRate (int value)
    {
        int ovalue = this.hourlyMusicFlowRate;
        requestAttributeChange(
            HOURLY_MUSIC_FLOW_RATE, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.hourlyMusicFlowRate = value;
    }

    /**
     * Requests that the <code>partyGameBonusFactor</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setPartyGameBonusFactor (float value)
    {
        float ovalue = this.partyGameBonusFactor;
        requestAttributeChange(
            PARTY_GAME_BONUS_FACTOR, Float.valueOf(value), Float.valueOf(ovalue));
        this.partyGameBonusFactor = value;
    }

    /**
     * Requests that the <code>partyMaxBonusPopulation</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setPartyMaxBonusPopulation (int value)
    {
        int ovalue = this.partyMaxBonusPopulation;
        requestAttributeChange(
            PARTY_MAX_BONUS_POPULATION, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.partyMaxBonusPopulation = value;
    }

    /**
     * Requests that the <code>payoutFactorReassessment</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setPayoutFactorReassessment (int value)
    {
        int ovalue = this.payoutFactorReassessment;
        requestAttributeChange(
            PAYOUT_FACTOR_REASSESSMENT, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.payoutFactorReassessment = value;
    }

    /**
     * Requests that the <code>creatorPercentage</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setCreatorPercentage (float value)
    {
        float ovalue = this.creatorPercentage;
        requestAttributeChange(
            CREATOR_PERCENTAGE, Float.valueOf(value), Float.valueOf(ovalue));
        this.creatorPercentage = value;
    }

    /**
     * Requests that the <code>affiliatePercentage</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setAffiliatePercentage (float value)
    {
        float ovalue = this.affiliatePercentage;
        requestAttributeChange(
            AFFILIATE_PERCENTAGE, Float.valueOf(value), Float.valueOf(ovalue));
        this.affiliatePercentage = value;
    }

    /**
     * Requests that the <code>charityPercentage</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setCharityPercentage (float value)
    {
        float ovalue = this.charityPercentage;
        requestAttributeChange(
            CHARITY_PERCENTAGE, Float.valueOf(value), Float.valueOf(ovalue));
        this.charityPercentage = value;
    }

    /**
     * Requests that the <code>blingPoolSize</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setBlingPoolSize (int value)
    {
        int ovalue = this.blingPoolSize;
        requestAttributeChange(
            BLING_POOL_SIZE, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.blingPoolSize = value;
    }

    /**
     * Requests that the <code>blingWorth</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setBlingWorth (int value)
    {
        int ovalue = this.blingWorth;
        requestAttributeChange(
            BLING_WORTH, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.blingWorth = value;
    }

    /**
     * Requests that the <code>minimumBlingCashOut</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setMinimumBlingCashOut (int value)
    {
        int ovalue = this.minimumBlingCashOut;
        requestAttributeChange(
            MINIMUM_BLING_CASH_OUT, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.minimumBlingCashOut = value;
    }

    /**
     * Requests that the <code>barPoolSize</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setBarPoolSize (int value)
    {
        int ovalue = this.barPoolSize;
        requestAttributeChange(
            BAR_POOL_SIZE, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.barPoolSize = value;
    }

    /**
     * Requests that the <code>targetExchangeRate</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setTargetExchangeRate (float value)
    {
        float ovalue = this.targetExchangeRate;
        requestAttributeChange(
            TARGET_EXCHANGE_RATE, Float.valueOf(value), Float.valueOf(ovalue));
        this.targetExchangeRate = value;
    }

    /**
     * Requests that the <code>barCost</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setBarCost (int value)
    {
        int ovalue = this.barCost;
        requestAttributeChange(
            BAR_COST, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.barCost = value;
    }
    // AUTO-GENERATED: METHODS END
}
