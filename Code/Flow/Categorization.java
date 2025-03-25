import PIJournalPaper.View;
import soot.*;
import soot.jimple.AnyNewExpr;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.solver.cfg.InfoflowCFG;
import soot.util.Chain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Categorization {
    DataFlowResult result;
    String activity;
    InfoflowCFG iCFG;
    String activityLayoutContent;
    String activityBody;
    JADXHandler JADX;
    String specialResult;
    String thirdPartyFilePath = "-";

    ArrayList<ArrayList<String>> finalCategory = new ArrayList<>();
    ArrayList<ArrayList<String>> finalCategoryItem = new ArrayList<>();
    String firstPartyName;


    Categorization(LeakFactory inputLeakFactory, JADXHandler newInstance, String thirdPartyFilePathInput, String firstPartyName){
        this.thirdPartyFilePath =  thirdPartyFilePathInput;
        this.result = inputLeakFactory.getResult();
        if(inputLeakFactory.getEnclosingComponent() != null){
            this.activity = inputLeakFactory.getEnclosingComponent().getName();
        }
        else {
            this.activity = "Empty!";
        }
        this.firstPartyName =  firstPartyName;
        this.JADX = newInstance;
        this.iCFG = inputLeakFactory.getiCFG();
        this.activityLayoutContent = newInstance.getLayoutContent(activity);
        this.activityBody = newInstance.findActivityBody(activity);
        startCategory();
        startCategoryItem();
    }

    private void startCategoryItem() {
        List<String> nonLegitWords = new ArrayList<>(Arrays.asList("advertise", "adSize", "adView", "ads", "_ad", "ad_", "facebook", "amazon", "mopub",
                "analytic", "localytics", "BannerAd" , "AdReport" , "showAd" , "NativeAd", "AdRequest" , "AdListener", "mobileads"));
        String[] viewArray  = this.activityLayoutContent.split("\n");
        for(String view : viewArray){
            int index1 = view.indexOf("android:id=\"@+id/");
            if(index1 == -1){
                continue;
            }
            if(view.contains("Layout") ){
                continue;
            }
            String subID =  view.substring(index1 +17);
            index1 = subID.indexOf("\"");
            subID = subID.substring(0,index1);
            for (String word : nonLegitWords) {
                if(view.contains("<fragment") && word.contains("MapFragment")) {
                    ArrayList<String> temp = new ArrayList<String>();
                    temp.add("Map");
                    temp.add("seagreen");
                    this.finalCategoryItem.add(temp);
                    break;
                }
                boolean check = false;
                if (view.contains(word)) {
                    if(view.contains("ads")) {
                        if (view.contains("reads") || view.contains("loads") || view.contains("pads")) {
                            check = false;
                        }
                        else {
                            check = true;
                        }
                    }
                    else{
                        check = true;
                    }
                    if (check) {
                        view = view.trim();
                        String type =  GUIcheckType(view);
                        type = type + " \nID: " +subID;
                        ArrayList<String> temp = new ArrayList<String>();
                        temp.add(type);
                        temp.add("red");
                        this.finalCategoryItem.add(temp);
                        break;
                    }
                }
            }

        }
    }

    private void startCategory(){
        String categoryOfActivity  = categoryOfActivity(this.activity);
        ArrayList<String> temp = new ArrayList<String>();
        temp.add(this.activity);
        temp.add(categoryOfActivity);
        this.finalCategory.add(temp);
        categoryOfResult(this.result);
    }

    private void categoryOfResult(DataFlowResult result) {
        List<String> methods = new ArrayList<>();
        for (Stmt statement : result.getSource().getPath()) {
            String name = this.iCFG.getMethodOf(statement).getDeclaringClass().getName();
            name =  name + "." + this.iCFG.getMethodOf(statement).getName() + "()";
            String label = labelOfMethod(this.iCFG.getMethodOf(statement).getDeclaringClass().getName(),this.iCFG.getMethodOf(statement).getName());
            if (!(methods.contains(name))) {
                ArrayList<String> temp = new ArrayList<String>();
                temp.add(this.iCFG.getMethodOf(statement).getName() + "()");
                temp.add(label);
                temp.add("Method");
                temp.add(this.iCFG.getMethodOf(statement).getDeclaringClass().getName());
                this.finalCategory.add(temp);
                methods.add(name);
            }
            if( statement.containsInvokeExpr()){
                if (!(statement.getInvokeExpr() == null)) {
                    String name1 = statement.getInvokeExpr().getMethod().getDeclaringClass().getName();
                    name1 = name1 +"."+statement.getInvokeExpr().getMethod().getName()+"()";

                    String label1 = labelOfMethod(statement.getInvokeExpr().getMethod().getDeclaringClass().getName(), statement.getInvokeExpr().getMethod().getName());
                    if (!(methods.contains(name1))) {
                        ArrayList<String> temp1 = new ArrayList<String>();
                        temp1.add(statement.getInvokeExpr().getMethod().getName()+"()");
                        temp1.add(label1);
                        temp1.add("Method");
                        temp1.add(statement.getInvokeExpr().getMethod().getDeclaringClass().getName());
                        this.finalCategory.add(temp1);
                        methods.add(name1);
                    }
                }
            }
            if(statement.toString().contains("findViewById")){
                int index = statement.toString().indexOf("findViewById(int)>(");
                String sub  = statement.toString().substring(index+19);
                String viewId = sub.substring(0,sub.indexOf(")"));
                String elementID = "";
                if(viewId.matches("\\d+")){
                    elementID = JADX.findElementID(viewId);
                }
                else{
                    SootMethod method = iCFG.getMethodOf(statement);
                    method.retrieveActiveBody();
                    if(method.hasActiveBody()){
                        Body body = method.getActiveBody();
                        for (Unit unit : body.getUnits()) {
                            if (unit instanceof AssignStmt) {
                                AssignStmt assign = (AssignStmt) unit;
                                Value leftOp = assign.getLeftOp();
                                Value rightOp = assign.getRightOp();
                                if(leftOp.toString().equals(viewId) && rightOp.toString().contains("R$id: int ")){
                                    int indexNew = rightOp.toString().indexOf("R$id: int ");
                                    String subNew  = rightOp.toString().substring(indexNew+10);
                                    elementID = subNew.substring(0,subNew.indexOf(">"));
                                }
                            }
                        }
                    }
                }
                String elementLine = JADX.layoutContentWithElement(this.activity,elementID);
                int index1 = elementLine.indexOf("android:id=\"@+id/");
                String subID =  elementLine.substring(index1 +17);
                index1 = subID.indexOf("\"");
                subID = subID.substring(0,index1);
                String color = "";
                List<String> nonLegitWords = new ArrayList<>(Arrays.asList("advertise", "adSize", "adView", "ads", "_ad", "ad_", "facebook", "amazon", "mopub",
                        "analytic", "localytics", "BannerAd" , "AdReport" , "showAd" , "NativeAd", "AdRequest" , "AdListener", "mobileads"));
                for(String nonLegit: nonLegitWords){
                    boolean check = false;
                    if(elementLine.contains(nonLegit)){
                        if(elementLine.contains("ads")) {
                            if (elementLine.contains("reads") || elementLine.contains("loads") || elementLine.contains("pads")) {
                                check = false;
                            }
                            else {
                                check = true;
                            }
                        }
                        else{
                            check = true;
                        }
                        if(check){
                            color =  "red";
                        }
                    }
                }
                if(color.isEmpty()){
                    color = "seagreen";
                }
                elementLine = elementLine.trim();
                String type =  GUIcheckType(elementLine);
                ArrayList<String> temp1 = new ArrayList<String>();
                specialResult =  type+"-"+subID;
                type = type + " \nID: " +subID;
                temp1.add(type);
                temp1.add(color);
                temp1.add("Element");
                this.finalCategory.add(temp1);

            }
        }
    }

    private String GUIcheckType(String input) {
        String Name;
        String[] splitter = input.split(" ");
        Name = splitter[0];
        if(Name.charAt(0) == '<'){
            Name = Name.substring(1);
        }
        return Name;
    }

    private String labelOfMethod(String className , String methodName) {
        String  value = "red";
        String[] splitter = firstPartyName.split("\\.");
        String firstParty = "";
        String firstPartyExtra = "";

        if (splitter.length == 2) {
            firstParty = splitter[0];
            firstPartyExtra = splitter[0];
        }
        else if(splitter.length == 3){
            firstParty = splitter[1];
            firstPartyExtra = splitter[1];
        }
        else{
            firstParty = splitter[1];
            firstPartyExtra = splitter[2];
        }
        if(firstParty.equals("com") || firstParty.equals("android")  ){
            firstParty = "NotPossibleConsiderThisCommonWord";
        }
        if(firstPartyExtra.equals("com") || firstPartyExtra.equals("android")  ){
            firstPartyExtra = "NotPossibleConsiderThisCommonWord";
        }
        List<String> nonLegitWords = new ArrayList<>();

        String first5 = "";
        String first8 = "";
        String first9 = "";
        if(className.length() > 5){
            first5 = className.substring(0,5);
        }
        if(className.length() > 8 ){
            first8 = className.substring(0,8);
        }
        if(className.length() > 9 ){
            first9 = className.substring(0,9);
        }

        if(first5.equals("java.") || first8.equals("android.") || first9.equals("androidx.")){
            return "black";
        }
        if(className.contains("dummyMainClass") || methodName.contains("dummyMainMethod") ){
            return "black";
        }

        List<String> lines =new ArrayList<>();
        try {
            lines = Files.readAllLines(Paths.get(this.thirdPartyFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        nonLegitWords.addAll(lines);
        for(String word : nonLegitWords){
            if(className.contains(word) && !(word.contains(firstParty) || word.contains(firstPartyExtra))){
                value =  "red";
            }
        }
        if(className.contains(firstParty) || className.contains(firstPartyExtra) ){
            value = "seagreen";
        }
        return value;
    }
    private String categoryOfActivity(String activity) {
        String[] splitter = firstPartyName.split("\\.");
        String firstParty = "";
        String firstPartyExtra = "";

        if (splitter.length == 2) {
            firstParty = splitter[0];
            firstPartyExtra = splitter[0];
        }
        else if(splitter.length == 3){
            firstParty = splitter[1];
            firstPartyExtra = splitter[1];
        }
        else{
            firstParty = splitter[1];
            firstPartyExtra = splitter[2];
        }
        if(firstParty.equals("com") || firstParty.equals("android")  ){
            firstParty = "NotPossibleConsiderThisCommonWord";
        }
        if(firstPartyExtra.equals("com") || firstPartyExtra.equals("android")  ){
            firstPartyExtra = "NotPossibleConsiderThisCommonWord";
        }
        String value =  "red";
        List<String> nonLegitWords = new ArrayList<>(Arrays.asList("advertisement", "adSize", "adView", ".ads.",
                "analytic", "localytics", "BannerAd" , "AdReport" , "showAd" , "NativeAd", "AdRequest" , "AdListener", "InterstitialAd",
                "RewardedAd" , "NativeAd" , "AdRequest" , "advertising" , "AdUnitId" , "LoadAd", "AdMob" , "UnityAds"));
        List<String> lines =new ArrayList<>();
        try {
            lines = Files.readAllLines(Paths.get(this.thirdPartyFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        nonLegitWords.addAll(lines);
        for(String word : nonLegitWords){
            if(activity.contains(word) && !(word.contains(firstParty) || word.contains(firstPartyExtra))){
                value = "red";
                break;
            }
        }
        if(activity.contains(firstParty) || activity.contains(firstPartyExtra) ){
            value = "seagreen";
        }
        return value;
    }

    public ArrayList<ArrayList<String>>  getFinalCategory(){
        return this.finalCategory;
    }
    public ArrayList<ArrayList<String>> getFinalCategoryItems(){
        return this.finalCategoryItem;
    }
}