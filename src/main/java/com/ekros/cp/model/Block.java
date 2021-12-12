package com.ekros.cp.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Block implements Serializable {

  public static final transient int MAX_BLOCK_SIZE = 8;
  private boolean isUsed;
  private String data;

  public Block(){
    data = "0".repeat(MAX_BLOCK_SIZE);
    isUsed = false;
  }

  public void setUsed(boolean used) {
    isUsed = used;
    data = "0".repeat(MAX_BLOCK_SIZE);
  }
}
