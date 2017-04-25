package org.orthomcl.shared.model.layout;

public interface Edge {

  double getPreferredLength();

  Node getNodeA();

  Node getNodeB();

}
