package com.ekros.cp.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.Getter;

@Getter
public class Descriptor implements Serializable {
  private final boolean isDirectory;
  private final boolean isSymlink;
  private int size;
  private final Descriptor prev;
  private final Map<String, Descriptor> nameLinks;
  private final List<Integer> blockLinks;
  private final String symlink;

  public Descriptor(boolean isDirectory, boolean isSymlink, Integer blockLink, int size, Descriptor prev, String symlink){
    this.blockLinks = new ArrayList<>();
    this.isDirectory = isDirectory;
    this.isSymlink = isSymlink;
    this.size = formatSize(size);
    this.prev = prev;
    this.symlink = symlink;
    nameLinks = new HashMap<>();
    if(blockLink != null){
      blockLinks.add(blockLink);
    }
  }

  public boolean contains(String name){
    return nameLinks.containsKey(name);
  }

  public boolean contains(Descriptor descriptor){
    return nameLinks.containsValue(descriptor);
  }

  public void addDescriptor(String name, Descriptor descriptor){
    nameLinks.put(name, descriptor);
  }

  public Descriptor removeDescriptor(String name){
    return nameLinks.remove(name);
  }

  public Set<String> getDescriptorLinks(Descriptor descriptor){
    Set<String> keySet = new HashSet<>();
    for (Entry<String, Descriptor> entry : nameLinks.entrySet()) {
      if(descriptor.equals(entry.getValue())){
        keySet.add(entry.getKey());
      }
    }
    return keySet;
  }

  public Descriptor getByName(String name){
    return nameLinks.get(name);
  }

  public void setSize(int size){
    this.size = formatSize(size);
  }

  public static int formatSize(int size){
    return (int) (Block.MAX_BLOCK_SIZE*(Math.ceil(Math.abs((double) size/Block.MAX_BLOCK_SIZE))));
  }

  @Override
  public String toString() {
    return "Descriptor{" +
        "isDirectory=" + isDirectory +
        ", size=" + size +
        '}';
  }
}
