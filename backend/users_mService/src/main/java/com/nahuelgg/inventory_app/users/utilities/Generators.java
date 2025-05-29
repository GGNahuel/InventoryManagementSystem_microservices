package com.nahuelgg.inventory_app.users.utilities;

import java.security.SecureRandom;

public class Generators {
  public static String generateKey(int length) {
    SecureRandom random = new SecureRandom();
    StringBuilder sb = new StringBuilder(length);
    String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    
    for (int i = 0; i < length; i++) {
      sb.append(characters.charAt(random.nextInt(characters.length())));
    }
    
    return sb.toString();
  }
}

