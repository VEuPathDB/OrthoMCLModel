package org.orthomcl.service.core.report;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.stream.FileBasedRecordStream;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.attribute.AttributeField;
import org.gusdb.wdk.model.report.AbstractReporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONObject;

/**
 * @author xingao
 */
public class FastaReporter extends AbstractReporter {

  private static Logger logger = Logger.getLogger(FastaReporter.class);

  public static final String FIELD_DOWNLOAD_TYPE = "downloadType";
  public static final String FIELD_HAS_ORGANISM = "hasOrganism";
  public static final String FIELD_HAS_DESCRIPTION = "hasDescription";
  public static final String FIELD_HAS_GROUP = "hasGroup";

  private static final String ATTR_FULL_ID = "full_id";
  private static final String ATTR_ORGANISM = "taxon_name";
  private static final String ATTR_DESCRIPTION = "product";
  private static final String ATTR_SEQUENCE = "sequence";
  private static final String ATTR_GROUP = "group_name";

  private static final String[] NEEDED_ATTRIBUTES = {
      ATTR_FULL_ID, ATTR_ORGANISM, ATTR_DESCRIPTION, ATTR_SEQUENCE, ATTR_GROUP
  };

  private static final int FASTA_LINE_LENGTH = 60;

  private boolean _includeOrganism;
  private boolean _includeDescription;
  private boolean _includeGroup;
  private String _downloadType;

  @Override
  public FastaReporter configure(Map<String,String> config) throws ReporterConfigException {

    // get basic configurations
    _downloadType = getValidDownloadTypeString(config.get(FIELD_DOWNLOAD_TYPE));

    String strOrganism = config.get(FIELD_HAS_ORGANISM);
    _includeOrganism = (strOrganism != null && (strOrganism.equals("yes") || strOrganism.equals("true")));

    String strDescription = config.get(FIELD_HAS_DESCRIPTION);
    _includeDescription = (strDescription != null && (strDescription.equals("yes") || strDescription.equals("true")));

    String strGroup = config.get(FIELD_HAS_GROUP);
    _includeGroup = (strGroup != null && (strGroup.equals("yes") || strGroup.equals("true")));

    return this;
  }

  /**
   * Expected input:
   * {
   *   includeOrganism: bool,
   *   includeDescription: bool,
   *   includeGroup: bool,
   *   attachmentType: string
   * }
   * where:
   *   text = download
   *   plain = show in browser
   */
  @Override
  public FastaReporter configure(JSONObject config) throws ReporterConfigException {
    _downloadType = getValidDownloadTypeString(config.getString("attachmentType"));
    _includeOrganism = JsonUtil.getBooleanOrDefault(config, "includeOrganism", true);
    _includeDescription = JsonUtil.getBooleanOrDefault(config, "includeDescription", true);
    _includeGroup = JsonUtil.getBooleanOrDefault(config, "includeGroup", true);
    return this;
  }

  private static String getValidDownloadTypeString(String downloadType) throws ReporterConfigException {
    if (downloadType == null ||
        (!downloadType.equals("text") && !downloadType.equals("plain"))) {
      throw new ReporterConfigException("Property 'downloadType' is required.  Value must be 'text' or 'plain'.");
    }
    return downloadType;
  }

  @Override
  public String getDownloadFileName() {
    if (_downloadType.equals("text")) {
      logger.info("Internal format: " + _downloadType);
      String name = getQuestion().getName();
      return name + ".fasta";
    }
    else {
      // use the default content type defined in the parent class
      return super.getDownloadFileName();
    }
  }

  @Override
  public void write(OutputStream out) throws WdkModelException {
    RecordClass sequenceRecordClass = _baseAnswer.getQuestion().getRecordClass(); // it better be!

    List<AttributeField> neededAttrs = Functions.mapToList(Arrays.asList(NEEDED_ATTRIBUTES),
        attrName -> sequenceRecordClass.getAttributeField(attrName).orElseThrow());
    try (FileBasedRecordStream records = new FileBasedRecordStream(_baseAnswer, neededAttrs, Collections.EMPTY_LIST)) {
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
  
      for (RecordInstance record : records) {
        // output source id
        String fullId = record.getAttributeValue(ATTR_FULL_ID).getValue().toString().trim();
        writer.print(">" + fullId);

        // output organism if selected
        if (_includeOrganism) {
          String organism = record.getAttributeValue(ATTR_ORGANISM).getValue().toString().trim();
          writer.print(" | organism=" + organism);
        }

        // output description if selected
        if (_includeDescription) {
          Object value = record.getAttributeValue(ATTR_DESCRIPTION).getValue();
          String description = (value == null) ? "" : value.toString().trim();
          writer.print(" | " + description);
        }

        // output ortholog group if selected
        if (_includeGroup) {
	    Object value = record.getAttributeValue(ATTR_GROUP).getValue();
	    String group = (value == null) ? "no group" : value.toString().trim();
	    writer.print(" | " + group);
	}

        writer.println();

        // output sequence
        String sequence = record.getAttributeValue(ATTR_SEQUENCE).getValue().toString();
        sequence = format(sequence);
        writer.println(sequence);

        writer.println();
        writer.flush();
      }
    }
    catch (WdkUserException e) {
      throw new WdkModelException("Unable to produce FASTA report", e);
    }
  }

  private static String format(String sequence) {
    StringBuilder buffer = new StringBuilder();
    int charsInLine = 0;
    for (int i = 0; i < sequence.length(); i++) {
      char c = sequence.charAt(i);
      if (c != '\n') {
        buffer.append(c);
        charsInLine++;
      }
      if (charsInLine == FASTA_LINE_LENGTH) {
        buffer.append('\n');
        charsInLine = 0;
      }
    }
    return buffer.toString();
  }
}
