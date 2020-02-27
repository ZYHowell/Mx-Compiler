package MIR.IRtype;

import java.util.ArrayList;

public class ClassType extends IRBaseType{
    //each class type is treated as a pointer(an i32 address)
    private String name;
    private int size = 0;
    private ArrayList<IRBaseType> members = new ArrayList<>();

    public ClassType(String name) {
        super();
        this.name = name;
    }

    public void addMember(IRBaseType member) {
        members.add(member);
        size += member.size();
    }

    @Override
    public int size() {
        return size;
    }
    @Override
    public String toString() {
        return "%struct." + name;
    }
}
