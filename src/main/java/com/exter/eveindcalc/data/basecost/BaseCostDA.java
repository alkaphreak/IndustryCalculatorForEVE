package com.exter.eveindcalc.data.basecost;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.exter.cache.Cache;
import com.exter.cache.InfiniteCache;
import com.exter.eveindcalc.EICApplication;
import com.exter.eveindcalc.data.EveDatabase;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.math.BigDecimal;

import exter.eveindustry.data.inventory.IItem;

public class BaseCostDA
{
  static private class CacheMissListener implements Cache.IMissListener<Integer, BigDecimal>
  {
    @Override
    public BigDecimal onCacheMiss(Integer key)
    {
      SQLiteDatabase db = EveDatabase.getDatabase();
      Cursor c = db.rawQuery("SELECT cost FROM base_cost WHERE id=" +String.valueOf(key),null);
      if(c.getCount() != 1)
      {
        c.close();
        return BigDecimal.ZERO;
      }
      c.moveToNext();
      BigDecimal bc = new BigDecimal(c.getString(0));
      c.close();
      return bc;
    }
  }

  static private long expire = -1;

  static private final Cache<Integer, BigDecimal> cache = new InfiniteCache<>(new CacheMissListener());

  static public boolean isExpired()
  {
    synchronized(cache)
    {
      if(expire < 0)
      {
        SharedPreferences sp = EICApplication.getContext().getSharedPreferences("EIC", Context.MODE_PRIVATE);
        expire = sp.getLong("basecost.exipire", 0);
      }

      long time = (System.currentTimeMillis() / 1000);
      
      Log.i("BaseCostDA", "Time: " + time + " Expire: " + expire);
      
      return time > expire;
    }
  }

  static private void setExpire(long exp)
  {
    synchronized(cache)
    {
      expire = exp;
      SharedPreferences sp = EICApplication.getContext().getSharedPreferences("EIC", Context.MODE_PRIVATE);
      SharedPreferences.Editor ed = sp.edit();
      ed.putLong("basecost.exipire", exp);
      ed.apply();
    }
  }
  
  static public void update(JsonReader reader)
  {
    SQLiteDatabase db = EveDatabase.getDatabase();
    try
    {
      reader.beginObject();
      while(reader.hasNext())
      {
        String name = reader.nextName();
        if(name.equals("items"))
        {
          reader.beginArray();
          while(reader.hasNext())
          {
            int id = -1;
            double cost = -1;
            reader.beginObject();
            while(reader.hasNext())
            {
              String val = reader.nextName();
              switch (val)
              {
                case "type":
                  reader.beginObject();
                  while (reader.hasNext())
                  {
                    String tval = reader.nextName();
                    if (tval.equals("id"))
                    {
                      id = reader.nextInt();
                    } else
                    {
                      reader.skipValue();
                    }
                  }
                  reader.endObject();
                  break;
                case "adjustedPrice":
                  cost = reader.nextDouble();
                  break;
                default:
                  reader.skipValue();
                  break;
              }
            }
            reader.endObject();
            if(id > 0 && cost >= 0)
            {
              //Log.i("BaseCostDA","Added base cost " + id + ": " + cost);
              db.execSQL("insert or replace into base_cost (id,cost) values ("
                  + String.valueOf(id) + ","
                  + String.valueOf(cost) + ");");
              synchronized(cache)
              {
                cache.flush(id);
              }
            }
          }
          reader.endArray();
        } else
        {
          reader.skipValue();
        }
      }
    } catch(IOException e)
    {
      retryUpdate(30);
    } catch(IllegalStateException e)
    {
      retryUpdate(30 * 60);
    } finally
    {
      setExpire((System.currentTimeMillis() / 1000) + 24 * 60 * 60);
    }
  }

  static public void retryUpdate(long time)
  {
    setExpire((System.currentTimeMillis() / 1000) + time);
  }

  static public BigDecimal getCost(IItem item)
  {
    synchronized(cache)
    {
      return cache.get(item.getID());
    }
  }
}
