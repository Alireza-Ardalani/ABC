
package qilin.parm.select;


import qilin.core.context.ContextElement;
import qilin.core.context.ContextElements;
import qilin.core.pag.AllocNode;
import qilin.core.pag.FieldValNode;
import qilin.core.pag.LocalVarNode;
import qilin.parm.ctxcons.CtxConstructor;
import soot.Context;
import soot.SootMethod;
import qilin.DataClass;

import java.util.*;

public abstract class CtxSelector {


    public abstract Context select(SootMethod m, Context context);

    public abstract Context select(LocalVarNode lvn, Context context);

    public abstract Context select(FieldValNode fvn, Context context);

    public abstract Context select(AllocNode heap, Context context);

    /**
     *  Component
     */
    protected Context contextTailor_Component(Context context, int length) {
        ContextElements ctx = (ContextElements) context;
        ContextElement[] fullContexts = ctx.getElements();
        String origin = ctx.getOrigin();
        if (length == 0) {
            return new ContextElements(new ContextElement[0], 0 , origin);
        }
        if (length >= ctx.size()) {
            return context;
        }
        if(origin.isEmpty()){
                String temp =  fullContexts[fullContexts.length-1].toString();
                temp  = temp.substring(temp.indexOf("<"), temp.lastIndexOf(">")+1);
                if(DataClass.components.contains(temp)){
                    origin = temp;
                }
        }
        ContextElement[] newContexts = new ContextElement[length];
        System.arraycopy(fullContexts, 0, newContexts, 0, length);
        return new ContextElements(newContexts, length, origin);
    }
    /**
     *  Bytecode
     */
    protected Context contextTailor_Bytecode(Context context, int length) {
        ContextElements ctx = (ContextElements) context;
        ContextElement[] fullContexts = ctx.getElements();
        String origin = ctx.getOrigin() ;
        if (length == 0) {
            return new ContextElements(new ContextElement[0], 0 , origin);
        }
        if (length >= ctx.size()) {
            return context;
        }
        String temp =  fullContexts[0].toString();
        temp  = temp.substring(temp.indexOf("<")+1, temp.lastIndexOf(":"));
        String check1;
        String[] splitter = temp.split("\\.");
        // There was a try-catch statement! and I remove it.
        try{
            check1 = splitter[0]+"."+splitter[1];
            if (DataClass.origins.contains(check1)){
                origin = check1;
            }
        } catch (Exception E){
            System.out.println(temp);
            System.out.println("contextTailor-> " + E);
        }
        ContextElement[] newContexts = new ContextElement[length];
        System.arraycopy(fullContexts, 0, newContexts, 0, length);
        return new ContextElements(newContexts, length, origin);
    }

    /**
     *  Original K-CFA
     */

    protected Context contextTailor_0(Context context, int length) {
        if (length == 0) {
            return CtxConstructor.emptyContext;
        }
        ContextElements ctx = (ContextElements) context;
        ContextElement[] fullContexts = ctx.getElements();
        if (length >= ctx.size()) {
            return context;
        }
        ContextElement[] newContexts = new ContextElement[length];
        System.arraycopy(fullContexts, 0, newContexts, 0, length);
        return new ContextElements(newContexts, length,"");
    }

    /**
     *  Bytecode_Old_Version
     */

    protected Context contextTailor_Bytecode_Old_Version(Context context, int length) {
        ContextElements ctx = (ContextElements) context;
        ContextElement[] fullContexts = ctx.getElements();
        String origin = ctx.getOrigin() ;
        if (length == 0) {
            return new ContextElements(new ContextElement[0], 0 , origin);
        }
        if (length >= ctx.size()) {
            return context;
        }
        String temp =  fullContexts[fullContexts.length-1].toString();
        temp  = temp.substring(temp.indexOf("<")+1, temp.lastIndexOf(":"));
        String check1;
        String[] splitter = temp.split("\\.");
        try{
            check1 = splitter[0]+"."+splitter[1];
            if (DataClass.origins.contains(check1)){
                if(!origin.contains(check1)){
                    origin = origin+"_"+check1;
                }
            }
        } catch (Exception E){

        }
        ContextElement[] newContexts = new ContextElement[length];
        System.arraycopy(fullContexts, 0, newContexts, 0, length);
        return new ContextElements(newContexts, length, origin);
    }

}
