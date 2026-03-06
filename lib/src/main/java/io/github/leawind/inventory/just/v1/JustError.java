package io.github.leawind.inventory.just.v1;

import java.util.Formatter;
import java.util.Locale;

/**
 * @see <a href="https://doc.rust-lang.org/core/macro.panic.html">`panic` on doc.rust-lang.org</a>
 */
public class JustError extends Error {

  private JustError(String message) {
    super(message);
  }

  public static JustError panic(String message) {
    return new JustError(message);
  }

  public static JustError panic(String format, Object... args) {
    return new JustError(new Formatter().format(format, args).toString());
  }

  public static JustError panic(Locale l, String format, Object... args) {
    return new JustError(new Formatter(l).format(format, args).toString());
  }
}
