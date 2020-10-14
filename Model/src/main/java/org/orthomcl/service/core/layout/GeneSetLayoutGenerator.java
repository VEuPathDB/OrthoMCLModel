package org.orthomcl.service.core.layout;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.sql.DataSource;

import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.orthomcl.service.core.Gene;
import org.orthomcl.service.core.GenePair;
import org.orthomcl.service.core.GeneSet;
import org.orthomcl.service.core.Taxon;
import org.orthomcl.shared.model.layout.GraphicsException;
import org.orthomcl.shared.model.layout.SpringLayout;

public class GeneSetLayoutGenerator {

  public static final int MAX_GENES = 500;

  private static final int DEFAULT_SIZE = LayoutManager.DEFAULT_SIZE;
  
  private final Random random = new Random();

  public GroupLayout generateLayout(AnswerValue answer) throws WdkModelException {
    // only do layout for the step with genes of MAX_GENES or less
    if (answer.getResultSizeFactory().getResultSize() > MAX_GENES)
      return null;

    GeneSet geneSet = new GeneSet(answer.getAnswerSpec().getQuestion().getDisplayName());
    GroupLayout layout = new GroupLayout(geneSet, DEFAULT_SIZE);
    Map<String, GeneNode> nodes = loadNodes(layout, answer);
    loadEdges(layout, answer, nodes);
    loadEdgeTypes(layout, answer);
    computeLocations(layout);
    return layout;
  }
  
  private Map<String, GeneNode> loadNodes(GroupLayout layout, AnswerValue answer) throws WdkModelException {
    Map<String, GeneNode> nodes = new HashMap<>();
    int index = 0;

    // create a dummy taxon
    Taxon taxon = new Taxon(0);
    taxon.setAbbrev("dummy");

    String idSql = answer.getIdSql();
    String sql = "SELECT full_id FROM (" + idSql + ")";
    WdkModel wdkModel = answer.getWdkModel();
    DataSource dataSource = wdkModel.getAppDb().getDataSource();
    ResultSet resultSet = null;
    try {
      resultSet = SqlUtils.executeQuery(dataSource, sql,
          answer.getAnswerSpec().getQuestion().getFullName() + "__ortho-gene-ids",
          1000);
      while (resultSet.next()) {
        String fullId = resultSet.getString("full_id");
        Gene gene = new Gene(fullId);
        gene.setTaxon(taxon);
        GeneNode node = new GeneNode(gene);
        node.setIndex(index);

        // assign random location to the node; the actual locations will be computed later
        double x = random.nextInt(DEFAULT_SIZE);
        double y = random.nextInt(DEFAULT_SIZE);
        node.setLocation(x, y);
        layout.addNode(node);
        nodes.put(fullId, node);

        index++;
      }
    }
    catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(resultSet, null);
    }
    return nodes;
  }

  private void loadEdges(GroupLayout layout, AnswerValue answer, Map<String, GeneNode> nodes) throws WdkModelException {
    // contruct an SQL to get blast scores
    String idSql = answer.getIdSql();
    String sql = "WITH sequences AS (SELECT * FROM (" + idSql + ")) " +
        " SELECT query_id, subject_id, evalue_mant, evalue_exp FROM apidb.SimilarSequences " +
        " WHERE query_id IN (SELECT full_id FROM sequences) " +
        "  AND subject_id IN (SELECT full_id FROM sequences) ";
    WdkModel wdkModel = answer.getWdkModel();
    DataSource dataSource = wdkModel.getAppDb().getDataSource();
    ResultSet resultSet = null;
    try {
      resultSet = SqlUtils.executeQuery(dataSource, sql, answer.getAnswerSpec().getQuestion().getFullName() + "__ortho-blast-scores",
          1000);
      while (resultSet.next()) {
        String queryId = resultSet.getString("query_id");
        String subjectId = resultSet.getString("subject_id");
        float evalueMant = resultSet.getFloat("evalue_mant");
        if (evalueMant < 0.1) evalueMant = 1;
        int evalueExp = resultSet.getInt("evalue_exp");
        String evalue = evalueMant + "E" + evalueExp;

        BlastEdge edge = new BlastEdge(queryId, subjectId);
        BlastEdge oldEdge = layout.getEdge(edge);
        if (oldEdge != null) { // old edge exists, combine the value
          if (!evalue.equals(oldEdge.getEvalueA())) {
            oldEdge.setEvalueB(evalue);
            double scoreB = Math.log10(Double.valueOf(evalue));
            oldEdge.setScore((oldEdge.getScore() + scoreB) / 2);
          }
        }
        else { // old edge doesn't exist
          edge.setEvalueA(evalue);
          edge.setScore(Math.log10(Double.valueOf(evalue)));
          edge.setNodeA(nodes.get(queryId));
          edge.setNodeB(nodes.get(subjectId));
          // set the edge as default normal type, will be updated later
          edge.setType(EdgeType.Normal);
          layout.addEdge(edge);
        }
      }
    }
    catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(resultSet, null);
    }
  }

  private void loadEdgeTypes(GroupLayout layout, AnswerValue answer) throws WdkModelException {
    loadEdgeTypes(layout, EdgeType.Ortholog, answer, "Ortholog");
    loadEdgeTypes(layout, EdgeType.Coortholog, answer, "Coortholog");
    loadEdgeTypes(layout, EdgeType.Inparalog, answer, "Inparalog");
    loadEdgeTypes(layout, EdgeType.PeripheralCore, answer, "PeripheralCore");
    loadEdgeTypes(layout, EdgeType.PeripheralPeripheral, answer, "PeripheralPeripheral");
  }

  private void loadEdgeTypes(GroupLayout layout, EdgeType type, AnswerValue answer, String tableName)
      throws WdkModelException {
    WdkModel wdkModel = answer.getWdkModel();
    String idSql = answer.getIdSql();
    String sql = "WITH sequences AS (SELECT * FROM (" + idSql + ")) " +
        " SELECT sequence_id_a AS query_id, sequence_id_b AS subject_id FROM apidb." + tableName +
        " WHERE sequence_id_a IN (SELECT full_id FROM sequences) " +
        "   AND sequence_id_b IN (SELECT full_id FROM sequences)";
    DataSource dataSource = wdkModel.getAppDb().getDataSource();
    ResultSet resultSet = null;
    try {
      resultSet = SqlUtils.executeQuery(dataSource, sql, answer.getAnswerSpec().getQuestion().getFullName()+ "__orthomcl-get-" +
          tableName, 1000);
      while (resultSet.next()) {
        String queryId = resultSet.getString("query_id");
        String subjectId = resultSet.getString("subject_id");
        BlastEdge edge = layout.getEdge(new GenePair(queryId, subjectId));
        edge.setType(type);
      }
    }
    catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(resultSet, null);
    }
  }

  private void computeLocations(GroupLayout layout) throws WdkModelException {
    try {
      SpringLayout springLayout = new SpringLayout(layout);
      springLayout.process(null);
    }
    catch (GraphicsException ex) {
      throw new WdkModelException(ex);
    }
  }

}
