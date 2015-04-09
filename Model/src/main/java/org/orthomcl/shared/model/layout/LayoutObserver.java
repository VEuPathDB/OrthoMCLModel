package org.orthomcl.shared.model.layout;

public interface LayoutObserver {

  void step(Graph graph, int iteration, double globalStress);

  void finish(Graph graph, int iteration, double globalStress);

}
