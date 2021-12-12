package com.ekros.cp.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BlockTest {

  @Test
  public void testSetUsed(){
    Block block = new Block();
    block.setUsed(true);
    assertEquals("0".repeat(Block.MAX_BLOCK_SIZE), block.getData());
  }
}