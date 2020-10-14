package org.orthomcl.service.core.wsfplugin;

import org.eupathdb.websvccommon.wsfplugin.blast.NcbiBlastResultFormatter;

public class OrthoMCLBlastResultFormatter extends NcbiBlastResultFormatter {

  @Override
  protected String getProject(String organism) {
    return "OrthoMCL";
  }

}
