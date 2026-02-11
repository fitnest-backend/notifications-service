package az.fitnest.notifications.util;

import org.apache.commons.codec.digest.DigestUtils;

public class LsimHashUtil {
    public static String generateKey(String password, String login, 
                                     String text, String msisdn, String sender) {
        String md5OfPassword = DigestUtils.md5Hex(password);
        String concat = md5OfPassword + login + text + msisdn + sender;
        return DigestUtils.md5Hex(concat);
    }
}