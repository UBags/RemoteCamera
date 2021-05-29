package com.costheta.text.utils;

import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

/**
 * This is essentially a wrapper around a Set<Entry<String,String>> iterator
 * that has the smarts to ignore comment entries.
 *
 * Not thread safe. Do not call remove().
 *
 * @author cbrown
 *
 */
public class LinkedPropertiesIterator
        implements Iterator<Entry<String,String>> {
    //  Set<Entry<String,String>>       entries;
    Iterator<Entry<String,String>>  iterator;
    Entry<String,String>          nextEntry = null;
    boolean                         hasNext = false;
    boolean                         skipComments = true;

    public LinkedPropertiesIterator( Set<Entry<String,String>> entries, boolean skipComments ) {
//      this.entries = entries;
        this.skipComments = skipComments;
        iterator = entries.iterator();
        while( iterator.hasNext() )
        {
            nextEntry = iterator.next();
            if( ! skipComments || ! nextEntry.getKey().startsWith( "#" ) )
            {
                hasNext = true;
                break;
            }
        }
    }

    @Override
    public boolean hasNext()
    {
        return hasNext;
    }

    @Override
    public Entry<String, String> next()
    {
        if( ! hasNext() )
            return null;

        Entry<String,String>  ret = nextEntry;

        hasNext = false;
        while( iterator.hasNext() )
        {
            nextEntry = iterator.next();
            if( ! skipComments || ! nextEntry.getKey().startsWith( "#" ) )
            {
                hasNext = true;
                break;
            }
        }

        return ret;
    }

    @Override
    public void remove()
    {
    }
}