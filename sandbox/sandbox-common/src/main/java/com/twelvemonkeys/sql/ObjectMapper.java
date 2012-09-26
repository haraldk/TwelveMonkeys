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

package com.twelvemonkeys.sql;

import com.twelvemonkeys.lang.*;

import java.lang.reflect.*;

// Single-type import, to avoid util.Date/sql.Date confusion
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.StringTokenizer;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * A class for mapping JDBC ResultSet rows to Java objects.
 * 
 * @see ObjectReader
 *
 * @author Harald Kuhr (haraldk@iconmedialab.no)
 * @author last modified by $Author: haku $
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-sandbox/src/main/java/com/twelvemonkeys/sql/ObjectMapper.java#1 $
 *
 * @todo Use JDK logging instead of proprietary logging.
 */
public class ObjectMapper {
    final static String DIRECTMAP = "direct";
    final static String OBJECTMAP = "object";
    final static String COLLECTIONMAP = "collection";
    final static String OBJCOLLMAP = "objectcollection";
    
    Class mInstanceClass = null;

    Hashtable mMethods = null;

    Hashtable mColumnMap = null;
    Hashtable mPropertiesMap = null;

    Hashtable mJoins = null;

    private Hashtable mTables = null;
    private Vector mColumns = null;

    Hashtable mForeignKeys = null;
    Hashtable mPrimaryKeys = null;
    Hashtable mMapTypes = null;
    Hashtable mClasses = null;

    String mPrimaryKey = null;
    String mForeignKey = null;
    String mIdentityJoin = null;

    Log mLog = null;

    /**
     * Creates a new ObjectMapper for a DatabaseReadable 
     *
     * @param obj An object of type DatabaseReadable
     */

    /*
      public ObjectMapper(DatabaseReadable obj) {
      this(obj.getClass(), obj.getMapping());
      }
    */
    /**
     * Creates a new ObjectMapper for any object, given a mapping
     *
     * @param objClass The class of the object(s) created by this OM
     * @param mapping an Hashtable containing the mapping information 
     * for this OM
     */

    public ObjectMapper(Class pObjClass, Hashtable pMapping) {
        mLog = new Log(this);

        mInstanceClass = pObjClass;

        mJoins = new Hashtable();
        mPropertiesMap = new Hashtable();
        mColumnMap = new Hashtable();

        mClasses = new Hashtable();
        mMapTypes = new Hashtable();
        mForeignKeys = new Hashtable();
        mPrimaryKeys = new Hashtable();

        // Unpack and store mapping information
        for (Enumeration keys = pMapping.keys(); keys.hasMoreElements();) {
            String key  = (String) keys.nextElement();
            String value = (String) pMapping.get(key);
	    
            int dotIdx = key.indexOf(".");
	    
            if (dotIdx >= 0) {
                if (key.equals(".primaryKey")) {
                    // Primary key
                    mPrimaryKey = (String) pMapping.get(value); 
                }
                else if (key.equals(".foreignKey")) {
                    // Foreign key
                    mForeignKey = (String) pMapping.get(value);
                }
                else if (key.equals(".join")) {
                    // Identity join
                    mIdentityJoin = (String) pMapping.get(key);
                }
                else if (key.endsWith(".primaryKey")) {
                    // Primary key in joining table
                    mPrimaryKeys.put(key.substring(0, dotIdx), value);
                }
                else if (key.endsWith(".foreignKey")) {
                    // Foreign key
                    mForeignKeys.put(key.substring(0, dotIdx), value);
                }
                else if (key.endsWith(".join")) {
                    // Joins
                    mJoins.put(key.substring(0, dotIdx), value);
                }
                else if (key.endsWith(".mapType")) {
                    // Maptypes
                    value = value.toLowerCase();
		    
                    if (value.equals(DIRECTMAP) || value.equals(OBJECTMAP) || 
                        value.equals(COLLECTIONMAP) || 
                        value.equals(OBJCOLLMAP)) {
                        mMapTypes.put(key.substring(0, dotIdx), value);
                    }
                    else {
                        mLog.logError("Illegal mapType: \"" + value + "\"! " 
                                      + "Legal types are: direct, object, "
                                      + "collection and objectCollection.");
                    }
                }
                else if (key.endsWith(".class")) {
                    // Classes
                    try {
                        mClasses.put(key.substring(0, dotIdx), 
                                     Class.forName(value));
                    }
                    catch (ClassNotFoundException e) {
                        mLog.logError(e);
                        //e.printStackTrace();
                    }
                }
                else if (key.endsWith(".collection")) {
                    // TODO!!
                }
            }
            else {
                // Property to column mappings
                mPropertiesMap.put(key, value);
                mColumnMap.put(value.substring(value.lastIndexOf(".") + 1), 
                               key);
            }
        }

        mMethods = new Hashtable();
        Method[] methods = mInstanceClass.getMethods();
        for (int i = 0; i < methods.length; i++) {
            // Two methods CAN have same name...    
            mMethods.put(methods[i].getName(), methods[i]); 
        }
    }

    public void setPrimaryKey(String pPrimaryKey) {
        mPrimaryKey = pPrimaryKey;
    }
    
    /**
     * Gets the name of the property, that acts as the unique identifier for
     * this ObjectMappers type.
     *
     * @return The name of the primary key property
     */

    public String getPrimaryKey() {
        return mPrimaryKey;
    }

    public String getForeignKey() {
        return mForeignKey;
    }

    /**
     * Gets the join, that is needed to find this ObjectMappers type.
     *
     * @return The name of the primary key property
     */

    public String getIdentityJoin() {
        return mIdentityJoin;
    }

    Hashtable getPropertyMapping(String pProperty) {
        Hashtable mapping = new Hashtable();

        if (pProperty != null) {
            // Property
            if (mPropertiesMap.containsKey(pProperty))
                mapping.put("object", mPropertiesMap.get(pProperty));

            // Primary key
            if (mPrimaryKeys.containsKey(pProperty)) {
                mapping.put(".primaryKey", "id");
                mapping.put("id", mPrimaryKeys.get(pProperty));
            }

            //Foreign key
            if (mForeignKeys.containsKey(pProperty))
                mapping.put(".foreignKey", mPropertiesMap.get(mForeignKeys.get(pProperty)));

            // Join
            if (mJoins.containsKey(pProperty))
                mapping.put(".join", mJoins.get(pProperty));

            // mapType
            mapping.put(".mapType", "object");
        }

        return mapping;
    }
    

    /**
     * Gets the column for a given property.
     *
     * @param property The property
     * @return The name of the matching database column, on the form 
     *         table.column
     */

    public String getColumn(String pProperty) {
        if (mPropertiesMap == null || pProperty == null)
            return null;
        return (String) mPropertiesMap.get(pProperty);
    }

    /**
     * Gets the table name for a given property.
     *
     * @param property The property
     * @return The name of the matching database table.
     */

    public String getTable(String pProperty) {
        String table = getColumn(pProperty);

        if (table != null) {
            int dotIdx = 0;
            if ((dotIdx = table.lastIndexOf(".")) >= 0)
                table = table.substring(0, dotIdx);
            else
                return null;
        }

        return table;
    }

    /**
     * Gets the property for a given database column. If the column incudes
     * table qualifier, the table qualifier is removed.
     *
     * @param column The name of the column
     * @return The name of the mathcing property
     */

    public String getProperty(String pColumn) {
        if (mColumnMap == null || pColumn == null)
            return null;

        String property = (String) mColumnMap.get(pColumn);

        int dotIdx = 0;
        if (property == null && (dotIdx = pColumn.lastIndexOf(".")) >= 0)
            property = (String) mColumnMap.get(pColumn.substring(dotIdx + 1));

        return property;
    }


    /**
     * Maps each row of the given result set to an object ot this OM's type.
     *
     * @param rs The ResultSet to process (map to objects)
     * @return An array of objects (of this OM's class). If there are no rows  
     * in the ResultSet, an empty (zero-length) array will be returned.
     */

    public synchronized Object[] mapObjects(ResultSet pRSet) throws SQLException {
        Vector result = new Vector();

        ResultSetMetaData meta = pRSet.getMetaData();
        int cols = meta.getColumnCount();

        // Get colum names
        String[] colNames = new String[cols];
        for (int i = 0; i < cols; i++) {
            colNames[i] = meta.getColumnName(i + 1); // JDBC cols start at 1...

            /*
              System.out.println(meta.getColumnLabel(i + 1));
              System.out.println(meta.getColumnName(i + 1));
              System.out.println(meta.getColumnType(i + 1));
              System.out.println(meta.getColumnTypeName(i + 1));
              //	    System.out.println(meta.getTableName(i + 1));
              //	    System.out.println(meta.getCatalogName(i + 1));
              //	    System.out.println(meta.getSchemaName(i + 1));
              // Last three NOT IMPLEMENTED!!
              */
        }

        // Loop through rows in resultset
        while (pRSet.next()) {
            Object obj = null;
	    
            try {
                obj = mInstanceClass.newInstance(); // Asserts empty constructor!
            }
            catch (IllegalAccessException iae) {
                mLog.logError(iae);
                // iae.printStackTrace();
            }
            catch (InstantiationException ie) {
                mLog.logError(ie);
                // ie.printStackTrace();
            }
	    
            // Read each colum from this row into object 
            for (int i = 0; i < cols; i++) {

                String property = (String) mColumnMap.get(colNames[i]);

                if (property != null) {
                    // This column is mapped to a property
                    mapColumnProperty(pRSet, i + 1, property, obj);
                }
            }

            // Add object to the result Vector
            result.addElement(obj);
        }	
	
        return result.toArray((Object[]) Array.newInstance(mInstanceClass, 
                                                           result.size()));
    }

    /**
     * Maps a ResultSet column (from the current ResultSet row) to a named 
     * property of an object, using reflection.
     *
     * @param rs The JDBC ResultSet
     * @param index The column index to get the value from
     * @param property The name of the property to set the value of
     * @param obj The object to set the property to
     */

    void mapColumnProperty(ResultSet pRSet, int pIndex, String pProperty, 
                           Object pObj) {
        if (pRSet == null || pProperty == null || pObj == null)
            throw new IllegalArgumentException("ResultSet, Property or Object"
                                               + " arguments cannot be null!");
        if (pIndex <= 0)
            throw new IllegalArgumentException("Index parameter must be > 0!");
	    
        String methodName = "set" + StringUtil.capitalize(pProperty);
        Method setMethod = (Method) mMethods.get(methodName);
	
        if (setMethod == null) {
            // No setMethod for this property
            mLog.logError("No set method for property \"" 
                          + pProperty + "\" in " + pObj.getClass() + "!");
            return;
        }
	
        // System.err.println("DEBUG: setMethod=" + setMethod);
	
        Method getMethod = null;
	
        String type = "";
        try {
            Class[] cl = {Integer.TYPE};
            type = setMethod.getParameterTypes()[0].getName();
	    
            type = type.substring(type.lastIndexOf(".") + 1);
	    
            // There is no getInteger, use getInt instead
            if (type.equals("Integer")) {
                type = "int";
            }
	    
            // System.err.println("DEBUG: type=" + type);	    
            getMethod = pRSet.getClass().
                getMethod("get" + StringUtil.capitalize(type), cl);
        }
        catch (Exception e) {
            mLog.logError("Can't find method \"get" 
                          + StringUtil.capitalize(type) + "(int)\" " 
                          + "(for class " + StringUtil.capitalize(type) 
                          + ") in ResultSet", e);
	    
            return;
        }
	
        try {
            // Get the data from the DB
            // System.err.println("DEBUG: " + getMethod.getName() + "(" + (i + 1) + ")");
	    
            Object[] colIdx = {new Integer(pIndex)};
            Object[] arg = {getMethod.invoke(pRSet, colIdx)};
	    
            // Set it to the object
            // System.err.println("DEBUG: " + setMethod.getName() + "(" + arg[0] + ")");
            setMethod.invoke(pObj, arg);
        }
        catch (InvocationTargetException ite) {
            mLog.logError(ite);
            // ite.printStackTrace();
        }		    	    
        catch (IllegalAccessException iae) {
            mLog.logError(iae);
            // iae.printStackTrace();
        }		    
    }

    /**
     * Creates a SQL query string to get the primary keys for this 
     * ObjectMapper.
     */

    String buildIdentitySQL(String[] pKeys) {
        mTables = new Hashtable();
        mColumns = new Vector();

        // Get columns to select
        mColumns.addElement(getPrimaryKey());

        // Get tables to select (and join) from and their joins 
        tableJoins(null, false);

        for (int i = 0; i < pKeys.length; i++) {
            tableJoins(getColumn(pKeys[i]), true);
        }

        // All data read, build SQL query string
        return "SELECT " + getPrimaryKey() + " " + buildFromClause() 
            + buildWhereClause(); 
    }

    /**
     * Creates a SQL query string to get objects for this ObjectMapper.
     */

    public String buildSQL() {
        mTables = new Hashtable();
        mColumns = new Vector();

        String key = null;
        for (Enumeration keys = mPropertiesMap.keys(); keys.hasMoreElements();) {
            key = (String) keys.nextElement();
    
            // Get columns to select
            String column = (String) mPropertiesMap.get(key);
            mColumns.addElement(column);
    
            tableJoins(column, false);
        }

        // All data read, build SQL query string
        return buildSelectClause() + buildFromClause() 
            + buildWhereClause(); 
    }

    /**
     * Builds a SQL SELECT clause from  the columns Vector
     */

    private String buildSelectClause() {
        StringBuilder sqlBuf = new StringBuilder();

        sqlBuf.append("SELECT ");

        String column = null;
        for (Enumeration select = mColumns.elements(); select.hasMoreElements();) {
            column = (String) select.nextElement();

            /*
              String subColumn = column.substring(column.indexOf(".") + 1);
              // System.err.println("DEBUG: col=" + subColumn);
              String mapType = (String) mMapTypes.get(mColumnMap.get(subColumn));
            */
            String mapType = (String) mMapTypes.get(getProperty(column));

            if (mapType == null || mapType.equals(DIRECTMAP)) {
                sqlBuf.append(column);

                sqlBuf.append(select.hasMoreElements() ? ", " : " ");
            }
        }
	
        return sqlBuf.toString();
    }

    /**
     * Builds a SQL FROM clause from the tables/joins Hashtable
     */

    private String buildFromClause() {
        StringBuilder sqlBuf = new StringBuilder();

        sqlBuf.append("FROM ");

        String table = null;
        String schema = null;
        for (Enumeration from = mTables.keys(); from.hasMoreElements();) {
            table = (String) from.nextElement();
            /*
              schema = (String) schemas.get(table);

              if (schema != null)
              sqlBuf.append(schema + ".");
            */

            sqlBuf.append(table);
            sqlBuf.append(from.hasMoreElements() ? ", " : " ");
        }

        return sqlBuf.toString();
    }

    /**
     * Builds a SQL WHERE clause from the tables/joins Hashtable
     *
     * @return Currently, this metod will return "WHERE 1 = 1", if no other 
     *         WHERE conditions are specified. This can be considered a hack.
     */

    private String buildWhereClause() {
	
        StringBuilder sqlBuf = new StringBuilder();
       
        String join = null;
        boolean first = true;

        for (Enumeration where = mTables.elements(); where.hasMoreElements();) {
            join = (String) where.nextElement();
	    
            if (join.length() > 0) {
                if (first) {
                    // Skip " AND " in first iteration
                    first = false;
                }
                else {
                    sqlBuf.append(" AND ");
                }
            }

            sqlBuf.append(join);
        }

        if (sqlBuf.length() > 0) 
            return "WHERE " + sqlBuf.toString();
	
        return "WHERE 1 = 1"; // Hacky...
    }

    /**
     * Finds tables used in mappings and joins and adds them to the tables
     * Hashtable, with the table name as key, and the join as value.
     */

    private void tableJoins(String pColumn, boolean pWhereJoin) {
        String join = null;
        String table = null;
	    
        if (pColumn == null) {
            // Identity
            join = getIdentityJoin();
            table = getTable(getProperty(getPrimaryKey()));
        }
        else {
            // Normal
            int dotIndex = -1;
            if ((dotIndex = pColumn.lastIndexOf(".")) <= 0) {
                // No table qualifier
                return;
            }
	    
            // Get table qualifier.
            table = pColumn.substring(0, dotIndex);
	    
            // Don't care about the tables that are not supposed to be selected from
            String property = (String) getProperty(pColumn);
	    
            if (property != null) {
                String mapType = (String) mMapTypes.get(property);
                if (!pWhereJoin && mapType != null && !mapType.equals(DIRECTMAP)) {
                    return;
                }

                join = (String) mJoins.get(property);    
            }	
        }

        // If table is not in the tables Hash, add it, and check for joins.
        if (mTables.get(table) == null) {
            if (join != null) {
                mTables.put(table, join);
		
                StringTokenizer tok = new StringTokenizer(join, "= ");
                String next = null;
		
                while(tok.hasMoreElements()) {
                    next = tok.nextToken();
                    // Don't care about SQL keywords
                    if (next.equals("AND") || next.equals("OR") 
                        || next.equals("NOT") || next.equals("IN")) {
                        continue;
                    }
                    // Check for new tables and joins in this join clause.
                    tableJoins(next, false);
                }
            }
            else {
                // No joins for this table.
                join = "";
                mTables.put(table, join);
            }
        }    
    }

}
