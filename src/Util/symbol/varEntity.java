package Util.symbol;

import MIR.IRoperand.*;

public class varEntity extends Entity {

    private Type type;
    private boolean isOuter;    //indicate if the variable is {member of a class, global variable}
    private boolean isGlobal, isMember;   //in some way similar with the one above, but used for IR
    private Operand asOperand;  //if it is a member, is this able to be used as the offset?
    private Operand offset;

    public varEntity(String name, Type type, boolean isOuter, boolean isGlobal) {
        super(name);
        this.type = type;
        this.isOuter = isOuter;
        this.isGlobal = isGlobal;
        this.isMember = false;
    }

    public Operand offset() {
        return offset;
    }
    public void setOffset(int offset) {
        this.offset = new ConstInt(offset);
    }
    public Type type() { return type; }
    public boolean isOuter() {
        return isOuter;
    }
    public boolean isGlobal() { return isGlobal; }
    public void setIsMember() {
        isMember = true;
    }
    public boolean isMember() {
        return isMember;
    }
    public void setOperand(Operand ope) {
        asOperand = ope;
    }
    public Operand asOperand() {
        return asOperand;
    }
}
