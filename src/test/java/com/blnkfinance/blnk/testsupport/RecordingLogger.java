package com.blnkfinance.blnk.testsupport;

import com.blnkfinance.blnk.BlnkLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A logger that records every call so tests can assert on the log level,
 * message, and metadata of each entry.
 */
public final class RecordingLogger implements BlnkLogger {

  public record LogEntry(String level, String message, List<Object> meta) {}

  public final List<LogEntry> entries = new ArrayList<>();

  @Override
  public void info(String message, Object... meta) {
    entries.add(new LogEntry("info", message, Arrays.asList(meta)));
  }

  @Override
  public void error(String message, Object... meta) {
    entries.add(new LogEntry("error", message, Arrays.asList(meta)));
  }

  @Override
  public void debug(String message, Object... meta) {
    entries.add(new LogEntry("debug", message, Arrays.asList(meta)));
  }

  /** All entries of a level, in order. */
  public List<LogEntry> byLevel(String level) {
    return entries.stream().filter(e -> e.level().equals(level)).toList();
  }
}
