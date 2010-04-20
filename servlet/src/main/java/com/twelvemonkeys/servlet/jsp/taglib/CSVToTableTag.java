/*
 * Copyright (c) 2002 TwelveMonkeys.
 * All rights reserved.
 *
 * $Log: CSVToTableTag.java,v $
 * Revision 1.3  2003/10/06 14:24:50  WMHAKUR
 * Code clean-up only.
 *
 * Revision 1.2  2002/11/26 17:33:49  WMHAKUR
 * Added documentation & removed System.out.println()s.
 *
 * Revision 1.1  2002/11/19 10:50:10  WMHAKUR
 * Renamed from CSVToTable, to follow naming conventions.
 *
 * Revision 1.1  2002/11/18 22:11:16  WMHAKUR
 * Tag to convert CSV to HTML table.
 * Can be further transformed, using XSLT.
 *
 */

package com.twelvemonkeys.servlet.jsp.taglib;

import java.util.*;
import java.io.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 * Creates a table from a string of "comma-separated values" (CSV).
 * The delimiter character can be any character (or combination of characters).
 * The default delimiter is TAB ({@code \t}).
 *
 * <P/>
 * <HR/>
 * <P/>
 *
 * The input may look like this:
 * <PRE>
 * &lt;c:totable firstRowIsHeader="true" delimiter=";"&gt;
 *   header A;header B
 *   data 1A; data 1B
 *   data 2A; data 2B
 * &lt;/c:totable&gt;
 * </PRE>
 *
 * The output (source) will look like this:
 * <PRE>
 * &lt;TABLE&gt;
 *   &lt;TR&gt;
 *      &lt;TH&gt;header A&lt;/TH&gt;&lt;TH&gt;header B&lt;/TH&gt;
 *   &lt;/TR&gt;
 *   &lt;TR&gt;
 *      &lt;TD&gt;data 1A&lt;/TD&gt;&lt;TD&gt;data 1B&lt;/TD&gt;
 *   &lt;/TR&gt;
 *   &lt;TR&gt;
 *      &lt;TD&gt;data 2A&lt;/TD&gt;&lt;TD&gt;data 2B&lt;/TD&gt;
 *   &lt;/TR&gt;
 * &lt;/TABLE&gt;
 * </PRE>
 * You wil probably want to use XSLT to make the final output look nicer. :-)
 *
 * @see StringTokenizer
 * @see <A href="http://www.w3.org/TR/xslt">XSLT spec</A>
 *
 * @author Harald Kuhr
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/jsp/taglib/CSVToTableTag.java#1 $
 */

public class CSVToTableTag extends ExBodyTagSupport {

    public final static String TAB = "\t";

    protected String mDelimiter = null;
    protected boolean mFirstRowIsHeader = false;
    protected boolean mFirstColIsHeader = false;

    public void setDelimiter(String pDelimiter) {
        mDelimiter = pDelimiter;
    }

    public String getDelimiter() {
        return mDelimiter != null ? mDelimiter : TAB;
    }

    public void setFirstRowIsHeader(String pBoolean) {
        mFirstRowIsHeader = Boolean.valueOf(pBoolean).booleanValue();
    }

    public void setFirstColIsHeader(String pBoolean) {
        mFirstColIsHeader = Boolean.valueOf(pBoolean).booleanValue();
    }


    public int doEndTag() throws JspException {
        BodyContent content = getBodyContent();

        try {
            Table table =
                    Table.parseContent(content.getReader(), getDelimiter());

            JspWriter out = pageContext.getOut();

            //System.out.println("CSVToTable: " + table.getRows() + " rows, "
            //                   + table.getCols() + " cols.");

            if (table.getRows() > 0) {
                out.println("<TABLE>");
                // Loop over rows
                for (int row = 0; row < table.getRows(); row++) {
                    out.println("<TR>");

                    // Loop over cells in each row
                    for (int col = 0; col < table.getCols(); col++) {
                        // Test if we are using headers, else normal cell
                        if (mFirstRowIsHeader && row == 0
                                || mFirstColIsHeader && col == 0) {
                            out.println("<TH>" + table.get(row, col)
                                        + " </TH>");
                        }
                        else {
                            out.println("<TD>" + table.get(row, col)
                                        + " </TD>");
                        }
                    }

                    out.println("</TR>");

                }
                out.println("</TABLE>");
            }
        }
        catch (IOException ioe) {
            throw new JspException(ioe);
        }

        return super.doEndTag();
    }

    static class Table {
        List mRows = null;
        int mCols = 0;

        private Table(List pRows, int pCols) {
            mRows = pRows;
            mCols = pCols;
        }

        int getRows() {
            return mRows != null ? mRows.size() : 0;
        }

        int getCols() {
            return mCols;
        }

        List getTableRows() {
            return mRows;
        }

        List getTableRow(int pRow) {
            return mRows != null
                    ? (List) mRows.get(pRow)
                    : Collections.EMPTY_LIST;
        }

        String get(int pRow, int pCol) {
            List row = getTableRow(pRow);
            // Rows may contain unequal number of cols
            return (row.size() > pCol) ? (String) row.get(pCol) : "";
        }

        /**
         * Parses a BodyContent to a table.
         *
         */

        static Table parseContent(Reader pContent, String pDelim)
                throws IOException {
            ArrayList tableRows = new ArrayList();
            int tdsPerTR = 0;

            // Loop through TRs
            BufferedReader reader = new BufferedReader(pContent);
            String tr = null;
            while ((tr = reader.readLine()) != null) {
                // Discard blank lines
                if (tr != null
                        && tr.trim().length() <= 0 && tr.indexOf(pDelim) < 0) {
                    continue;
                }

                //System.out.println("CSVToTable: read LINE=\"" + tr + "\"");

                ArrayList tableDatas = new ArrayList();
                StringTokenizer tableRow = new StringTokenizer(tr, pDelim,
                                                               true);

                boolean lastWasDelim = false;
                while (tableRow.hasMoreTokens()) {
                    String td = tableRow.nextToken();

                    //System.out.println("CSVToTable: read data=\"" + td + "\"");

                    // Test if we have empty TD
                    if (td.equals(pDelim)) {
                        if (lastWasDelim) {
                            // Add empty TD
                            tableDatas.add("");
                        }

                        // We just read a delimitter
                        lastWasDelim = true;
                    }
                    else {
                        // No tab, normal data
                        lastWasDelim = false;

                        // Add normal TD
                        tableDatas.add(td);
                    }
                } // end while (tableRow.hasNext())

                // Store max TD count
                if (tableDatas.size() > tdsPerTR) {
                    tdsPerTR = tableDatas.size();
                }

                // Add a table row
                tableRows.add(tableDatas);
            }

            // Return TABLE
            return new Table(tableRows, tdsPerTR);
        }
    }


}
