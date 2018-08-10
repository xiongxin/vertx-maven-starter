package com.xiongxin.sample;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PDE {

  private static int pde(String word) {

    String[]  strings = word.split("");

    String  stringInts = Arrays.stream(strings)
      .map("ABCDEFGHIZ"::indexOf)
      .map(String::valueOf)
      .collect(Collectors.joining());

    int result = Integer.valueOf(stringInts) >> 3;

    return result;
  }

  public static void main(String[] args) {
    System.out.println("args = [" + pde("DFEE") + "]");
  }
}
