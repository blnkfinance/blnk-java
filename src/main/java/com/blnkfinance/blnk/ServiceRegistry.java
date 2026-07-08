package com.blnkfinance.blnk;

import com.blnkfinance.blnk.endpoints.ApiKeys;
import com.blnkfinance.blnk.endpoints.BalanceMonitor;
import com.blnkfinance.blnk.endpoints.BlnkSystem;
import com.blnkfinance.blnk.endpoints.Hooks;
import com.blnkfinance.blnk.endpoints.Identity;
import com.blnkfinance.blnk.endpoints.LedgerBalances;
import com.blnkfinance.blnk.endpoints.Ledgers;
import com.blnkfinance.blnk.endpoints.Metadata;
import com.blnkfinance.blnk.endpoints.Reconciliation;
import com.blnkfinance.blnk.endpoints.Search;
import com.blnkfinance.blnk.endpoints.Transactions;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The default service map wired by {@link Blnk#init}. The registration names
 * are load-bearing — {@code getService} resolves by these exact strings.
 * Note: the "System" service maps to {@link BlnkSystem}; the class carries a
 * prefix to avoid clashing with {@code java.lang.System}.
 */
public final class ServiceRegistry {

  private ServiceRegistry() {}

  public static Map<String, ServiceFactory> defaultServices() {
    Map<String, ServiceFactory> services = new LinkedHashMap<>();
    services.put("Ledgers", Ledgers::new);
    services.put("LedgerBalances", LedgerBalances::new);
    services.put("Transactions", Transactions::new);
    services.put("BalanceMonitor", BalanceMonitor::new);
    services.put("Reconciliation", Reconciliation::new);
    services.put("Search", Search::new);
    services.put("Identity", Identity::new);
    services.put("System", BlnkSystem::new);
    services.put("Metadata", Metadata::new);
    services.put("Hooks", Hooks::new);
    services.put("ApiKeys", ApiKeys::new);
    return services;
  }
}
