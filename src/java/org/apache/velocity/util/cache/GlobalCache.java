package org.apache.velocity.util.cache;

/*
 * Copyright (c) 1997-2000 The Java Apache Project.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All advertising materials mentioning features or use of this
 *    software must display the following acknowledgment:
 *    "This product includes software developed by the Java Apache
 *    Project for use in the Apache JServ servlet engine project
 *    <http://java.apache.org/>."
 *
 * 4. The names "Apache JServ", "Apache JServ Servlet Engine", "Turbine",
 *    "Apache Turbine", "Turbine Project", "Apache Turbine Project" and
 *    "Java Apache Project" must not be used to endorse or promote products
 *    derived from this software without prior written permission.
 *
 * 5. Products derived from this software may not be called "Apache JServ"
 *    nor may "Apache" nor "Apache JServ" appear in their names without
 *    prior written permission of the Java Apache Project.
 *
 * 6. Redistributions of any form whatsoever must retain the following
 *    acknowledgment:
 *    "This product includes software developed by the Java Apache
 *    Project for use in the Apache JServ servlet engine project
 *    <http://java.apache.org/>."
 *
 * THIS SOFTWARE IS PROVIDED BY THE JAVA APACHE PROJECT "AS IS" AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE JAVA APACHE PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Java Apache Group. For more information
 * on the Java Apache Project and the Apache JServ Servlet Engine project,
 * please see <http://java.apache.org/>.
 *
 */

// Java Core Classes
import java.util.Map;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

/**
 * This Service functions as a Global Cache.  A global cache is a good
 * place to store items that you may need to access often but don't
 * necessarily need (or want) to fetch from the database everytime.  A
 * good example would be a look up table of States that you store in a
 * database and use throughout your application.  Since information
 * about States doesn't change very often, you could store this
 * information in the Global Cache and decrease the overhead of
 * hitting the database everytime you need State information.
 *
 * @author <a href="mailto:mbryson@mont.mindspring.com">Dave Bryson</a>
 * @author <a href="mailto:jon@clearink.com">Jon S. Stevens</a>
 * @author <a href="mailto:jvanzyl@periapt.com">Jason van Zyl</a>
 * @version $Id: GlobalCache.java,v 1.1 2000/11/16 01:46:58 jvanzyl Exp $
 */
public class GlobalCache implements Runnable
{
    /** The cache. **/
    private Hashtable cache = null;

    /** cacheCheckFrequency (default - 5 seconds) */
    private long cacheCheckFrequency = 5000;
        //TurbineResources.getLong("cachedobject.cacheCheckFrequency", 5000);
        
    public static final int EXPIRABLE = 1;
    public static final int REFRESHABLE = 2;
    public static final int PERMANENT = 3;

    /**
     * Constructor.
     */
    public GlobalCache()
    {
    }

    /**
     * Set the frequency, in thousandths of a seconds,
     * for checking the state of objects in the cache.
     */
    public void setCacheCheckFrequency(long frequency)
    {
        this.cacheCheckFrequency = frequency;
    }

    /**
     * Called the first time the Service is used.
     */
    public void init()
    {
        try
        {
            //org.apache.turbine.util.Log.note ("TurbineGlobalCacheService not init()....starting!");
            cache = new Hashtable(20);

            // Start housekeeping thread.
            Thread housekeeping = new Thread(this);
            housekeeping.setDaemon(true);
            housekeeping.start();

            //setInit(true);
        }
        catch (Exception e)
        {
            //org.apache.turbine.util.Log.error ( "Cannot initialize TurbineGlobalCacheService!" );
            //org.apache.turbine.util.Log.error (e);
        }
    }

    /**
     * Returns an item from the cache.
     *
     * @param id The key of the stored object.
     * @return The object from the cache.
     * @exception ObjectExpiredException, when either the object is
     * not in the cache or it has expired.
     */
    //public CachedObject get(String id) throws ObjectExpiredException
    public Object get(String id) //throws ObjectExpiredException
    {
        CachedObject obj = null;
        boolean stale = false;

        obj = (CachedObject) cache.get(id);

        /*
        if (obj == null)
        {
            // Not in the cache.
            throw new ObjectExpiredException();
        }
        */

        /*
        if (obj.isStale())
        {
            // Expired.
            throw new ObjectExpiredException();
        }
        */

        if (obj instanceof RefreshableCachedObject)
        {
            // notify it that it's being accessed.
            RefreshableCachedObject rco = (RefreshableCachedObject) obj;
            rco.touch();
        }

        return obj.getContents();
    }

    /**
     * Adds an object to the cache.
     *
     * @param id The key to store the object by.
     * @param o The object to cache.
     */
    public void put(String id, CachedObject o)
    {
        // If the cache already contains the key, remove it and add
        // the fresh one.
        if ( cache.containsKey(id) )
        {
            cache.remove(id);
        }
        cache.put(id,o);
    }

    /**
     * Adds an object to the cache.
     *
     * @param id The key to store the object by.
     * @param o The object to cache.
     */
    public void put(String id, Object o)
    {
        // If the cache already contains the key, remove it and add
        // the fresh one.
        if ( cache.containsKey(id) )
        {
            cache.remove(id);
        }
        cache.put(id,new CachedObject(o));
    }

    public boolean contains(String id)
    {
        return cache.containsKey(id);
    }


    /**
     * Circle through the cache and remove stale objects.  Frequency
     * is determined by the cacheCheckFrequency property.
     */
    public void run()
    {
        while(true)
        {
            // Sleep for amount of time set in cacheCheckFrequency -
            // default = 5 seconds.
            try
            {
                Thread.sleep(cacheCheckFrequency);
            }
            catch(InterruptedException exc)
            {
            }

            clearCache();
        }
    }

    /**
     * Iterate through the cache and remove or refresh stale objects.
     */
    public void clearCache()
    {
        Vector refreshThese = new Vector(20);
        // Sync on this object so that other threads do not 
        // change the Hashtable while enumerating over it.      
        synchronized (this)
        {
            for ( Enumeration e = cache.keys(); e.hasMoreElements(); )
            {
                String key = (String) e.nextElement();
                CachedObject co = (CachedObject) cache.get(key);
                if (co instanceof RefreshableCachedObject)
                {
                    RefreshableCachedObject rco = (RefreshableCachedObject)co;
                    if (rco.isUntouched()) 
                        cache.remove(key);
                    else if (rco.isStale()) 
                        // Do refreshing outside of sync block so as not
                        // to prolong holding the lock on this object
                        refreshThese.addElement(key);
                }
                else if ( co.isStale() )
                {
                    cache.remove(key);
                }
            }
        }
        
        for ( Enumeration e = refreshThese.elements(); e.hasMoreElements(); )
        {
            String key = (String)e.nextElement();
            CachedObject co = (CachedObject)cache.get(key);
            RefreshableCachedObject rco = (RefreshableCachedObject)co;
            rco.refresh();
        }
    }
}
