package com.prosilion.afterimage.util;

import java.util.List;
import java.util.UUID;
import nostr.api.factory.impl.NIP01Impl;
import nostr.event.BaseTag;
import nostr.event.impl.GenericEvent;
import nostr.event.impl.TextNoteEvent;
import nostr.id.Identity;
import org.apache.commons.lang3.RandomStringUtils;

public class Factory {

  public static Identity createNewIdentity() {
    return Identity.generateRandomIdentity();
  }

  public static <T extends GenericEvent> T createVoteEvent(Identity identity, List<BaseTag> tags, String content) {
    TextNoteEvent textNoteEvent = new NIP01Impl.TextNoteEventFactory(identity, tags, content).create();
//    NIP01<NIP01Event> nip01_1 = new NIP01<>(identity);
//    EventNostr sign = nip01_1.createTextNoteEvent(tags, content).sign();
//    return sign;
    return (T) textNoteEvent;
  }

  public static <T> String lorumIpsum(Class<T> clazz) {
    return lorumIpsum(clazz, 64);
  }

  public static <T> String lorumIpsum(Class<T> clazz, int length) {
    return lorumIpsum(clazz.getSimpleName(), length);
  }

  public static <T> String lorumIpsum(String s, int length) {
    boolean useLetters = false;
    boolean useNumbers = true;
    return cullStringLength(
        String.join("-", s, generateRandomAlphaNumericString(length, useLetters, useNumbers))
        , 64);
  }

  public static String lnUrl() {
//  lnurl1dp68gurn8ghj7um5v93kketj9ehx2amn9uh8wetvdskkkmn0wahz7mrww4excup0dajx2mrv92x9xp
//  match lnUrl string length of 84
    return cullStringLength("lnurl" + generateRandomHex64String(), 84);
  }

  private static String cullStringLength(String s, int x) {
    return s.length() > x ? s.substring(0, x) : s;
  }

  private static String generateRandomAlphaNumericString(int length, boolean useLetters, boolean useNumbers) {
    return RandomStringUtils.random(length, useLetters, useNumbers);
  }

  public static String generateRandomHex64String() {
    return UUID.randomUUID().toString().concat(UUID.randomUUID().toString()).replaceAll("[^A-Za-z0-9]", "");
  }
}
