package com.exter.eveindcalc.data;

import android.util.SparseArray;

import com.exter.eveindcalc.data.exception.EveDataException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import exter.eveindustry.data.inventory.IItem;
import exter.tsl.InvalidTSLException;
import exter.tsl.TSLObject;
import exter.tsl.TSLReader;

public class Index
{
  public class Entry
  {
    public final int ID;
    public final int Group;
    
    public Entry(int i,int g)
    {
      ID = i;
      Group = g;
    }
  }
  
  public class Group
  {
    public final int ID;
    public final String Name;

    public Group(int i,String n)
    {
      ID = i;
      Name = n;
    }

  }
  
  private List<Entry> entries;
  private List<Integer> items;
  private SparseArray<Group> groups;
  private List<Group> groups_list;
  
  public Index(TSLReader reader) throws EveDataException
  {
    groups = new SparseArray<>();
    groups_list = new ArrayList<>();
    entries = new ArrayList<>();
    items = new ArrayList<>();

    TSLObject node = new TSLObject();

    try
    {
      reader.moveNext();

      if(!reader.getName().equals("index"))
      {
        throw new EveDataException();
      }
      while(true)
      {
        reader.moveNext();
        TSLReader.State type = reader.getState();
        if(type == TSLReader.State.ENDOBJECT)
        {
          break;
        } else if(type == TSLReader.State.OBJECT)
        {
          String node_name = reader.getName();
          switch (node_name)
          {
            case "item":
            {
              node.loadFromReader(reader);
              int id = node.getStringAsInt("id", -1);
              int group = node.getStringAsInt("group", -1);
              if (id < 0 || group < 0)
              {
                throw new EveDataException();
              }
              entries.add(new Entry(id, group));
              items.add(id);
              break;
            }
            case "group":
            {
              node.loadFromReader(reader);
              int id = node.getStringAsInt("id", -1);
              String name = node.getString("name", null);
              if (id < -1 || name == null)
              {
                throw new EveDataException();
              }
              Group g = new Group(id, name);
              groups.put(g.ID, g);
              groups_list.add(g);
              break;
            }
            default:
              reader.skipObject();
              break;
          }
        }
      }
    } catch(InvalidTSLException | IOException e)
    {
      throw new EveDataException();
    }
  }
  
  public Index(String group_name,Set<Integer> itemids)
  {
    groups = new SparseArray<>();
    groups_list = new ArrayList<>();
    entries = new ArrayList<>();
    items = new ArrayList<>();

    Group g = new Group(0,group_name);
    groups.put(0, g);
    groups_list.add(g);

    for(int id:itemids)
    {
      entries.add(new Entry(id, 0));
      items.add(id);
    }
  }


  public Index(String group_name,List<IItem> itemlist)
  {
    groups = new SparseArray<>();
    groups_list = new ArrayList<>();
    entries = new ArrayList<>();
    items = new ArrayList<>();

    Group g = new Group(0,group_name);
    groups.put(0, g);
    groups_list.add(g);

    for(IItem it:itemlist)
    {
      entries.add(new Entry(it.getID(), 0));
      items.add(it.getID());
    }
  }
  public List<Entry> getEntries()
  {
    return entries;
  }
  
  public List<Integer> getItems()
  {
    return items;
  }

  public List<Group> getGroups()
  {
    return groups_list;
  }

}
