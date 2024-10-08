/*
 * Copyright (C) 2020 Deque Systems Inc.,
 *
 * Your use of this Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This entire copyright notice must appear in every copy of this file you
 * distribute or in any file that contains substantial portions of this source
 * code.
 */

package com.deque.html.axecore.extensions;

import com.deque.html.axecore.providers.IAxeScriptProvider;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import javax.naming.OperationNotSupportedException;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Holds the Web driver injection extension methods. */
public final class WebDriverInjectorExtensions {
  /** initializes the WebDriverInjectorExtensions class. */
  private WebDriverInjectorExtensions() {}

  /**
   * Execute an synchronous JavaScript command.
   *
   * @param webDriver for the page to be scanned
   * @param command The command to be executed.
   * @param args Additional arguments to be provided to the command.
   * @return the results that would normally be provided to the synchronous commands callback.
   */
  public static Object executeScript(
      final WebDriver webDriver, final String command, final Object... args) {
    return ((JavascriptExecutor) webDriver).executeScript(command, args);
  }

  /**
   * Execute an asynchronous JavaScript command.
   *
   * @param webDriver for the page to be scanned
   * @param command The command to be executed.
   * @param args Additional arguments to be provided to the command.
   * @return the results that would normally be provided to the asynchronous commands callback.
   */
  public static Object executeAsyncScript(
      final WebDriver webDriver, final String command, final Object... args) {
    return ((JavascriptExecutor) webDriver).executeAsyncScript(command, args);
  }

  /**
   * Injects Axe script into frames. If a frame (not top-level) errors when injecting due to not
   * being displayed, the error is ignored.
   *
   * @param driver WebDriver instance to inject into
   * @param scriptProvider Provider that get the aXe script to inject
   * @param disableIframeTesting Whether or not to inject intto iframes
   * @throws OperationNotSupportedException if the operation errors out
   * @throws IOException if an IO exception occurs
   */
  public static void inject(
      final WebDriver driver, final IAxeScriptProvider scriptProvider, boolean disableIframeTesting)
      throws OperationNotSupportedException, IOException {
    inject(driver, scriptProvider.getScript(), disableIframeTesting, null, false);
  }

  /**
   * Injects Axe script into frames. If a frame (not top-level) errors when injecting due to not
   * being displayed, the error is ignored.
   *
   * @param driver WebDriver instance to inject into
   * @param script The script to inject
   * @param disableIframeTesting Whether or not to inject intto iframes
   */
  public static void inject(
      final WebDriver driver, final String script, boolean disableIframeTesting) {
    inject(driver, script, disableIframeTesting, null, false);
  }

  /**
   * Injects Axe script into frames. If a frame (not top-level) errors when injecting due to not
   * being displayed, the error is ignored.
   *
   * @param driver WebDriver instance to inject into
   * @param script The script to inject
   * @param disableIframeTesting Whether or not to inject intto iframes
   * @param injectCB Function to call whenever a frame is injected into
   * @param doNotInjectAxe Whether or not to inject axe
   */
  public static void inject(
      final WebDriver driver,
      final String script,
      boolean disableIframeTesting,
      Consumer<WebDriver> injectCB,
      boolean doNotInjectAxe) {
    JavascriptExecutor js = (JavascriptExecutor) driver;

    driver.switchTo().defaultContent();
    if (!doNotInjectAxe) {
      js.executeScript(script);
    }
    if (injectCB != null) {
      injectCB.accept(driver);
    }
    if (!disableIframeTesting) {
      injectIntoFrames(driver, script, injectCB, doNotInjectAxe);
    }
  }

  /**
   * Injects script into frames to be run asynchronously.
   *
   * @param driver WebDriver instance to inject into
   * @param script The script to inject
   * @param disableIframeTesting Whether or not to inject intto iframes
   */
  public static void injectAsync(
      final WebDriver driver, final String script, boolean disableIframeTesting) {
    JavascriptExecutor js = (JavascriptExecutor) driver;

    driver.switchTo().defaultContent();
    js.executeAsyncScript(script);

    if (!disableIframeTesting) {
      injectIntoFramesAsync(driver, script);
    }
  }

  /**
   * Recursively find frames and inject a script into them. If a frame errors when injecting due to
   * not being displayed, the error is ignored.
   *
   * @param driver An initialized WebDriver
   * @param script Script to inject
   */
  private static void injectIntoFrames(
      final WebDriver driver,
      final String script,
      Consumer<WebDriver> injectCB,
      boolean doNotInjectAxe) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    List<WebElement> frames =
        driver.findElements(By.xpath(".//*[local-name()='frame' or local-name()='iframe']"));

    for (WebElement frame : frames) {
      try {
        driver.switchTo().frame(frame);
        if (!doNotInjectAxe) {
          js.executeScript(script);
        }
        if (injectCB != null) {
          injectCB.accept(driver);
        }

        injectIntoFrames(driver, script, injectCB, doNotInjectAxe);

        driver.switchTo().parentFrame();
      } catch (Exception e) {
        // Ignore all errors except those caused by the injected javascript itself
        if (e instanceof JavascriptException) {
          throw e;
        }
      }
    }
  }

  /**
   * Recursively find frames and inject a script into them to be run asynchronously. If a frame
   * errors when injecting due to not being displayed, the error is ignored.
   *
   * @param driver An initialized WebDriver
   * @param script Script to inject
   */
  private static void injectIntoFramesAsync(final WebDriver driver, final String script) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    List<WebElement> frames =
        driver.findElements(By.xpath(".//*[local-name()='frame' or local-name()='iframe']"));

    for (WebElement frame : frames) {
      try {
        driver.switchTo().frame(frame);
        js.executeAsyncScript(script);

        injectIntoFramesAsync(driver, script);

        driver.switchTo().parentFrame();
      } catch (Exception e) {
        // Ignore all errors except those caused by the injected javascript itself
        if (e instanceof JavascriptException) {
          throw e;
        }
      }
    }
  }
}
