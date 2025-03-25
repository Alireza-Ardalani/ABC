package org.example;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.ResourceFile;
import jadx.core.xmlgen.ResContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JADXHandler {
    private final JadxDecompiler jadx;

    public JADXHandler(String apkPath) {
        JadxArgs jadxArgs = new JadxArgs();
        jadxArgs.setInputFile(new File(apkPath));
        this.jadx = new JadxDecompiler(jadxArgs);
        jadxArgs.setShowInconsistentCode(true);
        jadxArgs.setDebugInfo(true);
        jadxArgs.setDeobfuscationOn(true);
        jadx.load();
    }
    public String findPackageName() {
        String output = "";
        List<ResourceFile> resources = jadx.getResources();
        for (ResourceFile fileInResource : resources) {
            if (fileInResource.getType().toString().equals("MANIFEST")) {
                ResContainer content = fileInResource.loadContent();
                String code = content.getText().toString();
                int startPosition = code.indexOf("package=\"");
                code = code.substring(startPosition + 9);
                int endPosition = code.indexOf("\"");
                code = code.substring(0, endPosition);
                output = code;
                break;
            }
        }
        return output;
    }
    public List<String> findAllActivities() {
        List<String> output = new ArrayList<String>();
        List<ResourceFile> resources = jadx.getResources();
        for (ResourceFile fileInResource : resources) {
            if (fileInResource.getType().toString().equals("MANIFEST")) {
                ResContainer content = fileInResource.loadContent();
                String code = content.getText().toString();
                String[] codeArray = code.split("\n");
                boolean flag = false;
                for (String line : codeArray) {
                    if(flag){
                        if(line.contains("android:name=\"")){
                            String temp = line.substring(line.indexOf("android:name=\"") + 14);
                            output.add(temp.substring(0, temp.indexOf("\"")));
                        }
                    }
                    if(line.contains(">")){
                        flag = false;
                    }
                    if (line.contains("<activity")) {
                        flag = true;
                        if(line.contains("android:name=\"")){
                            String temp = line.substring(line.indexOf("android:name=\"") + 14);
                            output.add(temp.substring(0, temp.indexOf("\"")));
                        }
                    }
                }

            }
        }
        return output;
    }
    public Set<String> findPackageNameFromActivities() {
        Set<String> packageName = new HashSet<>();
        List<String> activities = findAllActivities();
        for(String activity : activities){
            String[] splitter = activity.split("\\.");
            if(splitter.length>=3){
                packageName.add(splitter[0]+"."+splitter[1]+"."+splitter[2]);
            }
        }
        return packageName;
    }



}
