package com.shopmart.module.customer.dto;

/**
 * Segment thresholds are configuration (app.customer.segments.* in application.yml), not
 * hardcoded guesses — tune them per your actual customer base. Defaults documented below
 * are a starting point, not a business decision made on your behalf.
 *
 * Priority when a customer matches more than one rule (checked top to bottom, first match wins):
 *   VIP > HIGH_VALUE > RETURNING > NEW > INACTIVE > STANDARD
 */
public enum CustomerSegment {
    /** Lifetime paid total >= app.customer.segments.vip-min-spend (default ₹50,000). */
    VIP,
    /** Lifetime paid total >= app.customer.segments.high-value-min-spend (default ₹20,000). */
    HIGH_VALUE,
    /** More than one paid order (reuses the existing repeat-customer definition already used in Analytics). */
    RETURNING,
    /** Registered within app.customer.segments.new-customer-days (default 30 days), no paid orders yet. */
    NEW,
    /** Account older than the "new" window, with no order and no login activity in
     *  app.customer.segments.inactive-days (default 90 days). */
    INACTIVE,
    /** Doesn't match any of the above (e.g. registered a while ago, ordered once, still active). */
    STANDARD
}
