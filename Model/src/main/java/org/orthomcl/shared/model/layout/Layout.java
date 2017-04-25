package org.orthomcl.shared.model.layout;

public interface Layout {

  public static final double MAX_PREFERRED_LENGTH = 230;

  long getMaxIterations();

  void setMaxIterations(long maxIterations);

  void setMinMoves(double minMoves);

  void process(LayoutObserver observer);

  void cancel();

  boolean isStopped();

  Graph getGraph();
}
