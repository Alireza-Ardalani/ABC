package qilin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class DataClass {
    public static Set<String> components = new HashSet<>();
    public static Set<String> origins = new HashSet<>();
    public static String FirstParty = "Empty";


    static {
        try {
            List<String> entryMethods = Files.readAllLines(Paths.get("EntryMethods.txt"));
            components.addAll(entryMethods);
            List<String> parties = Files.readAllLines(Paths.get("parties.txt"));
            origins.addAll(parties);

            Map<String, Integer> map = new HashMap<>();
            for (String component: components){
                String temp = component;
                temp  = temp.substring(temp.indexOf("<")+1, temp.lastIndexOf(":"));
                String[] splitter = temp.split("\\.");
                if(splitter.length >= 2){
                    String party = splitter[0] + "." +splitter[1];
                    if(!ThirdPartyList.contains(party)){
                        if(!map.containsKey(party)){
                            map.put(party,1);
                        }
                        else {
                            map.put(party, map.get(party) + 1);
                        }
                    }
                }
            }
            List<Map.Entry<String, Integer>> entryList = new ArrayList<>(map.entrySet());
            entryList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
            Map<String, Integer> map1 = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : entryList) {
                map1.put(entry.getKey(), entry.getValue());
                FirstParty = entry.getKey();
                break;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Thi make distingushed between first-party and third-party it should be good to have a list of
     * common third-party, however if we just interested in considering different bytecode, we cannot ignore it
     * and fakeEntrance.jar make parties.txt for us.
     */
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

}
