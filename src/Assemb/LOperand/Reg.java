package Assemb.LOperand;

import Assemb.RISCInst.Mv;

import java.util.HashSet;

public abstract class Reg extends LOperand {
    public int degree = 0, weight = 0;
    public Reg alias = null;
    public PhyReg color;
    public Imm stackOffset = null;

    public HashSet<Mv> moveInst = new HashSet<>();
    public Reg() {
        super();
        if (this instanceof PhyReg) color = (PhyReg)this;
        else color = null;
    }

    public void init() {
        moveInst.clear();
        weight = 0;
    }
}
