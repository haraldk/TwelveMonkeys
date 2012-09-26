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


import java.lang.reflect.*;
import java.util.*;
import java.sql.SQLException;
import java.sql.Connection;

/*
  Det vi trenger er en mapping mellom
  - abstrakt navn/klasse/type/identifikator (tilsv. repository)
  - java klasse
  - selve mappingen av db kolonne/java property

  I tillegg en mapping mellom alle objektene som brukes i VM'en, og deres id'er

*/

/**
 * Under construction.
 *
 * @author Harald Kuhr (haraldk@iconmedialab.no), 
 * @version 0.5
 */
public abstract class ObjectManager {
    private ObjectReader mObjectReader = null;

    private WeakHashMap mLiveObjects = new WeakHashMap(); // object/id

    private Hashtable mTypes = new Hashtable();    // type name/java class
    private Hashtable mMappings = new Hashtable(); // type name/mapping

    /**
     * Creates an Object Manager with the default JDBC connection
     */

    public ObjectManager() {
    	this(DatabaseConnection.getConnection());
    }

    /**
     * Creates an Object Manager with the given JDBC connection
     */

    public ObjectManager(Connection pConnection) {
    	mObjectReader = new ObjectReader(pConnection);
    }


    /**
     * Gets the property/column mapping for a given type
     */

    protected Hashtable getMapping(String pType) {
    	return (Hashtable) mMappings.get(pType);
    }

    /**
     * Gets the class for a type
     *
     * @return The class for a type. If the type is not found, this method will
     * throw an excpetion, and will never return null.
     */

    protected Class getType(String pType) {
    	Class cl = (Class) mTypes.get(pType);

    	if (cl == null) {
    	    // throw new NoSuchTypeException();
    	}
    	
    	return cl;
    }

    /**
     * Gets a java object of the class for a given type.
     */

    protected Object getObject(String pType) 
    /*throws XxxException*/ {
    	// Get class
    	Class cl = getType(pType);

    	// Return the new instance (requires empty public constructor)
    	try {
    	    return cl.newInstance();
    	}
    	catch (Exception e) {
    	    // throw new XxxException(e);
    	    throw new RuntimeException(e.getMessage());
    	}
    	
    	// Can't happen
    	//return null;
    }

    /**
     * Gets a DatabaseReadable object that can be used for looking up the 
     * object properties from the database.
     */

    protected DatabaseReadable getDatabaseReadable(String pType) {
    	
    	return new DatabaseObject(getObject(pType), getMapping(pType));
    }

    /**
     * Reads the object of the given type and with the given id from the 
     * database
     */

    // interface
    public Object getObject(String pType, Object pId)
        throws SQLException {
    	
    	// Create DatabaseReadable and set id
    	DatabaseObject dbObject = (DatabaseObject) getDatabaseReadable(pType); 
    	dbObject.setId(pId);

    	// Read it
    	dbObject = (DatabaseObject) mObjectReader.readObject(dbObject);

    	// Return it
    	return dbObject.getObject();
    }
    
    /**
     * Reads the objects of the given type and with the given ids from the 
     * database
     */

    // interface
    public Object[] getObjects(String pType, Object[] pIds)
        throws SQLException {
    	
    	// Create Vector to hold the result
    	Vector result = new Vector(pIds.length);

    	// Loop through Id's and fetch one at a time (no good performance...)
    	for (int i = 0; i < pIds.length; i++) {
    	    // Create DBObject, set id and read it
    	    DatabaseObject dbObject = 
    	    	(DatabaseObject) getDatabaseReadable(pType);

    	    dbObject.setId(pIds[i]);
    	    dbObject = (DatabaseObject) mObjectReader.readObject(dbObject);

    	    // Add to result if not null
    	    if (dbObject != null) {
    	    	result.add(dbObject.getObject());
    	    }
    	}

    	// Create array of correct type, length equal to Vector
    	Class cl = getType(pType);
    	Object[] arr = (Object[]) Array.newInstance(cl, result.size());

    	// Return the vector as an array
    	return result.toArray(arr);
    }

    /**
     * Reads the objects of the given type and with the given properties from
     * the database
     */

    // interface
    public Object[] getObjects(String pType, Hashtable pWhere)
        throws SQLException {
    	return mObjectReader.readObjects(getDatabaseReadable(pType), pWhere); 
    }

    /**
     * Reads all objects of the given type from the database
     */

    // interface
    public Object[] getObjects(String pType)
        throws SQLException {
    	return mObjectReader.readObjects(getDatabaseReadable(pType)); 
    }
    
    // interface
    public Object addObject(Object pObject) {
    	// get id...

    	return pObject;
    }

    // interface
    public Object updateObject(Object pObject) {
    	// get id...

    	return pObject;
    }

    // interface
    public abstract Object deleteObject(String pType, Object pId);

    // interface
    public abstract Object deleteObject(Object pObject);

    // interface
    public abstract Object createObject(String pType, Object pId);

    // interface
    public abstract Object createObject(String pType);

}

/**
 * Utility class for reading Objects from the database
 */

class DatabaseObject implements DatabaseReadable {
    Hashtable mMapping = null;

    Object mId = null;
    Object mObject = null;
    
    public DatabaseObject(Object pObject, Hashtable pMapping) {
    	setObject(pObject);
    	setMapping(pMapping);
    }

    public Object getId() {
    	return mId;
    }
    
    public void setId(Object pId) {
    	mId = pId;
    }
    
    public void setObject(Object pObject) {
	mObject = pObject;
    }
    public Object getObject() {
	return mObject;
    }

    public void setMapping(Hashtable pMapping) {
    	mMapping = pMapping;
    }

    public Hashtable getMapping() {
    	return mMapping;
    }       
}
