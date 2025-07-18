import jadx.api.*;
import jadx.core.xmlgen.ResContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JADXHandler {
    private final JadxDecompiler jadx;

    public JADXHandler(String apkPath) {
        JadxArgs jadxArgs = new JadxArgs();
        jadxArgs.setInputFile(new File(apkPath));
        this.jadx = new JadxDecompiler(jadxArgs);
        jadx.load();
    }
    public String findPackageName() {
        String output = "";
        List<ResourceFile> resources = jadx.getResources();
        for(ResourceFile fileInResource : resources){
            if(fileInResource.getType().toString().equals("MANIFEST")){
                ResContainer content = fileInResource.loadContent();
                String code = content.getText().toString();
                int startPosition = code.indexOf("package=\"");
                code = code.substring(startPosition+9);
                int endPosition = code.indexOf("\"");
                code = code.substring(0,endPosition);
                output = code;
                break;
            }
        }
        return output;
    }


    public String stringXMLFileFinder() {
        String output = "";
        List<ResourceFile> resources = jadx.getResources();
        for(ResourceFile fileInResource : resources) {

            if(fileInResource.getOriginalName().contains("resources.arsc")){
                // System.out.println("Yaho");
                ResContainer allFiles = fileInResource.loadContent();
                for(ResContainer stringXML : allFiles.getSubFiles()){
                    if(stringXML.getFileName().contains("strings.xml")){
                        output = stringXML.getText().toString();
                    }
                }
            }
        }
        return output;
    }

    public String findElementID(String input){
        String output = "";
        int ID = 0;
        try {
            ID = Integer.parseInt(input);
        }
        catch (Exception e ){

        }
        //System.out.println(ID);
        String hexNumber = Integer.toHexString(ID);
        //System.out.println(hexNumber);
        String code = findIDinRClass();
        //System.out.println(code);
        String[] codeArray  = code.split("\n");
        for(String lineOfCode : codeArray){
            if(lineOfCode.contains(hexNumber)){
                int first = lineOfCode.indexOf("public static final int ");
                int last =  lineOfCode.indexOf(" =");
                output =  lineOfCode.substring(first+24,last);
            }
        }
        return output;
    }

    /**
     This method find IDs in R class.
     */
    public String findIDinRClass(){
        String output = "";
        List<JavaClass> classes = jadx.getClasses();
        String packageName = this.findPackageName();
        String name = packageName + ".R";
        for (JavaClass cls : classes) {
            if (cls.getFullName().equals(name)) {
                String code = cls.getCode();
                int startPosition = code.indexOf("static final class id {");
                code = code.substring(startPosition);
                int endPosition = code.indexOf("}");
                code = code.substring(0,endPosition+1);
                output = code;
                break;
            }
        }
        return output;
    }

    public String layoutContentWithElement(String activityName, String findElementID){
        String output ="";
        List<ResourceFile> resources = jadx.getResources();
        String allLayoutContents  = this.getLayoutContent(activityName);
        String[] codeArray = allLayoutContents.split("\n");
        for (String line : codeArray){
            if (line.contains(findElementID)){
                output = line;
                break;
            }
        }
        if(output.isEmpty()){
            String element =  "android:id=\"@+id/" +  findElementID;
            String element1 =  "android:id=\"@android:id/" +  findElementID;

            for(ResourceFile fileInResource : resources) {
                if (fileInResource.getOriginalName().contains(".xml")) {
                    try {
                        ResContainer content = fileInResource.loadContent();
                        String[] array = content.getText().toString().split("\n");
                        for (String line : array) {
                            if(line.contains(element) || line.contains(element1) ){
                                output = line;
                                break;
                            }
                        }
                    } catch (RuntimeException e) {
                    }
                }
            }
        }
        return  output;
    }

    //////////////////////////////////////////////////////

    public String findActivityBody(String activityName){
        String output = "";
        List<JavaClass> classes = jadx.getClasses();
        for (JavaClass cls : classes) {
            if (cls.getFullName().contains(activityName)) {
                output = cls.getCode();
                break;
            }
        }
        return output;
    }

    public String findExtension(String activityName){
        String extension = "EMPTY";
        List<JavaClass> classes = jadx.getClasses();
        for (JavaClass cls : classes) {
            if (cls.getFullName().contains(activityName)) {
                String[] codeList = cls.getCode().split("\n");
                for(String line : codeList){
                    if(line.contains("public class ")){
                        if(line.contains("extends")){
                            line  =  line.substring(line.indexOf("extends") +8);
                            if(line.contains("implements")){
                                line = line.substring(0,line.indexOf("implements")-1);
                            }
                            else {
                                line = line.substring(0,line.indexOf("{")-1);
                            }
                            for (String line1 : codeList){
                                if(line1.contains("import ")& line1.contains(line)){
                                    line1 = line1.substring(line1.indexOf("import ")+7);
                                    line1 = line1.substring(0,line1.length()-1);
                                    return line1;
                                }
                            }
                        }
                        return extension;
                    }
                }
            }
        }
        return extension;
    }

    public List<String> getElementsInXMLContent(String xmlFile){
        List<String> output = new ArrayList<String>();
        String[] lines =  xmlFile.split("\n");
        for ( String line : lines){
            if(line.contains("<fragment") || line.contains("<SearchView") || line.contains("<Button") ||
                    line.contains("<FloatingActionButton") ||  line.contains("<Switch") || line.contains("<TextView")
                    || line.contains("<ImageView") || line.contains("<EditText") || line.contains("<CheckBox")
                    || line.contains("<SeekBar")  || line.contains("<RadioButton") || line.contains("<ads") || line.contains("<adSize")  ){
                output.add(line);
            }
        }
        return output;
    }

    /**
     *Maybe there is more than one layout for an activity this method find body od all of them and merge them together.
     */
    public String getLayoutContent(String activityName){
        List<String> layouts = new ArrayList<String>();
        String output = "" ;
        List<ResourceFile> resources = jadx.getResources();
        layouts = layoutNameFinder(activityName);
        if(!layouts.isEmpty()){
            for(String layout : layouts){
                if(!layout.equals("")){
                    for(ResourceFile fileInResource : resources){
                        if(fileInResource.getOriginalName().contains(layout) {
                            try{
                                ResContainer content = fileInResource.loadContent();
                                output = output + content.getText().toString() + "\n";
                            }
                            catch (RuntimeException e){
                            }
                            break;
                        }
                    }
                }
            }
        }
        return  output;
    }
    public List<String> layoutNameFinder(String name){
        List<String> outPut = new ArrayList<String>();
        String originalBodyJava = "";
        List<JavaClass> classes = jadx.getClasses();
        for ( JavaClass ctx : classes){
            if(ctx.getFullName().toString().equals(name)){
                String code = ctx.getCode().toString();
                originalBodyJava = code;
                while(true){
                    int startPosition = code.indexOf("setContentView(R.layout");
                    if(startPosition != -1){
                        code = code.substring(startPosition+24);
                        int endPosition = code.indexOf(")");
                        if(endPosition != -1){
                            outPut.add(code.substring(0,endPosition));
                        }
                    }
                    else {
                        break;
                    }
                }
                break;
            }
        }
        while(true){
            int startPosition = originalBodyJava.indexOf("R.layout.");
            if(startPosition != -1){
                originalBodyJava = originalBodyJava.substring(startPosition+9);
                for(int i = 0 ; i < originalBodyJava.length() ; i++){
                    if(originalBodyJava.charAt(i) == ',' || originalBodyJava.charAt(i) == ')' || originalBodyJava.charAt(i) == ' '){
                        String newLayout = originalBodyJava.substring(0,i);
                        if(!outPut.contains(newLayout)){
                            outPut.add(newLayout);
                        }
                        break;
                    }
                }

            }
            else {
                break;
            }
        }
        return outPut;
    }
    public List<String> findAllActivities() {

        List<String> output = new ArrayList<String>();
        List<ResourceFile> resources = jadx.getResources();
        for(ResourceFile fileInResource : resources){
            if(fileInResource.getType().toString().equals("MANIFEST")){
                ResContainer content = fileInResource.loadContent();
                String code = content.getText().toString();
                String[] codeArray  = code.split("\n");
                for(String line : codeArray){
                    if(line.contains("<activity")){
                        String temp = line.substring(line.indexOf("android:name=\"")+14);
                        output.add(temp.substring(0, temp.indexOf("\"")));
                    }
                }

            }
        }
        return output;
    }
}