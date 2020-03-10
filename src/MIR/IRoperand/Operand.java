package MIR.IRoperand;

import MIR.IRinst.Inst;
import MIR.IRtype.IRBaseType;

import java.util.HashSet;

abstract public class Operand {

    private IRBaseType type;

    public Operand(IRBaseType type) {
        this.type = type;
    }


    public Inst defInst() {
        return null;
    }
    public IRBaseType type() {
        return type;
    }

    public abstract HashSet<Inst> uses();
    public abstract void addUse(Inst inst);
    public abstract void removeUse(Inst inst);
    public abstract Operand copy();
    public abstract String toString();
}
