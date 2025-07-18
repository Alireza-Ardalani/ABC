package qilin.core.context;

import qilin.DataClass;
import soot.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContextElementsBytecodeOrigin extends ContextElements {
    private int hashCode = 0;


    public ContextElementsBytecodeOrigin(ContextElement[] array, int s, String origin, List<String> arrayTop) {
        super(array, s, origin, arrayTop);
        this.offContext = origin;
        /**
         ByteCode_part
         **/
        for(int i = array.length-1; i > -1 ; i--){
            String temp =  array[i].toString();
            temp  = temp.substring(temp.indexOf("<")+1, temp.lastIndexOf(":"));
            String check1;
            String[] splitter = temp.split("\\.");
            if(splitter.length>1){
                check1 = splitter[0]+"."+splitter[1];
                if (DataClass.byteCodeOrigins.contains(check1)){
                    this.offContext = check1;
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
            hashCode = Arrays.hashCode(array);
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        ContextElementsBytecodeOrigin other = (ContextElementsBytecodeOrigin) obj;

        if (this.array == null || other.array == null)
            return false;

        if (this.size() != other.size())
            return false;
        if(this.offContext == null || other.offContext == null ){
            return false;
        }
        if(!this.offContext.equals(other.offContext)){
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

    public String getOffContext() {
        return offContext;
    }
    public void setOffContext(String inputOrigin) {
        this.offContext = inputOrigin;
    }
    public List<String> getTopElements() {
        return arrayTop;
    }


}
