package qilin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class DataClass {
    public static Set<String> components = new HashSet<>();
    public static Set<String> byteCodeOrigins = new HashSet<>();
    public static Set<String> threads = new HashSet<>();
    public static String FirstParty = "Empty";

    public static Set<String>  ThirdPartyList = new HashSet<>(Arrays.asList(
            "android.view",
            "org.json",
            "com.google",
            "android.support",
            "com.instabug",
            "org.hibernate",
            "dagger.internal",
            "org.codehaus",
            "com.adjust",
            "okhttp3.internal",
            "com.facebook",
            "sun.misc",
            "com.bumptech",
            "io.a",
            "okhttp3.ws",
            "com.doctadre",
            "com.newrelic",
            "io.branch",
            "com.squareup",
            "com.dieam",
            "il.ac",
            "me.leolin",
            "net.sourceforge",
            "com.fasterxml",
            "com.oblador",
            "cl.json",
            "com.bugsnag",
            "retrofit.mime",
            "retrofit.client",
            "com.lwansbrough",
            "com.localytics",
            "retrofit.converter",
            "org.jetbrains",
            "com.BV",
            "com.actionbarsherlock",
            "com.github",
            "io.realm",
            "org.jcodec",
            "com.microsoft",
            "org.greenrobot",
            "com.novell",
            "org.owasp",
            "org.eclipse",
            "com.mopub",
            "com.startapp",
            "androidx.startup",
            "androidx.profileinstaller",
            "com.appbrain",
            "org.apache",
            "com.adobe",
            "com.gae",
            "com.keyes.youtube",
            "de.appplant",
            "com.mixpanel",
            "com.keyes",
            "nl.xservices",
            "com.applovin",
            "com.seattleclouds",
            "com.revmob",
            "com.yalantis",
            "io.invertase",
            "io.fabric",
            "com.amazonaws",
            "com.skyhawktracker",
            "com.testfairy",
            "com.onesignal",
            "com.blueshift",
            "com.purplebrain",
            "com.urbanairship",
            "com.theartofdev",
            "com.tjeannin",
            "com.tenjin",
            "com.crashlytics"
    ));

    public static void setThreads(Set<String> threads) {
        DataClass.threads = threads;
    }

    public static void setComponents(Set<String> components) {
        DataClass.components = components;
    }

    public static void setBytecodeOrigins(Set<String> origins) {
        DataClass.byteCodeOrigins = origins;
    }
}
