
package driver;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qilin.DataClass;
import qilin.core.PTA;
import qilin.core.PTAScene;
import qilin.core.context.ContextElement;
import qilin.core.context.ContextElements;
import qilin.core.pag.*;
import qilin.pta.PTAConfig;
import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.util.Chain;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    static String jarFilePath;
    static String inputApkPath;
    static String ptaMode;
    public static String resultFolder;
    static String apkName;

    public static void main(String[] args) throws IOException {
        long maxHeap = Runtime.getRuntime().maxMemory();
        long minHeap = Runtime.getRuntime().totalMemory();
        System.out.println("Main: Min Heap Size: " + (minHeap / (1024 * 1024)) + " MB");
        System.out.println("Main: Max Heap Size: " + (maxHeap / (1024 * 1024)) + " MB");
        inputApkPath = args[0];
        resultFolder = args[1];
        ptaMode = args[2];
        jarFilePath = args[3];
        apkName = args[4];

        androidRun();
    }

    private static void androidRun() throws IOException {
        PTA pta;
        long startTime = System.nanoTime();

        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true);
        Options.v().set_process_multiple_dex(true);
        Options.v().set_process_dir(Collections.singletonList(inputApkPath));
        Options.v().set_src_prec(Options.src_prec_apk_class_jimple); // NeverCommentThisAgain!
        Options.v().set_include_all(true);
        Options.v().set_throw_analysis(Options.throw_analysis_dalvik);
        Options.v().set_ignore_resolution_errors(true);
        List<String> excludeList = new LinkedList<String>();
        Options.v().set_exclude(excludeList);
        Options.v().set_soot_classpath(jarFilePath + ":" + inputApkPath);
        soot.Main.v().autoSetOptions();
        Scene.v().loadNecessaryClasses();
        PackManager.v().getPack("wjpp").apply();

        /**
         * Before Run PTA! We can run, if we needed cleaning the code.
         */
//        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
//            if(sootClass.isPhantom() || !sootClass.getName().startsWith("com.example.staticAnalysisTest")){
//                continue;
//            }
//            var x  =  sootClass;
//            System.out.println("--> " + sootClass.getName());
//            ensureFieldInitialization(sootClass);
//            for (SootMethod method : sootClass.getMethods()) {
//                if(method.hasActiveBody()){
//                   // processObjectAllocations(method);
//                }
//            }
//        }

        new PTAOption().parseCommandLine(new String[]{
                "-se",
                "-pta", ptaMode
        });
        pta = PTAFactory.createPTA(PTAConfig.v().getPtaConfig().ptaPattern);
        pta.run();

        long endTime = System.nanoTime();
        double elapsedTimeInSeconds = (endTime - startTime) / 1_000_000_000.0;

        PAG pag = pta.getPag();
        System.out.println();
        System.out.println("Result==>");
        System.out.println("K-CFA===> " + ptaMode);
        System.out.println("Size===> " + pag.getAlloc().size());

        String outputTxt = apkName.replace(".apk", ".txt");
        //String outputTxt = apkName.replace(".apk","_KCFA.txt");
        //String outputTxt = apkName.replace(".apk","_Component.txt");
        //String outputTxt = apkName.replace(".apk","_Bytecode.txt");
        //String outputTxt = apkName.replace(".apk","KOBJ.txt");
        //String outputTxt = apkName.replace(".apk","_KHYB.txt");
        FileWriter Fw = new FileWriter(resultFolder + "/" + outputTxt, true);
        Fw.write("K-CFA: " + ptaMode + "\n");
        Fw.write("Time: " + elapsedTimeInSeconds + "\n");
        Fw.write("AllocSize: " + pag.getAlloc().size() + "\n");
        Fw.write("------------------------------------\n");
        Fw.close();

        /**
         * After Run PTA!
         */

        //allocation(pag);
        //method(pag);
        //analyse_Component(pag);
        //analyse_Component2(pag);
        analyse_First_To_Third1(pag);
    }

    private static void analyse_First_To_Third1(PAG pag) {
        Map<ValNode, Set<ValNode>> Simples = pag.getSimple();
        double _FF = 0;
        double _FT = 0;
        double _TF = 0;
        double _TT = 0;
        for (ValNode simple : Simples.keySet()) {
            if (simple.toString().contains("Exception")) {
                continue;
            }
            Context context = null;
            if (simple instanceof ContextVarNode) {
                context = ((ContextVarNode) simple).context();
            } else if (simple instanceof ContextField) {
                context = ((ContextField) simple).getContext();
            } else {
                System.out.println("Something New");
            }
            if (context == null) {
                System.out.println("Wierd");
            }
            ContextElements CTX = (ContextElements) context;
            String origin = CTX.getOrigin();

            boolean FF = false;
            boolean FT = false;
            boolean TF = false;
            boolean TT = false;

            if (!origin.isEmpty()) {
                Set<ValNode> related = Simples.get(simple);
                for (ValNode x : related) {
                    Context context1 = null;
                    if (x instanceof ContextVarNode) {
                        context1 = ((ContextVarNode) x).context();
                    } else if (x instanceof ContextField) {
                        context1 = ((ContextField) x).getContext();
                    } else {
                        System.out.println("Something New");
                    }
                    if (context1 == null) {
                        System.out.println("Wierd");
                    }
                    ContextElements CTX1 = (ContextElements) context1;
                    String origin1 = CTX1.getOrigin();

                    if (x.toString().contains("Exception")) {
                        continue;
                    }

                    if (!origin1.isEmpty()) {
                        if(origin.equals(DataClass.FirstParty)){
                            if(origin1.equals(DataClass.FirstParty)){
                                FF = true;
//                                System.out.println(origin);
//                                System.out.println("=> " + simple);
//                                System.out.println(origin1);
//                                System.out.println("=> " + x);
//                                System.out.println("-----");
                            }
                            else {
                                FT = true;
                                System.out.println(origin);
                                System.out.println("=> " + simple);
                                System.out.println(origin1);
                                System.out.println("=> " + x);
                                System.out.println("-----");
                            }
                        }
                        else {
                            if(origin1.equals(DataClass.FirstParty)){
                                TF = true;
//                                System.out.println(origin);
//                                System.out.println("=> " + simple);
//                                System.out.println(origin1);
//                                System.out.println("=> " + x);
//                                System.out.println("-----");
                            }
                            else {
                                TT = true;
//                                System.out.println(origin);
//                                System.out.println("=> " + simple);
//                                System.out.println(origin1);
//                                System.out.println("=> " + x);
//                                System.out.println("-----");
                            }

                        }
                    }
                }
            }
            if(FF){
                _FF++;
            }
            if(FT){
                _FT++;
            }
            if(TF){
                _TF++;
            }
            if(TT){
                _TT++;
            }

        }
        System.out.println(_FF);
        System.out.println(_FT);
        System.out.println(_TT);
        System.out.println(_TF);
        System.out.println();


//        for(ValNode simple :  Simples.keySet()) {
//            if (simple.toString().contains("Exception")) {
//                continue;
//            }
//            ContextVarNode ctxVarNode = null;
//            if(simple instanceof ContextVarNode){
//                 ctxVarNode = (ContextVarNode) simple;
//            }
//            else {
//                System.out.println(simple.getClass());
//            }
//
//            ContextElements CTX = (ContextElements) ctxVarNode.context();
//            String origin = CTX.getOrigin();
//            if (!origin.isEmpty()) {
//                if (origin.contains(DataClass.FirstParty)) {
//                    Boolean FT = false;
//                    Boolean FF = false;
//                    for (ValNode use : Simples.get(simple)) {
//                        ContextVarNode ctxVarNode1 = (ContextVarNode) use;
//                        ContextElements CTX1 = (ContextElements) ctxVarNode1.context();
//                        String origin1 = CTX1.getOrigin();
//                        if (!origin1.isEmpty() && origin1.contains(DataClass.FirstParty)) {
//                            FF = true;
//                        }
//                        if (!origin1.isEmpty() && !origin1.contains(DataClass.FirstParty)) {
//                            FT = true;
//                        }
//                    }
//                    if (FF) {
//                        _FF++;
//                    }
//                    if (FT) {
//                        _FT++;
//                    }
//                } else {
//                    Boolean TF = false;
//                    Boolean TT = false;
//                    for (ValNode use : Simples.get(simple)) {
//                        ContextVarNode ctxVarNode1 = null;
//                        if(simple instanceof ContextVarNode){
//                            ctxVarNode1 = (ContextVarNode) simple;
//                        }
//                        else {
//                            System.out.println(simple.getClass());
//                        }
//                        ContextElements CTX1 = (ContextElements) ctxVarNode1.context();
//                        String origin1 = CTX1.getOrigin();
//                        if (!origin1.isEmpty() && !origin1.contains(DataClass.FirstParty)) {
//                            TT = true;
//                        }
//                        if (!origin1.isEmpty() && origin1.contains(DataClass.FirstParty)) {
//                            TF = true;
//                        }
//                    }
//                    if (TT) {
//                        _TT++;
//                    }
//                    if (TF) {
//                        _TF++;
//                    }
//
//                }
//            }
//        }
//        System.out.println(_FT / (_FT+_FF));
//        System.out.println(_TF / (_TF+_TT));
//        System.out.println();
    }

    private static void analyse_First_To_Third(PAG pag) {
        Map<AllocNode, Set<VarNode>> allocations = pag.getAlloc();
        Map<AllocNode, Set<VarNode>> allocationsFirstToThird = new HashMap<>();
        double _FF = 0;
        double _FT = 0;
        double _TF = 0;
        double _TT = 0;
        for (AllocNode alloc : allocations.keySet()) {
            if (alloc.toString().contains("Exception")) {
                continue;
            }
            ContextAllocNode allocNode = (ContextAllocNode) alloc;
            ContextElements CTX = (ContextElements) allocNode.context();
            String origin = CTX.getOrigin();
            if (!origin.isEmpty()) {
                if (origin.contains(DataClass.FirstParty)) {
                    Boolean FT = false;
                    Boolean FF = false;
                    for (VarNode use : allocations.get(alloc)) {
                        ContextElements CTX1 = (ContextElements) use.context();
                        String origin1 = CTX1.getOrigin();
                        if (!origin1.isEmpty() && origin1.contains(DataClass.FirstParty)) {
                            FF = true;
                        }
                        if (!origin1.isEmpty() && !origin1.contains(DataClass.FirstParty)) {
                            FT = true;
                        }
                    }
                    if (FF) {
                        _FF++;
                    }
                    if (FT) {
                        _FT++;
                    }
                } else {
                    Boolean TF = false;
                    Boolean TT = false;
                    for (VarNode use : allocations.get(alloc)) {
                        ContextElements CTX1 = (ContextElements) use.context();
                        String origin1 = CTX1.getOrigin();
                        if (!origin1.isEmpty() && !origin1.contains(DataClass.FirstParty)) {
                            TT = true;
                        }
                        if (!origin1.isEmpty() && origin1.contains(DataClass.FirstParty)) {
                            TF = true;
                        }
                    }
                    if (TT) {
                        _TT++;
                    }
                    if (TF) {
                        _TF++;
                    }

                }
            }
        }

        System.out.println(_FT / (_FT + _FF));
        System.out.println(_TF / (_TF + _TT));
        System.out.println();
    }

    private static void analyse_Component2(PAG pag) {
        Map<AllocNode, Set<VarNode>> allocation = pag.getAlloc();
        Map<String, Integer> mapComponent = new HashMap<>();
        Map<String, Map<String, Integer>> special = new HashMap<>();
        List<String> allocations = Arrays.asList(
                "new android.widget",
                "new android.os.Handler",
                "new android.content.IntentFilter",
                "new android.content.Intent",
                "new android.os.Bundle",
                "new android.graphics.Bitmap",
                "new okhttp3.Request",
                "new okhttp3.Response",
                "new retrofit2.Retrofit",
                "new org.json.JSONObject",
                "new java.net.URL",
                "new java.io.InputStream",
                "new java.io.OutputStream",
                "new java.io.File",
                "new java.io.FileInputStream",
                "new java.io.FileOutputStream",
                "new android.content.SharedPreferences",
                "new android.database.sqlite.SQLiteOpenHelper",
                "new androidx.room.RoomDatabase"
        );
        for (AllocNode allocNode : allocation.keySet()) {
//            if(allocNode.toString().contains("ClassConstantNode") ||
//                    allocNode.toString().contains("Exception") ||
//                    allocNode.toString().contains("StringConstantNode")){
//                continue;
//            }
//            String object = "non";
//            for(var name: allocations){
//               if(allocNode.toString().contains(name)){
//                   object = name;
//               }
//            }
//            if(object.equals("non")){
//                continue;
//            }
            ContextAllocNode allocNode1 = (ContextAllocNode) allocNode;
            ContextElements CTX = (ContextElements) allocNode1.context();
            String origin = CTX.getOrigin();
            if (!origin.isEmpty()) {
                String newOrigin = origin.substring(origin.indexOf("<") + 1, origin.indexOf(":"));
//                if(special.containsKey(newOrigin)){
//                    Map<String, Integer> temp = special.get(newOrigin);
//                    if(temp.containsKey(object)) {
//                        temp.put(object,temp.get(object)+1);
//                    } else {
//                        temp.put(object,1);
//                    }
//                    special.put(newOrigin, temp);
//                } else {
//                    Map<String, Integer> temp = new HashMap<>();
//                    temp.put(object,1);
//                    special.put(newOrigin,temp );
//                }

                if (mapComponent.containsKey(newOrigin)) {
                    mapComponent.put(newOrigin, mapComponent.get(newOrigin) + 1);
                } else {
                    mapComponent.put(newOrigin, 1);
                }
            }
        }
        //System.out.println();
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(mapComponent.entrySet());
        entryList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        Map<String, Integer> mapAllocSorted = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entryList) {
            mapAllocSorted.put(entry.getKey(), entry.getValue());
            System.out.println(entry.getKey() + " --> " + entry.getValue());
        }

    }

    private static void analyse_Component1(PAG pag) {
        Map<AllocNode, Set<VarNode>> allocation = pag.getAlloc();
        for (AllocNode all : allocation.keySet()) {
            if (all.toString().contains("ClassConstantNode") || all.toString().contains("Exception") || all.toString().contains("StringConstantNode")) {
                continue;
            }

            Set<VarNode> map = allocation.get(all);
            for (VarNode x : map) {
                Context ctx = x.context();
                ContextElements ctxElm = (ContextElements) ctx;
                if (!ctxElm.getOrigin().isEmpty()) {
                    Object variable = x.getVariable();
                    if (variable.toString().contains("Parm THIS_NODE")) {
                        Parm parm = (Parm) variable;
                        SootMethod method = parm.method();
                        for (VarNode y : map) {
                            if (!x.equals(y)) {
                                Context ctx1 = y.context();
                                ContextElements ctxElm1 = (ContextElements) ctx1;
                                if (!ctxElm1.getOrigin().isEmpty()) {
                                    Object variable1 = y.getVariable();
                                    if (variable1.toString().contains("Parm THIS_NODE")) {
                                        Parm parm1 = (Parm) variable1;
                                        SootMethod method1 = parm1.method();
                                        if (method1.getSignature().toString().equals(method.getSignature().toString())) {
                                            if (!ctxElm1.getOrigin().equals(ctxElm.getOrigin())) {
                                                System.out.println(all);

                                                System.out.println(ctxElm1.getOrigin());
                                                System.out.println(method1);

                                                System.out.println(ctxElm.getOrigin());
                                                System.out.println(method);
                                                System.out.println("-------------------");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println();

    }

    private static void analyse_Component(PAG pag) {
        Map<SootMethod, Map<Context, MethodOrMethodContext>> methods = pag.getContextMethodMap();
        System.out.println();
        Map<SootMethod, Map<Context, MethodOrMethodContext>> sortedMethods = methods.entrySet()
                .stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size())) // Sort by inner map size (descending)
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
        System.out.println();

        List<String> list = new ArrayList<>();
//        list.add("<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>");
//        list.add("<java.lang.StringBuilder: java.lang.String toString()>");
//        list.add("<java.lang.String: java.lang.String replaceAll(java.lang.String,java.lang.String)>");
//        list.add("<java.util.HashMap: java.lang.Object put(java.lang.Object,java.lang.Object)>");
//        list.add("<java.lang.String: char charAt(int)>");
//        list.add("<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.Object)>");
//        list.add("<java.lang.String: int length()>");
//         list.add("<java.lang.StringBuilder: java.lang.StringBuilder append(int)>");
//        list.add("<java.lang.String: boolean equals(java.lang.Object)>");
        list.add("<java.util.ArrayList: int size()>");

        List<Set<String>> tupleList = new ArrayList<>();
        for (SootMethod m : methods.keySet()) {
            if (list.contains(m.getSignature())) {
                Map<Context, MethodOrMethodContext> ctx = methods.get(m);
                System.out.println("---> " + ctx.size());
                Map<Context, MethodOrMethodContext> ctx1 = methods.get(m);
                for (Context c : ctx.keySet()) {
                    ContextElements ctxElm = (ContextElements) c;
                    String origin = ctxElm.getOrigin();
                    for (Context c1 : ctx1.keySet()) {
                        ContextElements ctxElm1 = (ContextElements) c1;
                        String origin1 = ctxElm1.getOrigin();
                        if (!c.equals(c1) & !origin.isEmpty() & !origin1.isEmpty() & !origin1.equals(origin)) {
                            ContextElement[] elms = ctxElm.getElements();
                            ContextElement[] elms1 = ctxElm1.getElements();
                            if (elms.length != elms1.length) {
                            } else {
                                boolean check = true;
                                for (int i = 0; i < elms.length; i++) {
                                    if (!elms[i].toString().equals(elms1[i].toString())) {
                                        check = false;
                                    }
                                }
                                if (check) {
                                    //System.out.println(origin);
                                    //System.out.println(origin1);
                                    String newOrigin = origin.substring(origin.indexOf("<") + 1, origin.indexOf(":"));
                                    String newOrigin1 = origin1.substring(origin1.indexOf("<") + 1, origin1.indexOf(":"));

                                    if (!newOrigin.equals(newOrigin1)) {
                                        Set<String> pair1 = new HashSet<>(Arrays.asList(newOrigin, newOrigin1));
                                        if (!tupleList.contains(pair1)) {
                                            tupleList.add(pair1);
                                        } else {
                                            //System.out.println(pair1 + " is already in the list");
                                        }
                                    }
                                    //System.out.println("--------------");
                                }
                            }
                        }
                    }
                }
            }
        }

        for (Set<String> r : tupleList) {
            List<String> listt = r.stream().toList();
            boolean check = false;
            if (listt.get(0).contains(DataClass.FirstParty) & listt.get(1).contains(DataClass.FirstParty)) {
                check = false;
            } else {
                check = true;
            }
            if (!check) {
                //System.out.println(r);
            } else {
                System.out.println(r);
            }

        }

    }

    private static void allocation(PAG pag) {
        Map<AllocNode, Set<VarNode>> allocations = pag.getAlloc();
        System.out.println();

        Map<String, Integer> mapAlloc = new HashMap<>();
        for (var allocation : allocations.keySet()) {
            if (!mapAlloc.containsKey(allocation.getNewExpr().toString())) {
                mapAlloc.put(allocation.getNewExpr().toString(), 1);
            } else {
                mapAlloc.put(allocation.getNewExpr().toString(), mapAlloc.get(allocation.getNewExpr().toString()) + 1);
            }
        }


        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(mapAlloc.entrySet());
        entryList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        Map<String, Integer> mapAllocSorted = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entryList) {
            mapAllocSorted.put(entry.getKey(), entry.getValue());
        }
        System.out.println();
        Map<String, Integer> mapSelected = new HashMap<>();
//        mapSelected.put("new java.lang.StringBuilder", 5);
//        mapSelected.put("new java.util.ArrayList", 2);
//        mapSelected.put("new java.util.HashMap", 8);
//        mapSelected.put("new java.util.HashSet",0);
//        mapSelected.put("new android.os.Bundle", 1);
//        mapSelected.put("new android.content.Intent", 1);
//        mapSelected.put("new java.lang.Object", 1);
//        mapSelected.put("new java.util.concurrent.ConcurrentHashMap", 1);
//        mapSelected.put("new android.content.IntentFilter", 1);
//        mapSelected.put("new android.os.Handler", 1);


        Map<String, Integer> result = new HashMap<>();
        for (AllocNode allocNode : allocations.keySet()) {
            ContextAllocNode allocNode1 = (ContextAllocNode) allocNode;
            if (mapSelected.containsKey(allocNode1.getNewExpr().toString())) {
                ContextElements CTX = (ContextElements) allocNode1.context();
                String origin = CTX.getOrigin();
                if (!origin.isEmpty()) {
                    //System.out.println(origin);
                    if (result.containsKey(origin)) {
                        result.put(origin, result.get(origin) + 1);
                    } else {
                        result.put(origin, 1);
                    }
                } else {

                }
            }
        }

        for (String r : result.keySet()) {
            System.out.println(r + " -> " + result.get(r));
        }

        System.out.println();

    }

    private static void method(PAG pag) {
        Map<SootMethod, Map<Context, MethodOrMethodContext>> methods = pag.getContextMethodMap();
        System.out.println();
        Map<SootMethod, Map<Context, MethodOrMethodContext>> sortedMethods = methods.entrySet()
                .stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size())) // Sort by inner map size (descending)
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
        System.out.println();

        List<String> list = new ArrayList<>();
//        list.add("<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>");
//        list.add("<java.lang.StringBuilder: java.lang.String toString()>");
//        list.add("<java.lang.String: java.lang.String replaceAll(java.lang.String,java.lang.String)>");
//        list.add("<java.util.HashMap: java.lang.Object put(java.lang.Object,java.lang.Object)>");
//        list.add("<java.lang.String: char charAt(int)>");
//        list.add("<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.Object)>");
//        list.add("<java.lang.String: int length()>");
//         list.add("<java.lang.StringBuilder: java.lang.StringBuilder append(int)>");
//        list.add("<java.lang.String: boolean equals(java.lang.Object)>");
//        list.add("<java.util.ArrayList: int size()>");

        Map<String, Integer> result = new HashMap<>();
        for (SootMethod m : methods.keySet()) {
            if (list.contains(m.getSignature())) {
                Map<Context, MethodOrMethodContext> ctx = methods.get(m);
                for (Context c : ctx.keySet()) {
                    ContextElements ctxElm = (ContextElements) c;
                    String origin = ctxElm.getOrigin();
                    if (!origin.isEmpty()) {
                        if (result.containsKey(origin)) {
                            result.put(origin, result.get(origin) + 1);
                        } else {
                            result.put(origin, 1);
                        }
                    } else {

                    }
                }
            }
        }

        for (String r : result.keySet()) {
            System.out.println(r + " -> " + result.get(r));
        }

    }


    public static void Refactor1(SootMethod sootMethod, String signature) {

        sootMethod.retrieveActiveBody();
        Body body = sootMethod.getActiveBody();
        PatchingChain<Unit> units = body.getUnits();

        List<Unit> unitList = new ArrayList<>(units);
        for (Unit unit : unitList) {
            if (unit instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) unit;

                if (assignStmt.getRightOp() instanceof VirtualInvokeExpr) {
                    VirtualInvokeExpr invokeExpr = (VirtualInvokeExpr) assignStmt.getRightOp();
                    SootMethod invokedMethod = invokeExpr.getMethod();

                    if (invokedMethod.getSignature().contains(signature)) {
                        System.out.println("Found statement: " + assignStmt);

                        Local lhs = (Local) assignStmt.getLeftOp();

                        RefType locationType = RefType.v("android.location.Location");
                        AssignStmt fakeLocationStmt = Jimple.v().newAssignStmt(
                                lhs, Jimple.v().newNewExpr(locationType)
                        );

                        units.swapWith(assignStmt, fakeLocationStmt);
                    }
                }
            }
        }
    }

    public static void refactorDouble(SootMethod sootMethod) {
        sootMethod.retrieveActiveBody();
        Body body = sootMethod.getActiveBody();
        PatchingChain<Unit> units = body.getUnits();

        List<Unit> unitList = new ArrayList<>(units);
        for (Unit unit : unitList) {
            if (unit instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) unit;

                if (assignStmt.getRightOp() instanceof VirtualInvokeExpr) {
                    VirtualInvokeExpr invokeExpr = (VirtualInvokeExpr) assignStmt.getRightOp();
                    SootMethod invokedMethod = invokeExpr.getMethod();

                    if (invokedMethod.getSignature().equals("<android.location.Location: double getLatitude()>")) {
                        System.out.println("Found statement: " + assignStmt);

                        Local lhs = (Local) assignStmt.getLeftOp();

                        RefType doubleType = RefType.v("java.lang.Double");
                        AssignStmt fakeDoubleStmt = Jimple.v().newAssignStmt(
                                lhs, Jimple.v().newNewExpr(doubleType)
                        );

                        units.swapWith(assignStmt, fakeDoubleStmt);
                    }
                }
            }
        }
    }

    public static void refactorDouble1(SootMethod sootMethod) {
        sootMethod.retrieveActiveBody();
        Body body = sootMethod.getActiveBody();
        PatchingChain<Unit> units = body.getUnits();

        List<Unit> unitList = new ArrayList<>(units);
        for (Unit unit : unitList) {
            if (unit instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) unit;

                if (assignStmt.getRightOp() instanceof VirtualInvokeExpr) {
                    VirtualInvokeExpr invokeExpr = (VirtualInvokeExpr) assignStmt.getRightOp();
                    SootMethod invokedMethod = invokeExpr.getMethod();

                    if (invokedMethod.getSignature().equals("<android.location.Location: double getLatitude()>")) {
                        System.out.println("Found statement: " + assignStmt);

                        Local lhs = (Local) assignStmt.getLeftOp();

                        RefType doubleType = RefType.v("java.lang.Double");
                        Local doubleObj = Jimple.v().newLocal("doubleObj", doubleType);
                        body.getLocals().add(doubleObj);

                        AssignStmt fakeDoubleStmt = Jimple.v().newAssignStmt(
                                doubleObj, Jimple.v().newNewExpr(doubleType)
                        );

                        SootMethod toStringMethod = Scene.v().getMethod("<java.lang.Double: java.lang.String toString()>");
                        VirtualInvokeExpr toStringInvoke = Jimple.v().newVirtualInvokeExpr(doubleObj, toStringMethod.makeRef());
                        AssignStmt toStringStmt = Jimple.v().newAssignStmt(lhs, toStringInvoke);

                        units.swapWith(assignStmt, fakeDoubleStmt);
                        units.insertAfter(toStringStmt, fakeDoubleStmt);
                    }
                }
            }
        }
    }

    private static void ensureFieldInitialization(SootClass sootClass) {
        SootMethod constructor = null;
        for (SootMethod m : sootClass.getMethods()) {
            if (m.toString().contains("<init>")) {
                constructor = m;
                break;
            }
        }
        if (constructor == null) {
            System.out.println("[WARNING] No constructor found for class: " + sootClass.getName());
            return;
        }
        constructor.retrieveActiveBody();
        if (!constructor.hasActiveBody()) {
            System.out.println("[WARNING] No Active Body  for Class <init>: " + sootClass.getName());
            return;
        }


        Body body = constructor.retrieveActiveBody();
        PatchingChain<Unit> units = body.getUnits();

        Set<SootField> initializedFields = new HashSet<>();
        for (Unit unit : units) {
            if (unit instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) unit;
                if (assignStmt.getRightOp() instanceof NewExpr &&
                        assignStmt.getLeftOp() instanceof InstanceFieldRef) {
                    SootField assignedField = ((InstanceFieldRef) assignStmt.getLeftOp()).getField();
                    initializedFields.add(assignedField);
                }
            }
        }
        for (SootField field : sootClass.getFields()) {
            if (initializedFields.contains(field)) {
                continue;
            }

            Type fieldType = field.getType();

            if (fieldType instanceof RefType) {
                String className = ((RefType) fieldType).getClassName();

                if (className.equals("java.lang.String") ||
                        className.equals("java.lang.Integer") ||
                        className.equals("java.lang.Double") ||
                        className.startsWith("android")) { // Covers List, Map, Set

                    System.out.println("[INFO] Initializing missing field: " + field.getSignature());

                    Local tmpVar = Jimple.v().newLocal("tmp_" + field.getName(), (RefType) fieldType);
                    body.getLocals().add(tmpVar);

                    AssignStmt newAssign = Jimple.v().newAssignStmt(tmpVar, Jimple.v().newNewExpr((RefType) fieldType));
                    units.insertBefore(newAssign, units.getFirst());

                    AssignStmt fieldAssign;
                    if (field.isStatic()) {
                        StaticFieldRef staticFieldRef = Jimple.v().newStaticFieldRef(field.makeRef());
                        fieldAssign = Jimple.v().newAssignStmt(staticFieldRef, tmpVar);
                    } else {
                        InstanceFieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(body.getThisLocal(), field.makeRef());
                        fieldAssign = Jimple.v().newAssignStmt(instanceFieldRef, tmpVar);
                    }

                    units.insertAfter(fieldAssign, newAssign);

                    System.out.println("[FIXED] Inserted missing field initialization: " + field.getSignature());
                }
            }
        }
    }


    private static void processObjectAllocations(SootMethod method) {
        if (!method.hasActiveBody()) return;
        Body body = method.getActiveBody();
        PatchingChain<Unit> units = body.getUnits();

        SootClass declaringClass = method.getDeclaringClass();
        Local newThisInstance = Jimple.v().newLocal("newThisObj", RefType.v(declaringClass));
        body.getLocals().add(newThisInstance);

        List<Unit> toReplace = new ArrayList<>();

        for (Unit unit : new ArrayList<>(units)) {
            if (unit instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) unit;
                Value leftOp = assignStmt.getLeftOp();
                Value rightOp = assignStmt.getRightOp();

                if (leftOp.getType() instanceof RefType && !(rightOp instanceof NewExpr)) {
                    String className = ((RefType) leftOp.getType()).getClassName();
                    Value newExpr = null;

                    if (className.equals("java.lang.String")) {
                        newExpr = Jimple.v().newNewExpr(RefType.v("java.lang.String"));
                    } else if (className.equals("java.util.List")) {
                        newExpr = Jimple.v().newNewExpr(RefType.v("java.util.ArrayList"));
                    } else if (className.equals("java.util.Map")) {
                        newExpr = Jimple.v().newNewExpr(RefType.v("java.util.HashMap"));
                    } else if (className.equals("java.util.Set")) {
                        newExpr = Jimple.v().newNewExpr(RefType.v("java.util.HashSet"));
                    } else if (className.startsWith("android.")) {
                        newExpr = Jimple.v().newNewExpr((RefType) leftOp.getType());
                    }

                    if (newExpr != null) {
                        AssignStmt newAssignStmt = Jimple.v().newAssignStmt(leftOp, newExpr);
                        units.swapWith(assignStmt, newAssignStmt);
                        System.out.println("[FIXED] Replaced missing `new` for " + className);
                    }
                }
            }
            if (unit instanceof InvokeStmt) {
                InvokeStmt invokeStmt = (InvokeStmt) unit;
                InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
                List<Value> args = invokeExpr.getArgs();

                for (int i = 0; i < args.size(); i++) {
                    if (args.get(i) instanceof ThisRef) {
                        AssignStmt newAssign = Jimple.v().newAssignStmt(newThisInstance, Jimple.v().newNewExpr(RefType.v(declaringClass)));
                        units.insertBefore(newAssign, unit);

                        args.set(i, newThisInstance);
                        InvokeStmt newInvoke = Jimple.v().newInvokeStmt(invokeExpr);
                        toReplace.add(unit);
                        units.insertBefore(newInvoke, unit);
                        break;
                    }
                }
            }
        }
        for (Unit oldUnit : toReplace) {
            units.remove(oldUnit);
        }

        System.out.println("[FIXED] Processed method: " + method.getSignature());
    }


    public static void EmpowerDouble(SootMethod method) {
        if (method.isPhantom() || !method.hasActiveBody()) {
            return;
        }
        String className = method.getDeclaringClass().getName();
        if (className.startsWith("java.") || className.startsWith("javax.") ||
                className.startsWith("android.") || className.startsWith("androidx.")) {
            return;
        }

        Body body = method.getActiveBody();
        PatchingChain<Unit> units = body.getUnits(); // Use PatchingChain to modify safely
        List<Unit> unitList = new ArrayList<>(units); // Copy to avoid modification issues

        if (method.getReturnType() instanceof DoubleType) {
            method.setReturnType(RefType.v("java.lang.Double"));
        }

        List<Type> newParamTypes = new ArrayList<>();
        for (Type paramType : method.getParameterTypes()) {
            if (paramType instanceof DoubleType) {
                newParamTypes.add(RefType.v("java.lang.Double"));
            } else {
                newParamTypes.add(paramType);
            }
        }
        method.setParameterTypes(newParamTypes);

        for (Local local : new ArrayList<>(body.getLocals())) { // Copy to avoid ConcurrentModificationException
            if (local.getType() instanceof DoubleType) {
                local.setType(RefType.v("java.lang.Double"));
            }
        }

        for (Unit unit : unitList) {
            Stmt stmt = (Stmt) unit;

            if (stmt instanceof IdentityStmt) {
                IdentityStmt identityStmt = (IdentityStmt) stmt;
                Value leftOp = identityStmt.getLeftOp();
                Value rightOp = identityStmt.getRightOp();


                if (leftOp instanceof Local && leftOp.getType() instanceof DoubleType) {
                    ((Local) leftOp).setType(RefType.v("java.lang.Double"));
                }
                if (rightOp instanceof ParameterRef && rightOp.getType() instanceof DoubleType) {
                    rightOp = Jimple.v().newParameterRef(RefType.v("java.lang.Double"), ((ParameterRef) rightOp).getIndex());
                }

                IdentityStmt newIdentityStmt = Jimple.v().newIdentityStmt(leftOp, rightOp);
                units.swapWith(identityStmt, newIdentityStmt); // Replace safely
                continue;
            }
            if (unit instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) unit;
                Value leftOp = assignStmt.getLeftOp();
                Value rightOp = assignStmt.getRightOp();


                if (leftOp.getType() instanceof RefType &&
                        ((RefType) leftOp.getType()).getClassName().equals("java.lang.Double")) {

                    AssignStmt newAssignStmt = null;
                    Local tmpLocal = null;

                    if (rightOp instanceof DoubleConstant) {
                        InvokeExpr valueOfExpr = Jimple.v().newStaticInvokeExpr(
                                Scene.v().makeMethodRef(
                                        Scene.v().getSootClass("java.lang.Double"),
                                        "valueOf",
                                        Collections.singletonList(DoubleType.v()),
                                        RefType.v("java.lang.Double"),
                                        true
                                ),
                                rightOp
                        );
                        newAssignStmt = Jimple.v().newAssignStmt(leftOp, valueOfExpr);
                    }

                    else if (rightOp instanceof BinopExpr) {
                        BinopExpr binOp = (BinopExpr) rightOp;

                        if (tmpLocal == null) {
                            tmpLocal = Jimple.v().newLocal("tmpDouble", DoubleType.v());
                            body.getLocals().add(tmpLocal);
                        }

                        AssignStmt tmpAssign = Jimple.v().newAssignStmt(tmpLocal, binOp);
                        units.insertBefore(tmpAssign, assignStmt);

                        InvokeExpr valueOfExpr = Jimple.v().newStaticInvokeExpr(
                                Scene.v().makeMethodRef(
                                        Scene.v().getSootClass("java.lang.Double"),
                                        "valueOf",
                                        Collections.singletonList(DoubleType.v()),
                                        RefType.v("java.lang.Double"),
                                        true
                                ),
                                tmpLocal;
                        );
                        newAssignStmt = Jimple.v().newAssignStmt(leftOp, valueOfExpr);
                    }

                    if (newAssignStmt != null) {
                        units.swapWith(assignStmt, newAssignStmt);
                        System.out.println("[FIXED] Replaced Assignment: " + newAssignStmt);
                    }
                }


                if (rightOp instanceof FieldRef) {
                    FieldRef fieldRef = (FieldRef) rightOp;
                    SootField field = fieldRef.getField();

                    String fieldName;
                    SootClass declaringClass;
                    String fieldType;

                    if (field != null) {
                        fieldName = field.getName();
                        declaringClass = field.getDeclaringClass();
                        fieldType = field.getType().toString();
                    }
                    else {
                        String fieldStr = rightOp.toString();
                        String[] parts = fieldStr.split(":");

                        if (parts.length == 2) {
                            String classAndField = parts[0].trim();
                            String typeAndField = parts[1].trim();
                            int classStart = classAndField.indexOf('<') + 1;

                            int typeEnd = typeAndField.indexOf(' ');

                            if (classStart > 0 && typeEnd > 0) {
                                String className1 = classAndField.substring(classStart).trim();
                                fieldName = typeAndField.replace(">", "").substring(typeEnd + 1).trim();
                                fieldType = typeAndField.substring(0, typeEnd).trim();

                                declaringClass = Scene.v().getSootClassUnsafe(className1);
                            } else {
                                System.out.println("[ERROR] Failed to parse field info: " + fieldStr);
                                continue;
                            }
                        } else {
                            System.out.println("[ERROR] Unexpected field format: " + fieldStr);
                            continue;
                        }
                    }

                    if (fieldType.equals("double") && declaringClass != null) {
                        String newType = "java.lang.Double";
                        SootField updatedField = null;

                        if (declaringClass.declaresField(fieldName, RefType.v(newType))) {
                            updatedField = declaringClass.getField(fieldName, RefType.v(newType));
                        } else {
                            SootClass superclass = declaringClass.getSuperclassUnsafe();
                            while (superclass != null && !superclass.isPhantom()) {
                                if (superclass.declaresField(fieldName, RefType.v(newType))) {
                                    updatedField = superclass.getField(fieldName, RefType.v(newType));
                                    System.out.println("[INFO] Found field in superclass: " + superclass.getName());
                                    break;
                                }
                                superclass = superclass.getSuperclassUnsafe();
                            }

                            if (updatedField == null) {
                                for (SootClass cls : Scene.v().getClasses()) {
                                    if (!cls.isPhantom() && cls.declaresField(fieldName, RefType.v(newType))) {
                                        updatedField = cls.getField(fieldName, RefType.v(newType));
                                        System.out.println("[INFO] Found field in another class: " + cls.getName());
                                        break;
                                    }
                                }
                            }
                        }
                        if (updatedField != null) {
                            AssignStmt newAssignStmt;
                            if (fieldRef instanceof InstanceFieldRef) {
                                Value newFieldRef = Jimple.v().newInstanceFieldRef(((InstanceFieldRef) fieldRef).getBase(), updatedField.makeRef());
                                newAssignStmt = Jimple.v().newAssignStmt(leftOp, newFieldRef);
                            } else {
                                Value newFieldRef = Jimple.v().newStaticFieldRef(updatedField.makeRef());
                                newAssignStmt = Jimple.v().newAssignStmt(leftOp, newFieldRef);
                            }

                            units.swapWith(assignStmt, newAssignStmt);
                            System.out.println("[INFO] Updated statement: " + newAssignStmt);
                        } else {
                            System.out.println("[WARNING] No matching field found in class hierarchy for: " + fieldName);
                        }
                    }
                }
            }

            if (!stmt.containsInvokeExpr()) {
                continue;
            }
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            SootMethod invokedMethod = invokeExpr.getMethod();
            boolean changed = false;
            String classNameInvokedMethod = invokedMethod.getDeclaringClass().getName();
            if (classNameInvokedMethod.startsWith("java.") || classNameInvokedMethod.startsWith("javax.") ||
                    classNameInvokedMethod.startsWith("android.") || classNameInvokedMethod.startsWith("androidx.")) {
                continue;
            }

            Type newReturnType = invokedMethod.getReturnType();
            if (newReturnType instanceof DoubleType) {
                newReturnType = RefType.v("java.lang.Double");
                changed = true;
            }

            List<Type> newMethodParamTypes = new ArrayList<>();
            for (Type paramType : invokedMethod.getParameterTypes()) {
                if (paramType instanceof DoubleType) {
                    newMethodParamTypes.add(RefType.v("java.lang.Double"));
                    changed = true;
                } else {
                    newMethodParamTypes.add(paramType);
                }
            }

            if (!changed) {
                continue;
            }

            boolean isStatic = Modifier.isStatic(invokedMethod.getModifiers());

            SootMethodRef newMethodRef = Scene.v().makeMethodRef(
                    invokedMethod.getDeclaringClass(),
                    invokedMethod.getName(),
                    newMethodParamTypes,
                    newReturnType,
                    isStatic
            );

            InvokeExpr newInvokeExpr;
            List<Value> newArgs = new ArrayList<>(invokeExpr.getArgs());

            if (invokeExpr instanceof VirtualInvokeExpr) {
                newInvokeExpr = Jimple.v().newVirtualInvokeExpr(
                        (Local) ((VirtualInvokeExpr) invokeExpr).getBase(),
                        newMethodRef,
                        newArgs
                );
            } else if (invokeExpr instanceof StaticInvokeExpr) {
                newInvokeExpr = Jimple.v().newStaticInvokeExpr(
                        newMethodRef,
                        newArgs
                );
            } else if (invokeExpr instanceof InterfaceInvokeExpr) {
                newInvokeExpr = Jimple.v().newInterfaceInvokeExpr(
                        (Local) ((InterfaceInvokeExpr) invokeExpr).getBase(),
                        newMethodRef,
                        newArgs
                );
            } else if (invokeExpr instanceof SpecialInvokeExpr) {  // **Handle specialinvoke properly**
                newInvokeExpr = Jimple.v().newSpecialInvokeExpr(
                        (Local) ((SpecialInvokeExpr) invokeExpr).getBase(),
                        newMethodRef,
                        newArgs
                );
            } else {
                newInvokeExpr = invokeExpr; // Fallback case
            }

            if (stmt instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) stmt;
                AssignStmt newAssignStmt = Jimple.v().newAssignStmt(assignStmt.getLeftOp(), newInvokeExpr);
                units.swapWith(stmt, newAssignStmt);
            } else {
                InvokeStmt newInvokeStmt = Jimple.v().newInvokeStmt(newInvokeExpr);
                units.swapWith(stmt, newInvokeStmt);
            }
        }
    }

    private static void changeMethodSignatureOnly(SootMethod method) {
        String className = method.getDeclaringClass().getName();
        if (className.startsWith("java.") || className.startsWith("javax.") ||
                className.startsWith("android.") || className.startsWith("androidx.")) {
            return;
        }
        boolean changed = false;
        if (method.getReturnType() instanceof DoubleType) {
            method.setReturnType(RefType.v("java.lang.Double"));
            changed = true;
        }

        List<Type> newParamTypes = new ArrayList<>();
        for (Type paramType : method.getParameterTypes()) {
            if (paramType instanceof DoubleType) {
                newParamTypes.add(RefType.v("java.lang.Double"));
                changed = true;
            } else {
                newParamTypes.add(paramType);
            }
        }
        if (changed) {
            method.setParameterTypes(newParamTypes);
            System.out.println("Updated Signature, for Without Body Method! " + method);
        }
    }

    private static void convertDoubleFieldsInClass(SootClass clazz) {
        for (SootField field : clazz.getFields()) {
            if (field.getType() instanceof DoubleType) {
                field.setType(RefType.v("java.lang.Double"));
                System.out.println("Updated Field: " + field.getSignature() + " → java.lang.Double");
            }
        }
    }
}
