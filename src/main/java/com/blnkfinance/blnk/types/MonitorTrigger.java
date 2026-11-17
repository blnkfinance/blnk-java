package com.blnkfinance.blnk.types;

import java.util.List;

/**
 * Values for a balance monitor's {@code trigger} (Core 0.15.7+). Leave the
 * trigger unset to get Core's default, {@link #EDGE}.
 */
public final class MonitorTrigger {

  private MonitorTrigger() {}

  /**
   * Fires once when the condition becomes true, and again only after it has
   * gone false and become true again. Core's default.
   */
  public static final String EDGE = "edge";

  /** Fires on every balance update while the condition holds. */
  public static final String LEVEL = "level";

  /** Every value Core accepts. Core compares them case-sensitively. */
  public static final List<String> VALUES = List.of(EDGE, LEVEL);
}
