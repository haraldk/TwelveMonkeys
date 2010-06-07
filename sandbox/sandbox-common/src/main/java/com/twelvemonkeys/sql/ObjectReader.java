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


import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.lang.SystemUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

/**
 * Class used for reading table data from a database through JDBC, and map 
 * the data to Java classes.
 *
 * @see ObjectMapper
 *
 * @author Harald Kuhr (haraldk@iconmedialab.no)
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-sandbox/src/main/java/com/twelvemonkeys/sql/ObjectReader.java#1 $
 *
 * @todo Use JDK logging instead of proprietary logging.
 *
 */
public class ObjectReader {

    /**
     * Main method, for testing purposes only.
     */

    public final static void main(String[] pArgs) throws SQLException {
        /*
          System.err.println("Testing only!");
	
          // Get default connection
          ObjectReader obr = new ObjectReader(DatabaseConnection.getConnection());

          com.twelvemonkeys.usedcars.DBCar car = new com.twelvemonkeys.usedcars.DBCar(new Integer(1));
          com.twelvemonkeys.usedcars.DBDealer dealer = new com.twelvemonkeys.usedcars.DBDealer("NO4537");

          System.out.println(obr.readObject(dealer));
          com.twelvemonkeys.usedcars.Dealer[] dealers = (com.twelvemonkeys.usedcars.Dealer[]) obr.readObjects(dealer);

          for (int i = 0; i < dealers.length; i++) {
          System.out.println(dealers[i]);
          }

          System.out.println("------------------------------------------------------------------------------\n"
          + "Total: " + dealers.length + " dealers in database\n");
	
          Hashtable where = new Hashtable();
	
          where.put("zipCode", "0655");
          dealers = (com.twelvemonkeys.usedcars.Dealer[]) obr.readObjects(dealer, where);

          for (int i = 0; i < dealers.length; i++) {
          System.out.println(dealers[i]);
          }

          System.out.println("------------------------------------------------------------------------------\n"
          + "Total: " + dealers.length + " dealers matching query: " 
          + where + "\n");
	

          com.twelvemonkeys.usedcars.Car[] cars = null;
          cars = (com.twelvemonkeys.usedcars.Car[]) obr.readObjects(car);

          for (int i = 0; i < cars.length; i++) {
          System.out.println(cars[i]);
          }

          System.out.println("------------------------------------------------------------------------------\n"
          + "Total: " + cars.length + " cars in database\n");

          where = new Hashtable();
          where.put("year", new Integer(1995));
          cars = (com.twelvemonkeys.usedcars.Car[]) obr.readObjects(car, where);

          for (int i = 0; i < cars.length; i++) {
          System.out.println(cars[i]);
          }

          System.out.println("------------------------------------------------------------------------------\n"
          + "Total: " + cars.length + " cars matching query: "
          + where + " \n");
	

          where = new Hashtable();
          where.put("publishers", "Bilguiden");
          cars = (com.twelvemonkeys.usedcars.Car[]) obr.readObjects(car, where);

          for (int i = 0; i < cars.length; i++) {
          System.out.println(cars[i]);
          }

          System.out.println("------------------------------------------------------------------------------\n"
          + "Total: " + cars.length + " cars matching query: " 
          + where + "\n");

          System.out.println("==============================================================================\n"	
          + getStats());
        */
    }


    protected Log mLog = null;
    protected Properties mConfig = null;

    /**
     * The connection used for all database operations executed by this 
     * ObjectReader.
     */

    Connection mConnection = null;

    /**
     * The cache for this ObjectReader.
     * Probably a source for memory leaks, as it has no size limitations.
     */

    private Hashtable mCache = new Hashtable();

    /**
     * Creates a new ObjectReader, using the given JDBC Connection. The 
     * Connection will be used for all database reads by this ObjectReader.
     *
     * @param connection A JDBC Connection
     */

    public ObjectReader(Connection pConnection) {
        mConnection = pConnection;
	
        try {
            mConfig = SystemUtil.loadProperties(getClass());
        }
        catch (FileNotFoundException fnf) {
            // Just go with defaults
        }
        catch (IOException ioe) {
            new Log(this).logError(ioe);
        }

        mLog = new Log(this, mConfig);
    }

    /**
     * Gets a string containing the stats for this ObjectReader.
     *
     * @return A string to display the stats.
     */

    public static String getStats() {
        long total = sCacheHit + sCacheMiss + sCacheUn;
        double hit = ((double) sCacheHit / (double) total) * 100.0;
        double miss = ((double) sCacheMiss / (double) total) * 100.0;
        double un = ((double) sCacheUn / (double) total) * 100.0;

        // Default locale
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance();

        return "Total: " + total + " reads. "
            + "Cache hits: " + sCacheHit + " (" + nf.format(hit) + "%), "
            + "Cache misses: " + sCacheMiss + " (" + nf.format(miss) + "%), "
            + "Unattempted: " + sCacheUn + " (" + nf.format(un) + "%) ";
    }

    /**
     * Get an array containing Objects of type objClass, with the 
     * identity values for the given class set.
     */
    
    private Object[] readIdentities(Class pObjClass, Hashtable pMapping, 
                                    Hashtable pWhere, ObjectMapper pOM) 
        throws SQLException {
        sCacheUn++;
        // Build SQL query string
        if (pWhere == null)
            pWhere = new Hashtable();
	
        String[] keys = new String[pWhere.size()];
        int i = 0;
        for (Enumeration en = pWhere.keys(); en.hasMoreElements(); i++) {
            keys[i] = (String) en.nextElement();
        }

        // Get SQL for reading identity column
        String sql = pOM.buildIdentitySQL(keys)
            + buildWhereClause(keys, pMapping);

        // Log?
        mLog.logDebug(sql + " (" + pWhere + ")");

        // Prepare statement and set values
        PreparedStatement statement = mConnection.prepareStatement(sql);	
        for (int j = 0; j < keys.length; j++) {
            Object key =  pWhere.get(keys[j]);
	    
            if (key instanceof Integer)
                statement.setInt(j + 1, ((Integer) key).intValue());
            else if (key instanceof BigDecimal)
                statement.setBigDecimal(j + 1, (BigDecimal) key);
            else
                statement.setString(j + 1, key.toString());
        }
	
        // Execute query
        ResultSet rs = null;
        try {
            rs = statement.executeQuery();
        }
        catch (SQLException e) {
            mLog.logError(sql + " (" + pWhere + ")", e);
            throw e;
        }
        Vector result = new Vector();

        // Map query to objects
        while (rs.next()) {
            Object obj = null;
	    
            try {
                obj = pObjClass.newInstance();
            }
            catch (IllegalAccessException iae) {
                iae.printStackTrace();
            }
            catch (InstantiationException ie) {
                ie.printStackTrace();
            }
	    
            // Map it
            pOM.mapColumnProperty(rs, 1, 
                                  pOM.getProperty(pOM.getPrimaryKey()), obj);
            result.addElement(obj);
        }

        // Return array of identifiers
        return result.toArray((Object[]) Array.newInstance(pObjClass, 
                                                           result.size()));
    }


    /**
     * Reads one object implementing the DatabaseReadable interface from the 
     * database.
     *
     * @param readable A DatabaseReadable object
     * @return The Object read, or null in no object is found
     */

    public Object readObject(DatabaseReadable pReadable) throws SQLException {
        return readObject(pReadable.getId(), pReadable.getClass(), 
                          pReadable.getMapping());
    }

    /**
     * Reads the object with the given id from the database, using the given
     * mapping.
     *
     * @param id An object uniquely identifying the object to read
     * @param objClass The clas 
     * @return The Object read, or null in no object is found
     */

    public Object readObject(Object pId, Class pObjClass, Hashtable pMapping)
        throws SQLException {
        return readObject(pId, pObjClass, pMapping, null);
    }

    /**
     * Reads all the objects of the given type  from the 
     * database. The object must implement the DatabaseReadable interface.
     *
     * @return An array of Objects, or an zero-length array if none was found
     */

    public Object[] readObjects(DatabaseReadable pReadable) 
        throws SQLException {
	    return readObjects(pReadable.getClass(), 
                           pReadable.getMapping(), null);
    }

    /**
     * Sets the property value to an object using reflection
     *
     * @param obj The object to get a property from
     * @param property The name of the property
     * @param value The property value
     *
     */

    private void setPropertyValue(Object pObj, String pProperty, 
                                  Object pValue) {
	
        Method m = null;
        Class[] cl = {pValue.getClass()};
	
        try {
            //Util.setPropertyValue(pObj, pProperty, pValue);	   
	    
            // Find method
            m = pObj.getClass().
                getMethod("set" + StringUtil.capitalize(pProperty), cl);
            // Invoke it
            Object[] args = {pValue};
            m.invoke(pObj, args);
	    
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }		
        catch (IllegalAccessException iae) {
            iae.printStackTrace();
        }
        catch (InvocationTargetException ite) {
            ite.printStackTrace();
        }

    }

    /**
     * Gets the property value from an object using reflection
     *
     * @param obj The object to get a property from
     * @param property The name of the property
     *
     * @return The property value as an Object
     */

    private Object getPropertyValue(Object pObj, String pProperty) {
	
        Method m = null;
        Class[] cl = new Class[0];
	
        try {
            //return Util.getPropertyValue(pObj, pProperty);
	    
            // Find method
            m = pObj.getClass().
                getMethod("get" + StringUtil.capitalize(pProperty), 
                          new Class[0]);
            // Invoke it
            Object result = m.invoke(pObj, new Object[0]);	    
            return result;
	    
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException iae) {
            iae.printStackTrace();
        }
        catch (InvocationTargetException ite) {
            ite.printStackTrace();
        }
        return null;
    }

    /**
     * Reads and sets the child properties of the given parent object.
     *
     * @param parent The object to set the child obects to.
     * @param om The ObjectMapper of the parent object.
     */

    private void setChildObjects(Object pParent, ObjectMapper pOM)
        throws SQLException {
        if (pOM == null) {
            throw new NullPointerException("ObjectMapper in readChildObjects "
                                           + "cannot be null!!");
        }

        for (Enumeration keys = pOM.mMapTypes.keys(); keys.hasMoreElements();) {
            String property = (String) keys.nextElement();
            String mapType = (String) pOM.mMapTypes.get(property);
	    
            if (property.length() <= 0 || mapType == null) {
                continue;
            }
	    
            // Get the id of the parent
            Object id = getPropertyValue(pParent, 
                                         pOM.getProperty(pOM.getPrimaryKey()));

            if (mapType.equals(ObjectMapper.OBJECTMAP)) {
                // OBJECT Mapping		

                // Get the class for this property
                Class objectClass = (Class) pOM.mClasses.get(property);
		
                DatabaseReadable dbr = null;
                try {
                    dbr = (DatabaseReadable) objectClass.newInstance();
                }
                catch (Exception e) {
                    mLog.logError(e);
                }
		
                /*
                  Properties mapping = readMapping(objectClass);
                */
		    
                // Get property mapping for child object
                if (pOM.mJoins.containsKey(property))
                    // mapping.setProperty(".join", (String) pOM.joins.get(property));
                    dbr.getMapping().put(".join", pOM.mJoins.get(property));
		
                // Find id and put in where hash
                Hashtable where = new Hashtable();

                // String foreignKey = mapping.getProperty(".foreignKey");
                String foreignKey = (String) 
                    dbr.getMapping().get(".foreignKey");

                if (foreignKey != null) {
                    where.put(".foreignKey", id);
                }

                Object[] child = readObjects(dbr, where);
                // Object[] child = readObjects(objectClass, mapping, where);

                if (child.length < 1)
                    throw new SQLException("No child object with foreign key "
                                           + foreignKey + "=" + id);
                else if (child.length != 1)
                    throw new SQLException("More than one object with foreign "
                                           + "key " + foreignKey + "=" + id);

                // Set child object to the parent
                setPropertyValue(pParent, property, child[0]);
            }
            else if (mapType.equals(ObjectMapper.COLLECTIONMAP)) {
                // COLLECTION Mapping

                // Get property mapping for child object
                Hashtable mapping = pOM.getPropertyMapping(property);

                // Find id and put in where hash
                Hashtable where = new Hashtable();
                String foreignKey = (String) mapping.get(".foreignKey");
                if (foreignKey != null) {
                    where.put(".foreignKey", id);
                }
		
                DBObject dbr = new DBObject();
                dbr.mapping = mapping; // ugh...
                // Read the objects
                Object[] objs = readObjects(dbr, where);

                // Put the objects in a hash
                Hashtable children = new Hashtable();
                for (int i = 0; i < objs.length; i++) {
                    children.put(((DBObject) objs[i]).getId(), 
                                 ((DBObject) objs[i]).getObject());
                }
		
                // Set child properties to parent object
                setPropertyValue(pParent, property, children);
            }
        }
    }

    /**
     * Reads all objects from the database, using the given mapping.
     *
     * @param objClass The class of the objects to read
     * @param mapping The hashtable containing the object mapping
     *
     * @return An array of Objects, or an zero-length array if none was found
     */

    public Object[] readObjects(Class pObjClass, Hashtable pMapping)
        throws SQLException {
        return readObjects(pObjClass, pMapping, null);
    }

    /**
     * Builds extra SQL WHERE clause
     *
     * @param keys An array of ID names
     * @param mapping The hashtable containing the object mapping
     *
     * @return A string containing valid SQL
     */

    private String buildWhereClause(String[] pKeys, Hashtable pMapping) {
        StringBuilder sqlBuf = new StringBuilder();

        for (int i = 0; i < pKeys.length; i++) {
            String column = (String) pMapping.get(pKeys[i]);
            sqlBuf.append(" AND ");
            sqlBuf.append(column);
            sqlBuf.append(" = ?");
        }

        return sqlBuf.toString();

    }

    private String buildIdInClause(Object[] pIds, Hashtable pMapping) {
        StringBuilder sqlBuf = new StringBuilder();

        if (pIds != null && pIds.length > 0) {
            sqlBuf.append(" AND ");
            sqlBuf.append(pMapping.get(".primaryKey"));
            sqlBuf.append(" IN (");

            for (int i = 0; i < pIds.length; i++) {
                sqlBuf.append(pIds[i]); // SETTE INN '?' ???
                sqlBuf.append(", ");
            }
            sqlBuf.append(")");
        }

        return sqlBuf.toString();

    }

    /**
     * Reads all objects from the database, using the given mapping.
     *
     * @param readable A DatabaseReadable object
     * @param mapping The hashtable containing the object mapping
     *
     * @return An array of Objects, or an zero-length array if none was found
     */    

    public Object[] readObjects(DatabaseReadable pReadable, Hashtable pWhere) 
        throws SQLException {
	    return readObjects(pReadable.getClass(),
                           pReadable.getMapping(), pWhere);
    }


    /**
     * Reads the object with the given id  from the database, using the given
     * mapping.
     * This is the most general form of readObject().
     *
     * @param id An object uniquely identifying the object to read
     * @param objClass The class of the object to read
     * @param mapping The hashtable containing the object mapping
     * @param where An hashtable containing extra criteria for the read
     *
     * @return An array of Objects, or an zero-length array if none was found
     */    

    public Object readObject(Object pId, Class pObjClass, 
                             Hashtable pMapping, Hashtable pWhere)
        throws SQLException {
        ObjectMapper om = new ObjectMapper(pObjClass, pMapping);
        return readObject0(pId, pObjClass, om, pWhere);
    }

    public Object readObjects(Object[] pIds, Class pObjClass, 
                              Hashtable pMapping, Hashtable pWhere)
        throws SQLException {
        ObjectMapper om = new ObjectMapper(pObjClass, pMapping);
        return readObjects0(pIds, pObjClass, om, pWhere);
    }

    /**
     * Reads all objects from the database, using the given mapping.
     * This is the most general form of readObjects().
     *
     * @param objClass The class of the objects to read
     * @param mapping The hashtable containing the object mapping
     * @param where An hashtable containing extra criteria for the read
     *
     * @return An array of Objects, or an zero-length array if none was found
     */    

    public Object[] readObjects(Class pObjClass, Hashtable pMapping, 
                                Hashtable pWhere) throws SQLException {
        return readObjects0(pObjClass, pMapping, pWhere);
    }

    // readObjects implementation

    private Object[] readObjects0(Class pObjClass, Hashtable pMapping, 
                                  Hashtable pWhere) throws SQLException {
        ObjectMapper om = new ObjectMapper(pObjClass, pMapping);

        Object[] ids = readIdentities(pObjClass, pMapping, pWhere, om);

        Object[] result = readObjects0(ids, pObjClass, om, pWhere); 

        return result;
    }

    private Object[] readObjects0(Object[] pIds, Class pObjClass, 
                                  ObjectMapper pOM, Hashtable pWhere) 
        throws SQLException {
        Object[] result = new Object[pIds.length];

        // Read each object from ID
        for (int i = 0; i < pIds.length; i++) {
	
            // TODO: For better cahce efficiency/performance:
            // - Read as many objects from cache as possible
            // - Read all others in ONE query, and add to cache
            /*
              sCacheUn++;
              // Build SQL query string
              if (pWhere == null)
              pWhere = new Hashtable();
	
              String[] keys = new String[pWhere.size()];
              int i = 0;
              for (Enumeration en = pWhere.keys(); en.hasMoreElements(); i++) {
              keys[i] = (String) en.nextElement();
              }

              // Get SQL for reading identity column
              String sql = pOM.buildSelectClause() + pOM.buildFromClause() + 
              + buildWhereClause(keys, pMapping) + buildIdInClause(pIds, pMapping);

              // Log?
              mLog.logDebug(sql + " (" + pWhere + ")");


              // Log?
              mLog.logDebug(sql + " (" + pWhere + ")");

              PreparedStatement statement = null;

              // Execute query, and map columns/properties
              try {
              statement = mConnection.prepareStatement(sql);
	    
              // Set keys 
              for (int j = 0; j < keys.length; j++) {
              Object value = pWhere.get(keys[j]);

              if (value instanceof Integer)
              statement.setInt(j + 1, ((Integer) value).intValue());
              else if (value instanceof BigDecimal)
              statement.setBigDecimal(j + 1, (BigDecimal) value);
              else
              statement.setString(j + 1, value.toString());
              }
              // Set ids
              for (int j = 0; j < pIds.length; j++) {
              Object id = pIds[i];

              if (id instanceof Integer)
              statement.setInt(j + 1, ((Integer) id).intValue());
              else if (id instanceof BigDecimal)
              statement.setBigDecimal(j + 1, (BigDecimal) id);
              else
              statement.setString(j + 1, id.toString());
              }

              ResultSet rs = statement.executeQuery();

              Object[] result = pOM.mapObjects(rs);

              // Set child objects and return
              for (int i = 0; i < result.length; i++) {
              // FOR THIS TO REALLY GET EFFECTIVE, WE NEED TO SET ALL
              // CHILDREN IN ONE GO!
              setChildObjects(result[i], pOM);
              mContent.put(pOM.getPrimaryKey() + "=" + pId, result[0]);

              }
              // Return result
              return result[0];

              }
            */

            Object id = getPropertyValue(result[i], 
                                         pOM.getProperty(pOM.getPrimaryKey()));

            result[i] = readObject0(id, pObjClass, pOM, null);

        }

        return result;
    }

    // readObject implementation, used for ALL database reads

    static long sCacheHit;
    static long sCacheMiss;
    static long sCacheUn;
    
    private Object readObject0(Object pId, Class pObjClass, ObjectMapper pOM, 
                               Hashtable pWhere) throws SQLException {
        if (pId == null && pWhere == null)
            throw new IllegalArgumentException("Either id or where argument" 
                                               + "must be non-null!");
	
        // First check if object exists in cache
        if (pId != null) {
            Object o = mCache.get(pOM.getPrimaryKey() + "=" + pId);
            if (o != null) {
                sCacheHit++;
                return o;
            }
            sCacheMiss++;
        }
        else {
            sCacheUn++;
        }
	
        // Create where hash
        if (pWhere == null) 
            pWhere = new Hashtable();

        // Make sure the ID is in the where hash
        if (pId != null)
            pWhere.put(pOM.getProperty(pOM.getPrimaryKey()), pId);
	
        String[] keys = new String[pWhere.size()];
        Enumeration en = pWhere.keys();
        for (int i = 0; en.hasMoreElements(); i++) {
            keys[i] = (String) en.nextElement();
        }	

        // Get SQL query string
        String sql = pOM.buildSQL() + buildWhereClause(keys, pOM.mPropertiesMap);

        // Log?
        mLog.logDebug(sql + " (" + pWhere + ")");

        PreparedStatement statement = null;

        // Execute query, and map columns/properties
        try {
            statement = mConnection.prepareStatement(sql);
	    
            for (int j = 0; j < keys.length; j++) {
                Object value = pWhere.get(keys[j]);

                if (value instanceof Integer)
                    statement.setInt(j + 1, ((Integer) value).intValue());
                else if (value instanceof BigDecimal)
                    statement.setBigDecimal(j + 1, (BigDecimal) value);
                else
                    statement.setString(j + 1, value.toString());
            }

            ResultSet rs = statement.executeQuery();

            Object[] result = pOM.mapObjects(rs);

            // Set child objects and return
            if (result.length == 1) {
                setChildObjects(result[0], pOM);
                mCache.put(pOM.getPrimaryKey() + "=" + pId, result[0]);

                // Return result
                return result[0];
            }
            // More than 1 is an error...
            else if (result.length > 1) {
                throw new SQLException("More than one object with primary key "
                                       + pOM.getPrimaryKey() + "=" 
                                       + pWhere.get(pOM.getProperty(pOM.getPrimaryKey())) + "!");
            }
        }
        catch (SQLException e) {
            mLog.logError(sql + " (" + pWhere + ")", e);
            throw e;
        }
        finally {
            try {
                statement.close();
            }
            catch (SQLException e) {
                mLog.logError(e);
            }
        }

        return null;
    }

    /**
     * Utility method for reading a property mapping from a properties-file
     *
     */
    
    public static Properties loadMapping(Class pClass) {
        try {
            return SystemUtil.loadProperties(pClass);
        }
        catch (FileNotFoundException fnf) {
            // System.err... err... 
            System.err.println("ERROR: " + fnf.getMessage());	    
        }
        catch (IOException ioe) {
            ioe.printStackTrace();    
        }
        return new Properties();	    
    }

    /**
     * @deprecated Use loadMapping(Class) instead
     * @see #loadMapping(Class)
     */

    public static Properties readMapping(Class pClass) {
        return loadMapping(pClass);
    }


}

/**
 * Utility class
 */

class DBObject implements DatabaseReadable {
    Object id;
    Object o;
    static Hashtable mapping; // WHOA, STATIC!?!?
    
    public DBObject() {
    }
    
    public void setId(Object id) {
        this.id = id;
    }
    public Object getId() {
        return id;
    }
    
    public void setObject(Object o) {
        this.o = o;
    }
    public Object getObject() {
        return o;
    }
    
    public Hashtable getMapping() {
        return mapping;
    }
}


