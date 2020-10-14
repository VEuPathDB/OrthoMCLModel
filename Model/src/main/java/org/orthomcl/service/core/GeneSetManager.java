package org.orthomcl.service.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.query.SqlQuery;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.record.RecordClass;
import org.orthomcl.service.core.layout.RenderingHelper;

public class GeneSetManager {

  private static final String RECORD_CLASS = "SequenceRecordClasses.SequenceRecordClass";

  private static final String ATTRIBUTE_QUERY = "SequenceAttributes.SequenceAttrs";
  private static final String PFAM_QUERY = "SequenceTables.PFamDomains";

  private final WdkModel _wdkModel;

  public GeneSetManager(WdkModel wdkModel) {
    _wdkModel = wdkModel;
  }

  public GeneSet getGeneSet(AnswerValue answer) throws WdkModelException {
    // check if the step is of the supported sequence type
    Question question = answer.getAnswerSpec().getQuestion();
    RecordClass recordClass = question.getRecordClass();
    if (!recordClass.getFullName().equals(RECORD_CLASS))
      throw new WdkModelException("Only steps of type " + RECORD_CLASS + " are supported.");

    GeneSet geneSet = new GeneSet(question.getDisplayName());
    loadGenes(answer, geneSet);
    loadPFamDomains(answer, geneSet);
    processPFamDomains(geneSet);
    processEcNumbers(geneSet);

    return geneSet;
  }

  public void loadGenes(AnswerValue answer, GeneSet geneSet) throws WdkModelException {
    Map<String, Taxon> taxons = TaxonManager.getTaxons(_wdkModel);

    String idSql = answer.getIdSql();
    SqlQuery attrQuery = (SqlQuery) _wdkModel.resolveReference(ATTRIBUTE_QUERY);
    String sql = "SELECT aq.full_id, aq.product, aq.length, aq.abbreviation, aq.ec_numbers      " +
        " FROM (" + attrQuery.getSql() + ") aq, (SELECT * FROM (" + idSql + ")) idq " +
        " WHERE aq.full_id = idq.full_id";
    DataSource dataSource = _wdkModel.getAppDb().getDataSource();
    ResultSet resultSet = null;
    try {
      resultSet = SqlUtils.executeQuery(dataSource, sql, ATTRIBUTE_QUERY + "__whole-step", 1000);
      while (resultSet.next()) {
        Gene gene = new Gene(resultSet.getString("full_id"));
        gene.setDescription(resultSet.getString("product"));
        gene.setLength(resultSet.getLong("length"));
        gene.setTaxon(taxons.get(resultSet.getString("abbreviation")));

        // load ec numbers
        String ecNumbers = resultSet.getString("ec_numbers");
        if (ecNumbers != null) {
          for (String code : ecNumbers.split(",")) {
            code = code.trim();
            gene.addEcNumber(code);

            // also add ec number object
            EcNumber ecNumber = geneSet.getEcNumber(code);
            if (ecNumber == null) {
              ecNumber = new EcNumber(code);
              geneSet.addEcNumber(ecNumber);
            }
            ecNumber.setCount(ecNumber.getCount() + 1);
          }
        }
        geneSet.addGene(gene);
      }
    }
    catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(resultSet, null);
    }
  }

  private void processEcNumbers(GeneSet geneSet) {
    List<EcNumber> ecNumbers = new ArrayList<>(geneSet.getEcNumbers().values());
    Collections.sort(ecNumbers);

    // assign index
    for (int i = 0; i < ecNumbers.size(); i++) {
      ecNumbers.get(i).setIndex(i);
    }

    // assign spectrum colors to the ec numbers
    RenderingHelper.assignSpectrumColors(ecNumbers);

  }

  private void loadPFamDomains(AnswerValue answer, GeneSet geneSet) throws WdkModelException {
    String idSql = answer.getIdSql();
    DataSource dataSource = _wdkModel.getAppDb().getDataSource();
    SqlQuery pfamQuery = (SqlQuery) _wdkModel.resolveReference(PFAM_QUERY);

    // get pfam summary
    String sql = "SELECT aq.accession, aq.symbol, aq.description, count(DISTINCT aq.full_id) AS gene_count " +
        " FROM (" + pfamQuery.getSql() + ") aq, (" + idSql + ") idq       " +
        " WHERE aq.full_id = idq.full_id   " + " GROUP BY aq.accession, aq.symbol, aq.description  ";
    ResultSet resultSet = null;
    int index = 0;
    try {
      resultSet = SqlUtils.executeQuery(dataSource, sql, PFAM_QUERY + "__step-summary", 1000);
      while (resultSet.next()) {
        PFamDomain pfam = new PFamDomain(resultSet.getString("accession"));
        pfam.setSymbol(resultSet.getString("symbol"));
        pfam.setDescription(resultSet.getString("description"));
        pfam.setCount(resultSet.getInt("gene_count"));
        pfam.setIndex(index);
        geneSet.addPFamDomain(pfam);

        index++;
      }
    }
    catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(resultSet, null);
    }

    // get pfams for each gene
    sql = "SELECT aq.full_id, aq.accession, aq.start_min, aq.end_max, aq.length                  " +
        " FROM (" + pfamQuery.getSql() + ") aq, (" + idSql + ") idq       " +
        " WHERE aq.full_id = idq.full_id AND aq.start_min IS NOT NULL";
    try {
      resultSet = SqlUtils.executeQuery(dataSource, sql, PFAM_QUERY + "__whole-step", 1000);
      while (resultSet.next()) {
        Gene gene = geneSet.getGene(resultSet.getString("full_id"));
        String accession = resultSet.getString("accession");
        int[] location = { resultSet.getInt("start_min"), resultSet.getInt("end_max"),
            resultSet.getInt("length") };
        gene.addPFamDomain(accession, location);
      }
    }
    catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(resultSet, null);
    }
  }

  private void processPFamDomains(GeneSet geneSet) {
    List<PFamDomain> pfams = new ArrayList<>(geneSet.getPFamDomains().values());
    Collections.sort(pfams);

    // assign index
    for (int i = 0; i < pfams.size(); i++) {
      pfams.get(i).setIndex(i);
    }

    // generate random color for the domains
    RenderingHelper.assignSpectrumColors(pfams);
  }
}
