# OrthoMCLModel

In summary, this repository contains xml files with basic data type definitions, searches, attributes, queries, and tables for OrthoMCL.org. The two data types are Sequences and Groups.

** Dependencies

   + yarn / npm / ant
   + WEBAPP_PROP_FILE file (file with one property for the webapp target directory)
      webappTargetDir=BLAH
   + environment variables for GUS_HOME and PROJECT_HOME
   + Internal Dependencies
   + WDK, CBIL, ReFlow, EbrcModelCommon

** Installation instructions.

   + bld OrthoMCLModel
   + changes to wdk model xml files requires reload of tomcat instance

** Operating instructions.

   + Installation is successful if you can run "wdkXml -model $MODEL_NAME" without error

** manifest

   + OrthoMCLModel/MapProteomeService contains mcl but the workflow uses /eupath/workflow-software/bin/mcl
   + OrthoMCLModel/Ppe/lib/perl contains MatrixColumnManager.pm
   + OrthoMCLModel/Model/lib/wdk :: xml files which define wdk records, searches, params.
   + OrthoMCLModel/Model/lib/xml :: tuning manager configuration
   + OrthoMCLModel/Model/src/main/java/org/orthomcl/shared/model/layout :: java code per data class for cluster layout
   + OrthoMCLModel/Model/target/classes/org/orthomcl/shared/model/layout :: java class files for cluster layout
