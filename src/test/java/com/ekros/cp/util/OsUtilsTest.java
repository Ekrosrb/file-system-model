package com.ekros.cp.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OsUtilsTest {
  @Test
  public void testClearAndReadTest(){
    OsUtils.clear();
    assertNotNull(OsUtils.readFs());
  }
  @Test
  public void testUpdateAndReadTest(){
    OsUtils.updateFs(new byte[]{0, 1, 3, 4, 53, 1, 3, 10});
    assertNotNull(OsUtils.readFs());
  }
}