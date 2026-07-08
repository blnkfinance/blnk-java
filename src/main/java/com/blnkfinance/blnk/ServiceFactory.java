package com.blnkfinance.blnk;

/**
 * Builds a service instance from the standard dependency seam
 * {@code (request, logger, formatResponse)}.
 */
@FunctionalInterface
public interface ServiceFactory {

  Object create(BlnkRequest request, BlnkLogger logger, FormatResponseFn formatResponse);
}
