package qilin;

import soot.SootMethod;

import java.util.HashSet;
import java.util.Set;

public class EventBasedEntryPoints {

    static  Set<SootMethod> entryPoints =  new HashSet<>();
    static  Set<String> entryPointsString =  new HashSet<>();
    public EventBasedEntryPoints(){

    }

    public void setEntryPoints(Set<SootMethod> inputEntryPoints) {
        for(SootMethod method : inputEntryPoints){
            if (isNotCorePackages(method)) {
                entryPoints.add(method);
                entryPointsString.add(method.getSignature());
                System.out.println("EntryPoint: " + method.getSignature());
            }

        }
    }

    private boolean isNotCorePackages(SootMethod method) {
        if(method.getDeclaringClass().toString().trim().startsWith("androidx.") ||
                method.getDeclaringClass().toString().trim().startsWith("android.")){
            return false;
        }
        return true;
    }

    public Set<SootMethod> getEntryPoints() {
        return entryPoints;
    }

    public static Set<String> getEntryPointsString() {
        return entryPointsString;
    }
}
