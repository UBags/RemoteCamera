package com.costheta.machine;

import java.util.ArrayList;

public interface MachiningNode {

    String getName();

    MachiningNode getParent();

    void setParent(MachiningNode machiningNode);

    ArrayList<MachiningNode> getChildren();
}
