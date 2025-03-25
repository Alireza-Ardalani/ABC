
package qilin.core.context;

import qilin.DataClass;
import soot.Context;

import java.util.Arrays;

public class ContextElements implements Context {
    private final ContextElement[] array;
    private final int size;
    private int hashCode = 0;
    /**
     * For now, have been used  for Component or Bytecode Context.
     * In the future we will split it into two different field, then we can hava
     * K-CFA + Bytecode + Component
     */
    public String  origin = "";
    public ContextElements(ContextElement[] array, int s, String origin) {
        this.array = array;
        this.size = s;
        this.origin = origin;
        for(int i = array.length-1; i > -1 ; i--){
            String temp =  array[i].toString();
            temp  = temp.substring(temp.indexOf("<")+1, temp.lastIndexOf(":"));
            String check1;
            String[] splitter = temp.split("\\.");
            if(splitter.length>1){
                check1 = splitter[0]+"."+splitter[1];
                if (DataClass.origins.contains(check1)){
                    this.origin = check1;
                }
            }
        }
    }

    public ContextElement[] getElements() {
        return array;
    }

    public ContextElement get(int i) {
        return array[i];
    }

    public boolean contains(ContextElement heap) {
        for (int i = 0; i < size; ++i) {
            if (array[i] == heap) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }


    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = 31 * Arrays.hashCode(array) + (origin != null ? origin.hashCode() : 0);
        }
        return hashCode;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        ContextElements other = (ContextElements) obj;

        if (this.array == null || other.array == null)
            return false;

        if (this.size() != other.size())
            return false;
        if(this.origin == null || other.origin == null ){
            return false;
        }
        if(!this.origin.equals(other.origin)){
            return false;
        }
        for (int i = 0; i < size(); i++) {
            Object o1 = this.array[i];
            Object o2 = other.array[i];
            if (!(o1 == null ? o2 == null : o1.equals(o2)))
                return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder localStringBuilder = new StringBuilder();
        localStringBuilder.append('[');
        for (int i = 0; i < array.length; i++) {
            localStringBuilder.append(array[i]);
            if (i < array.length - 1) {
                localStringBuilder.append(", ");
            }
        }
        localStringBuilder.append(']');
        return localStringBuilder.toString();
    }

    /**
     * Compose a new context by a given context and a heap.
     * The depth of new context is also required.
     */
    public static ContextElements newContext(ContextElements c, ContextElement ce, int depth) {
        ContextElement[] array;
        if (c.size() < depth) {
            array = new ContextElement[c.size() + 1];
            System.arraycopy(c.getElements(), 0, array, 1, c.size());
        } else {
            array = new ContextElement[depth];
            System.arraycopy(c.getElements(), 0, array, 1, depth - 1);
        }
        array[0] = ce;
        return new ContextElements(array, array.length, c.getOrigin());
    }

    public String getOrigin() {

        return origin;
    }
    public void setOrigin(String inputOrigin) {

        this.origin = inputOrigin;
    }

}
