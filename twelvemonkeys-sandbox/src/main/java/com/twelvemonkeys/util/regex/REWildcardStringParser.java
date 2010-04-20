/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.util.regex;

import com.twelvemonkeys.util.DebugUtil;

import java.io.PrintStream;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class parses arbitrary strings against a wildcard string mask provided.
 * The wildcard characters are '*' and '?'.
 * <p>
 * The string masks provided are treated as case sensitive.<br>
 * Null-valued string masks as well as null valued strings to be parsed, will lead to rejection.
 *
 * <p><hr style="height=1"><p>
 *
 * This task is performed based on regular expression techniques.
 * The possibilities of string generation with the well-known wildcard characters stated above,
 *    represent a subset of the possibilities of string generation with regular expressions.<br>
 * The '*' corresponds to ([Union of all characters in the alphabet])*<br>
 * The '?' corresponds to ([Union of all characters in the alphabet])<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<small>These expressions are not suited for textual representation at all, I must say. Is there any math tags included in HTML?</small>
 *
 * <p>
 *
 * This class uses the Regexp package from Apache's Jakarta Project, links below.
 *
 * <p><hr style="height=1"><p>
 *
 * Examples of usage:<br>
 * This example will return "Accepted!".
 * <pre>
 * REWildcardStringParser parser = new REWildcardStringParser("*_28????.jp*");
 * if (parser.parseString("gupu_280915.jpg")) {
 *     System.out.println("Accepted!");
 * } else {
 *     System.out.println("Not accepted!");
 * }
 * </pre>
 *
 * <p><hr style="height=1"><p>
 *
 * @author <a href="mailto:eirik.torske@iconmedialab.no">Eirik Torske</a>
 * @see <a href="http://jakarta.apache.org/regexp/">Jakarta Regexp</a>
 * @see <a href="http://jakarta.apache.org/regexp/apidocs/org/apache/regexp/RE.html">{@code org.apache.regexp.RE}</a>
 * @see com.twelvemonkeys.util.regex.WildcardStringParser
 *
 * @todo Rewrite to use this regex package, and not Jakarta directly!
 */
public class REWildcardStringParser /*extends EntityObject*/ {

  // Constants

  /** Field ALPHABET           */
  public static final char[] ALPHABET = {
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'æ',
    'ø', 'å', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'N', 'M', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y',
    'Z', 'Æ', 'Ø', 'Å', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '_', '-'
  };

  /** Field FREE_RANGE_CHARACTER           */
  public static final char FREE_RANGE_CHARACTER = '*';

  /** Field FREE_PASS_CHARACTER           */
  public static final char FREE_PASS_CHARACTER = '?';

  // Members
  Pattern mRegexpParser;
  String      mStringMask;
  boolean     mInitialized = false;
  int         mTotalNumberOfStringsParsed;
  boolean     mDebugging;
  PrintStream out;

  // Properties
  // Constructors

  /**
   * Creates a wildcard string parser.
   * <p>
   * @param pStringMask the wildcard string mask.
   */
  public REWildcardStringParser(final String pStringMask) {
    this(pStringMask, false);
  }

  /**
   * Creates a wildcard string parser.
   * <p>
   * @param pStringMask the wildcard string mask.
   * @param pDebugging {@code true} will cause debug messages to be emitted to {@code System.out}.
   */
  public REWildcardStringParser(final String pStringMask, final boolean pDebugging) {
    this(pStringMask, pDebugging, System.out);
  }

  /**
   * Creates a wildcard string parser.
   * <p>
   * @param pStringMask the wildcard string mask.
   * @param pDebugging {@code true} will cause debug messages to be emitted.
   * @param pDebuggingPrintStream the {@code java.io.PrintStream} to which the debug messages will be emitted.
   */
  public REWildcardStringParser(final String pStringMask, final boolean pDebugging, final PrintStream pDebuggingPrintStream) {

    this.mStringMask = pStringMask;
    this.mDebugging  = pDebugging;
    this.out         = pDebuggingPrintStream;
    mInitialized     = buildRegexpParser();
  }

  // Methods

  /**
   * Converts wildcard string mask to regular expression.
   * This method should reside in som utility class, but I don't know how proprietary the regular expression format is...
   * @return the corresponding regular expression or {@code null} if an error occurred.
   */
  private String convertWildcardExpressionToRegularExpression(final String pWildcardExpression) {

    if (pWildcardExpression == null) {
      if (mDebugging) {
        out.println(DebugUtil.getPrefixDebugMessage(this) + "wildcard expression is null - also returning null as regexp!");
      }
      return null;
    }
    StringBuilder regexpBuffer    = new StringBuilder();
    boolean      convertingError = false;

    for (int i = 0; i < pWildcardExpression.length(); i++) {
      if (convertingError) {
        return null;
      }

      // Free-range character '*'
      char stringMaskChar = pWildcardExpression.charAt(i);

      if (isFreeRangeCharacter(stringMaskChar)) {
        regexpBuffer.append("(([a-åA-Å0-9]|.|_|-)*)");
      }

      // Free-pass character '?'
      else if (isFreePassCharacter(stringMaskChar)) {
        regexpBuffer.append("([a-åA_Å0-9]|.|_|-)");
      }

      // Valid characters
      else if (isInAlphabet(stringMaskChar)) {
        regexpBuffer.append(stringMaskChar);
      }

      // Invalid character - aborting
      else {
        if (mDebugging) {
          out.println(DebugUtil.getPrefixDebugMessage(this)
                      + "one or more characters in string mask are not legal characters - returning null as regexp!");
        }
        convertingError = true;
      }
    }
    return regexpBuffer.toString();
  }

  /**
   * Builds the regexp parser.
   */
  private boolean buildRegexpParser() {

    // Convert wildcard string mask to regular expression
    String regexp = convertWildcardExpressionToRegularExpression(mStringMask);

    if (regexp == null) {
      out.println(DebugUtil.getPrefixErrorMessage(this)
                  + "irregularity in regexp conversion - now not able to parse any strings, all strings will be rejected!");
      return false;
    }

    // Instantiate a regular expression parser
    try {
      mRegexpParser = Pattern.compile(regexp);
    }
    catch (PatternSyntaxException e) {
      if (mDebugging) {
        out.println(DebugUtil.getPrefixErrorMessage(this) + "RESyntaxException \"" + e.getMessage()
                    + "\" caught - now not able to parse any strings, all strings will be rejected!");
      }
      if (mDebugging) {
        e.printStackTrace(System.err);
      }
      return false;
    }
    if (mDebugging) {
      out.println(DebugUtil.getPrefixDebugMessage(this) + "regular expression parser from regular expression " + regexp
                  + " extracted from wildcard string mask " + mStringMask + ".");
    }
    return true;
  }

  /**
   * Simple check of the string to be parsed.
   */
  private boolean checkStringToBeParsed(final String pStringToBeParsed) {

    // Check for nullness
    if (pStringToBeParsed == null) {
      if (mDebugging) {
        out.println(DebugUtil.getPrefixDebugMessage(this) + "string to be parsed is null - rejection!");
      }
      return false;
    }

    // Check if valid character (element in alphabet)
    for (int i = 0; i < pStringToBeParsed.length(); i++) {
      if (!isInAlphabet(pStringToBeParsed.charAt(i))) {
        if (mDebugging) {
          out.println(DebugUtil.getPrefixDebugMessage(this)
                      + "one or more characters in string to be parsed are not legal characters - rejection!");
        }
        return false;
      }
    }
    return true;
  }

  /**
   * Tests if a certain character is a valid character in the alphabet that is applying for this automaton.
   */
  public static boolean isInAlphabet(final char pCharToCheck) {

    for (int i = 0; i < ALPHABET.length; i++) {
      if (pCharToCheck == ALPHABET[i]) {
        return true;
      }
    }
    return false;
  }

  /**
   * Tests if a certain character is the designated "free-range" character ('*').
   */
  public static boolean isFreeRangeCharacter(final char pCharToCheck) {
    return pCharToCheck == FREE_RANGE_CHARACTER;
  }

  /**
   * Tests if a certain character is the designated "free-pass" character ('?').
   */
  public static boolean isFreePassCharacter(final char pCharToCheck) {
    return pCharToCheck == FREE_PASS_CHARACTER;
  }

  /**
   * Tests if a certain character is a wildcard character ('*' or '?').
   */
  public static boolean isWildcardCharacter(final char pCharToCheck) {
    return ((isFreeRangeCharacter(pCharToCheck)) || (isFreePassCharacter(pCharToCheck)));
  }

  /**
   * Gets the string mask that was used when building the parser atomaton.
   * <p>
   * @return the string mask used for building the parser automaton.
   */
  public String getStringMask() {
    return mStringMask;
  }

  /**
   * Parses a string.
   * <p>
   *
   * @param pStringToBeParsed
   * @return {@code true} if and only if the string are accepted by the parser.
   */
  public boolean parseString(final String pStringToBeParsed) {

    if (mDebugging) {
      out.println();
    }
    if (mDebugging) {
      out.println(DebugUtil.getPrefixDebugMessage(this) + "parsing \"" + pStringToBeParsed + "\"...");
    }

    // Update statistics
    mTotalNumberOfStringsParsed++;

    // Check string to be parsed
    if (!checkStringToBeParsed(pStringToBeParsed)) {
      return false;
    }

    // Perform parsing and return accetance/rejection flag
    if (mInitialized) {
      return mRegexpParser.matcher(pStringToBeParsed).matches();
    } else {
      out.println(DebugUtil.getPrefixErrorMessage(this) + "trying to use non-initialized parser - string rejected!");
    }
    return false;
  }

  /*
   * Overriding mandatory methods from EntityObject's.
   */

  /**
   * Method toString
   *
   *
   * @return
   *
   */
  public String toString() {

    StringBuilder buffer = new StringBuilder();

    buffer.append(DebugUtil.getClassName(this));
    buffer.append(":  String mask ");
    buffer.append(mStringMask);
    buffer.append("\n");
    return buffer.toString();
  }

  // Just taking the lazy, easy and dangerous way out

  /**
   * Method equals
   *
   *
   * @param pObject
   *
   * @return
   *
   */
  public boolean equals(Object pObject) {

    if (pObject instanceof REWildcardStringParser) {
      REWildcardStringParser externalParser = (REWildcardStringParser) pObject;

      return (externalParser.mStringMask == this.mStringMask);
    }
    return ((Object) this).equals(pObject);
  }

  // Just taking the lazy, easy and dangerous way out

  /**
   * Method hashCode
   *
   *
   * @return
   *
   */
  public int hashCode() {
    return ((Object) this).hashCode();
  }

  protected Object clone() throws CloneNotSupportedException {
    return new REWildcardStringParser(mStringMask);
  }

  // Just taking the lazy, easy and dangerous way out
  protected void finalize() throws Throwable {}
}


/*--- Formatted in Sun Java Convention Style on ma, des 1, '03 ---*/


/*------ Formatted by Jindent 3.23 Basic 1.0 --- http://www.jindent.de ------*/
