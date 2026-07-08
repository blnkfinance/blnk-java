package com.blnkfinance.blnk;

import java.util.Map;

/**
 * What the client hands to a {@link BlnkTransport} for each attempt. The
 * per-attempt timeout is carried here so the transport can enforce its own
 * deadline; unit tests assert on {@code timeoutMs}.
 *
 * @param method    "POST" | "GET" | "PUT" | "DELETE"
 * @param headers   fully-built header map (insertion order = precedence order)
 * @param body      encoded body bytes, or null when the request has no body
 * @param multipart true when the body is a multipart form payload
 * @param timeoutMs per-attempt timeout in milliseconds; may be fractional,
 *                  zero, or non-finite — the transport clamps it
 */
public record TransportRequest(
    String method, Map<String, String> headers, byte[] body, boolean multipart, double timeoutMs) {}
