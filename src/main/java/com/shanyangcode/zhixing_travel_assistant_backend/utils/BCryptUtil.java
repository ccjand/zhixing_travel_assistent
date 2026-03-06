package com.shanyangcode.zhixing_travel_assistant_backend.utils;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class BCryptUtil {


    public static String encode(String str, String salt) {
        return BCrypt.hashpw(str, salt);
    }

    public static String encode(String str) {
        return encode(str, BCrypt.gensalt());
    }

    public static boolean check(String original, String encoded) {
        return BCrypt.checkpw(original, encoded);
    }
}
