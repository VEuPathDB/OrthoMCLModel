package org.orthomcl.service.core.wsfplugin;

import org.eupathdb.websvccommon.wsfplugin.blast.*;
import org.veupathdb.lib.blast.BlastTool;
import org.veupathdb.lib.blast.field.Dust;
import org.veupathdb.lib.blast.field.Seg;

import java.util.List;
import java.util.Map;

public class OrthoMCLBlastPlugin extends AbstractMultiBlastServicePlugin {

  public static final String PARAM_DATABASE = "BlastDatabase";

  public OrthoMCLBlastPlugin() {
    super(new OrthoMCLBlastResultFormatter());
  }

  /**
   * Mash-up of the AbstractBlastPlugin required params and multiblast required
   * params.
   * <p>
   * This was done to avoid client work and a database migration to
   * update the old CLI "ByBlast" process query parameters names to the standard
   * mblast process query parameters.
   */
  @Override
  public String[] getRequiredParameterNames() {
    return new String[] {
      PARAM_DATABASE,
      AbstractBlastPlugin.PARAM_ALGORITHM,
      AbstractBlastPlugin.PARAM_SEQUENCE,
      AbstractBlastPlugin.PARAM_RECORD_CLASS,
      AbstractBlastPlugin.PARAM_MAX_SUMMARY,
      AbstractBlastPlugin.PARAM_EVALUE
    };
  }

  @Override
  protected MBlastJobConfig buildNewBlastConfig(Map<String, String> params) {
    final var out = new MBlastJobConfig()
      .setTool(BlastTool.fromString(params.get(AbstractBlastPlugin.PARAM_ALGORITHM)))
      .setQuery(params.get(AbstractBlastPlugin.PARAM_SEQUENCE))
      .setEValue(params.get(AbstractBlastPlugin.PARAM_EVALUE))
      .setNumAlignments(longFrom(params.get(AbstractBlastPlugin.PARAM_MAX_SUMMARY)));

    out.setNumDescriptions(out.getNumAlignments());

    var filter = params.get(AbstractBlastPlugin.PARAM_FILTER);

    if (filter != null) {
      if ("yes".equals(filter)) {
        if (out.getTool() == BlastTool.BlastN) {
          out.setDust(Dust.yesDust());
        } else {
          out.setSeg(Seg.getYesSeg());
        }
      } else {
        if (out.getTool() == BlastTool.BlastN) {
          out.setDust(Dust.noDust());
        } else {
          out.setSeg(Seg.getNoSeg());
        }
      }
    }

    return out;
  }

  @Override
  protected List<MBlastJobRequest.JobTarget> buildBlastTargetList(Map<String, String> params) {
    var database = params.get(PARAM_DATABASE);
    if (database.charAt(0) == '\'')
      database = database.substring(1, database.length()-1);

//    var lastSlash = database.lastIndexOf('/');
//    if (lastSlash > -1)
//      database = database.substring(lastSlash+1);

    params.put(MultiBlastServiceParams.BLAST_DATABASE_ORGANISM_PARAM_NAME, database);
    params.put(MultiBlastServiceParams.BLAST_DATABASE_TYPE_PARAM_NAME, "");
    return super.buildBlastTargetList(params);
  }

  private static Long longFrom(String value) {
    return value == null ? null : Long.valueOf(value);
  }
}
