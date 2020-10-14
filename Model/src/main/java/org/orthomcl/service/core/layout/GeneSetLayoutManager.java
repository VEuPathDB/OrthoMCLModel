package org.orthomcl.service.core.layout;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.orthomcl.service.core.GeneSet;
import org.orthomcl.service.core.GeneSetManager;

public class GeneSetLayoutManager extends LayoutManager {

  public GeneSetLayoutManager(WdkModel wdkModel) {
    super(wdkModel);
  }

  public GroupLayout getLayout(AnswerValue answer, String layoutString) throws WdkModelException {

    // load gene set
    GeneSetManager geneSetManager = new GeneSetManager(_wdkModel);
    GeneSet geneSet = geneSetManager.getGeneSet(answer);
    GroupLayout layout = new GroupLayout(geneSet, getSize());

    loadLayout(layout, layoutString);

    return layout;
  }
}
