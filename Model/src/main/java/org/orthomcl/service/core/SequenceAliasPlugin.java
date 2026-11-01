package org.orthomcl.service.core;

import org.gusdb.wdk.model.record.alias.QueryBasedPrimaryKeyAliasPlugin;

public class SequenceAliasPlugin extends QueryBasedPrimaryKeyAliasPlugin {

  @Override
  protected String getRecordClassFullName() {
    return "SequenceRecordClasses.SequenceRecordClass";
  }

  @Override
  protected String getQueryFullName() {
    return "SequenceIds.ByIdList";
  }

}
