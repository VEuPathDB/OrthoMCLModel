package org.orthomcl.service.core.report;

import static org.gusdb.fgputil.functional.Functions.f0Swallow;
import static org.gusdb.wdk.model.answer.TransformUtil.transformToNewResultTypeAnswer;

import org.gusdb.wdk.model.answer.AnswerValue;

public class GroupFastaReporter extends FastaReporter {

  private static final String GROUP_RECORDCLASS = "GroupRecordClasses.GroupRecordClass";
  private static final String SEQUENCE_RECORDCLASS = "SequenceRecordClasses.SequenceRecordClass";
  private static final String XFORM_QUESTION_NAME = "SequenceQuestions.FromGroups";
  private static final String XFORM_STEP_ID_PARAM_NAME = "group_answer";

  @Override
  public GroupFastaReporter setAnswerValue(AnswerValue groupsAnswer) {
    super.setAnswerValue(f0Swallow(() ->
      transformToNewResultTypeAnswer(
        groupsAnswer,
        GROUP_RECORDCLASS,
        XFORM_QUESTION_NAME,
        XFORM_STEP_ID_PARAM_NAME,
        SEQUENCE_RECORDCLASS
      )
    ).get());
    return this;
  }

}
