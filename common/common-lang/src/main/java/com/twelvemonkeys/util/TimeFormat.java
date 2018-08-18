/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.util;

import com.twelvemonkeys.lang.StringUtil;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Format for converting and parsing time.
 * <P>
 * The format is expressed in a string as follows:
 * <DL>
 * <DD>m (or any multiple of m's)
 * <DT>the minutes part (padded with 0's, if number has less digits than 
 *     the number of m's)
 *     m -> 0,1,...,59,60,61,...
 *     mm -> 00,01,...,59,60,61,...
 * <DD>s or ss
 * <DT>the seconds part (padded with 0's, if number has less digits than 
 *     the number of s's)
 *     s -> 0,1,...,59
 *     ss -> 00,01,...,59
 * <DD>S
 * <DT>all seconds (including the ones above 59)
 * </DL>
 * <P>
 * May not handle all cases, and formats... ;-)
 * Safest is: Always delimiters between the minutes (m) and seconds (s) part.
 * <P>
 * TODO: 
 * Move to com.twelvemonkeys.text?
 * Milliseconds!
 * Fix bugs.
 * Known bugs: 
 * <P>
 * The last character in the formatString is not escaped, while it should be. 
 * The first character after an escaped character is escaped while is shouldn't
 * be.
 * <P>
 * This is not a 100% compatible implementation of a java.text.Format.
 *
 * @see com.twelvemonkeys.util.Time
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 */
public class TimeFormat extends Format {
    final static String MINUTE = "m";
    final static String SECOND = "s";
    final static String TIME = "S";
    final static String ESCAPE = "\\";
    
    /**
     * The default time format 
     */

    private final static TimeFormat DEFAULT_FORMAT = new TimeFormat("m:ss");
    protected String formatString = null;
    
    /** 
     * Main method for testing ONLY
     */

    static void main(String[] argv) {
	Time time = null;
	TimeFormat in = null;
	TimeFormat out = null;

	if (argv.length >= 3) {
	    System.out.println("Creating out TimeFormat: \"" + argv[2] + "\"");
	    out = new TimeFormat(argv[2]);
	}

	if (argv.length >= 2) {
	    System.out.println("Creating in TimeFormat: \"" + argv[1] + "\"");
	    in = new TimeFormat(argv[1]);
	}
	else {
	    System.out.println("Using default format for in");
	    in = DEFAULT_FORMAT;
	}

	if (out == null)
	    out = in;

	if (argv.length >= 1) {
	    System.out.println("Parsing: \"" + argv[0] + "\" with format \""
			       + in.formatString + "\"");
	    time = in.parse(argv[0]);
	}
	else
	    time = new Time();
	
	System.out.println("Time is \"" +  out.format(time) +
			   "\" according to format \"" + out.formatString + "\"");
    }


    /**
     * The formatter array.
     */

    protected TimeFormatter[] formatter;

    /**
     * Creates a new TimeFormat with the given formatString,
     */

    public TimeFormat(String pStr) {
	formatString = pStr;

	Vector formatter = new Vector();
	StringTokenizer tok = new StringTokenizer(pStr, "\\msS", true);

	String previous = null;
	String current = null;
	int previousCount = 0;
	
	while (tok.hasMoreElements()) {
	    current = tok.nextToken();

	    if (previous != null && previous.equals(ESCAPE)) {
		// Handle escaping of s, S or m
		current = ((current != null) ? current : "") 
		    + (tok.hasMoreElements() ? tok.nextToken() : "");
		previous = null;
		previousCount = 0;
	    }
	    
	    // Skip over first,
	    // or if current is the same, increase count, and try again
	    if (previous == null || previous.equals(current)) {
		previousCount++;
		previous = current;
	    }
	    else {
		// Create new formatter for each part
		if (previous.equals(MINUTE))
		    formatter.add(new MinutesFormatter(previousCount));
		else if (previous.equals(SECOND))
		    formatter.add(new SecondsFormatter(previousCount));
		else if (previous.equals(TIME))
		    formatter.add(new SecondsFormatter(-1));
		else 
		    formatter.add(new TextFormatter(previous));

		previousCount = 1;
		previous = current;
		
	    }
	}

	// Add new formatter for last part
	if (previous != null) {
	    if (previous.equals(MINUTE))
		formatter.add(new MinutesFormatter(previousCount));
	    else if (previous.equals(SECOND))
		formatter.add(new SecondsFormatter(previousCount));
	    else if (previous.equals(TIME))
		formatter.add(new SecondsFormatter(-1));
	    else
		formatter.add(new TextFormatter(previous));
	}

	// Debug
	/*
	for (int i = 0; i < formatter.size(); i++) {
	    System.out.println("Formatter " + formatter.get(i).getClass() 
			       + ": length=" + ((TimeFormatter) formatter.get(i)).digits);
	}
	*/
	this.formatter = (TimeFormatter[])
	    formatter.toArray(new TimeFormatter[formatter.size()]);

    }

    /** 
     * DUMMY IMPLEMENTATION!!
     * Not locale specific.
     */

    public static TimeFormat getInstance() {
	return DEFAULT_FORMAT;
    }

    /** DUMMY IMPLEMENTATION!! */
    /* Not locale specific
    public static TimeFormat getInstance(Locale pLocale) {
	return DEFAULT_FORMAT;
    }
    */

    /** DUMMY IMPLEMENTATION!! */
    /* Not locale specific
    public static Locale[] getAvailableLocales() {
	return new Locale[] {Locale.getDefault()};
    }
    */

    /** Gets the format string.  */
    public String getFormatString() {
	return formatString;
    }

    /** DUMMY IMPLEMENTATION!! */
    public StringBuffer format(Object pObj, StringBuffer pToAppendTo,
			       FieldPosition pPos) {
	if (!(pObj instanceof Time)) {
	    throw new IllegalArgumentException("Must be instance of " + Time.class);
	}

	return pToAppendTo.append(format(pObj));
    }

    /**
     * Formats the the given time, using this format.
     */

    public String format(Time pTime) {
	StringBuilder buf = new StringBuilder();
	for (int i = 0; i < formatter.length; i++) {
	    buf.append(formatter[i].format(pTime));
	}
	return buf.toString();
    }

    /** DUMMY IMPLEMENTATION!! */
    public Object parseObject(String pStr, ParsePosition pStatus) {
	Time t = parse(pStr);
	
	pStatus.setIndex(pStr.length()); // Not 100%
	
	return t;
    }

    /**
     * Parses a Time, according to this format.
     * <p>
     * Will bug on some formats. It's safest to always use delimiters between
     * the minutes (m) and seconds (s) part.
     * 
     */
    public Time parse(String pStr) {
	Time time = new Time();

	int sec = 0;
	int min = 0;
	int pos = 0;
	int skip = 0;

	boolean onlyUseSeconds = false;

	for (int i = 0; (i < formatter.length)
		 && (pos + skip < pStr.length()) ; i++) {
	    // Go to next offset
	    pos += skip;
	    
	    if (formatter[i] instanceof MinutesFormatter) {
		// Parse MINUTES
		if ((i + 1) < formatter.length
		    && formatter[i + 1] instanceof TextFormatter) {
		    // Skip until next format element
		    skip = pStr.indexOf(((TextFormatter) formatter[i + 1]).text, pos);
		    // Error in format, try parsing to end
		    if (skip < 0)
			skip = pStr.length();
		}
		else if ((i + 1) >= formatter.length) {
		    // Skip until end of string
		    skip = pStr.length();
		}
		else {
		    // Hope this is correct...
		    skip = formatter[i].digits;
		}

		// May be first char
		if (skip > pos)
		    min = Integer.parseInt(pStr.substring(pos, skip));
	    }
	    else if (formatter[i] instanceof SecondsFormatter) {
		// Parse SECONDS
		if (formatter[i].digits == -1) {
		    // Only seconds (or full TIME)
		    if ((i + 1) < formatter.length
			&& formatter[i + 1] instanceof TextFormatter) {
			// Skip until next format element
			skip = pStr.indexOf(((TextFormatter) formatter[i + 1]).text, pos);

		    }
		    else if ((i + 1) >= formatter.length) {
			// Skip until end of string
			skip = pStr.length();
		    }
		    else {
			// Cannot possibly know how long?
			skip = 0;
			continue;					    
		    }
		    
		    // Get seconds
		    sec = Integer.parseInt(pStr.substring(pos, skip));
		    //		    System.out.println("Only seconds: " + sec);

		    onlyUseSeconds = true;
		    break;
		}
		else {
		    // Normal SECONDS
		    if ((i + 1) < formatter.length
			&& formatter[i + 1] instanceof TextFormatter) {
			// Skip until next format element
			skip = pStr.indexOf(((TextFormatter) formatter[i + 1]).text, pos);
			
		    }
		    else if ((i + 1) >= formatter.length) {
			// Skip until end of string
			skip = pStr.length();
		    }
		    else {
			skip = formatter[i].digits;
		    }
		    // Get seconds
		    sec = Integer.parseInt(pStr.substring(pos, skip));
		}
	    }
	    else if (formatter[i] instanceof TextFormatter) {
		skip = formatter[i].digits;
	    }
	    
	}

	// Set the minutes part if we should
	if (!onlyUseSeconds)
	    time.setMinutes(min);

	// Set the seconds part
	time.setSeconds(sec);

	return time;
    }
}

/**
 * The base class of TimeFormatters
 */
abstract class TimeFormatter {
    int digits = 0;

    abstract String format(Time t);
}

/**
 * Formats the seconds part of the Time
 */
class SecondsFormatter extends TimeFormatter {

    SecondsFormatter(int pDigits) {
	digits = pDigits;
    }
    
    String format(Time t) {
	// Negative number of digits, means all seconds, no padding
	if (digits < 0) {
            return Integer.toString(t.getTime());
        }

        // If seconds is more than digits long, simply return it
	if (t.getSeconds() >= Math.pow(10, digits)) {
            return Integer.toString(t.getSeconds());
        }

        // Else return it with leading 0's
	//return StringUtil.formatNumber(t.getSeconds(), digits);
        return StringUtil.pad("" + t.getSeconds(), digits, "0", true);
    }
}

/**
 * Formats the minutes part of the Time
 */
class MinutesFormatter extends TimeFormatter {

    MinutesFormatter(int pDigits) {
	digits = pDigits;
    }

    String format(Time t) {
	// If minutes is more than digits long, simply return it
	if (t.getMinutes() >= Math.pow(10, digits)) {
            return Integer.toString(t.getMinutes());
        }

        // Else return it with leading 0's
	//return StringUtil.formatNumber(t.getMinutes(), digits);
        return StringUtil.pad("" + t.getMinutes(), digits, "0", true);
    }
}

/**
 * Formats text constant part of the Time
 */
class TextFormatter extends TimeFormatter {
    String text = null;

    TextFormatter(String pText) {
	text = pText;

	// Just to be able to skip over
	if (pText != null) {
	    digits = pText.length();
	}
    }

    String format(Time t) {
	// Simply return the text
	return text;
    }

}
