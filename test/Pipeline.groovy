/**
 * Determine flow of steps need to execute and orchestrate pipeline flow
 * @return
 */
def execute() {
  def valueMap = null
  try {

    loadPipelineMetadata = load 'src/com/freddiemac/a22497/sfta/pipeline/LoadPipelineMetadata.groovy'
    gitUtility = load 'src/com/freddiemac/a22497/sfta/pipeline/GitUtility.groovy'
    gitUpdateUtility = load 'src/com/freddiemac/a22497/sfta/pipeline/GitUpdateUtility.groovy'
    utilFunctions = load 'src/com/freddiemac/a22497/sfta/pipeline/UtilFunctions.groovy'
    serviceNowUtility = load 'src/com/freddiemac/a22497/sfta/pipeline/ServiceNowUtility.groovy'
    build = load 'src/com/freddiemac/a22497/sfta/pipeline/Build.groovy'
    artifactUtility = load 'src/com/freddiemac/a22497/sfta/pipeline/ArtifactUtility.groovy'
    pipelineStage = load 'src/com/freddiemac/a22497/sfta/pipeline/PipelineStage.groovy'
    deploy = load 'src/com/freddiemac/a22497/sfta/pipeline/Deploy.groovy'
    emailNotification = load 'src/com/freddiemac/a22497/sfta/pipeline/EmailNotification.groovy'
    approval = load 'src/com/freddiemac/a22497/sfta/pipeline/Approval.groovy'
    auditMetadataUtility = load 'src/com/freddiemac/a22497/sfta/pipeline/AuditMetadataUtility.groovy'
    scanUtility = load 'src/com/freddiemac/a22497/sfta/pipeline/ScanUtility.groovy'
    integrationTestUtility = load 'src/com/freddiemac/a22497/sfta/pipeline/IntegrationTestUtility.groovy'
    buildMetrics = load 'src/com/freddiemac/a22497/sfta/pipeline/BuildMetrics.groovy'

    auditMap.put("BuildNum", "${env.BUILD_NUMBER}")
    stage("Determine Build Inventory") {
      valueMap = loadPipelineMetadata.loadPipleMetadataJson(pipelineBuildMetadataFilename)
      buildMetrics.capturePipelineStageData("Itinerary", utilFunctions.getCurrentDateAndTime(), "", "")
      serviceNowUtility.createOrUpdateTicketIfValid(valueMap)
      buildMetrics.capturePipelineStageData("Itinerary", utilFunctions.getCurrentDateAndTime(), "SUCCESS", "")
    }
    currentBuild.displayName = "${valueMap.releaseName} (${env.BUILD_NUMBER})"

      if (valueMap.buildRepos != null && valueMap.buildRepos.size() > 0) {
        stage("Build and Publish Artifacts") {
          buildMetrics.capturePipelineStageData("Build", utilFunctions.getCurrentDateAndTime(), "", "")
          gitUtility.checkoutSourceRepos(valueMap.buildRepos, envMap.gitTool)
          build.buildGenericPackages(valueMap)
          buildMetrics.capturePipelineStageData("Build", utilFunctions.getCurrentDateAndTime(), "SUCCESS", "")
          if (valueMap.containsKey("sonar")) {
             echo "before running sonar scan"
             scanUtility.runSonarScan(valueMap)
             echo "completed running sonar scan"
          }
          if (valueMap.containsKey("fortify")) {
             echo "before running fortify scan"
             scanUtility.runFortifyScan(valueMap)
             echo "completed running fortify scan"
          }
          //build.copyAndTarArtifacts(valueMap)
          artifactUtility.uploadArtifacts(valueMap)
      }

        // loop thru allowed deployment stages and execute in sequential order
        triggerDeploy(valueMap, valueMap.buildRepos, true)
        auditMap.put("srcRepos", valueMap.buildRepos)
        auditMap.put("BuildPerformed", true)

      } else if(valueMap.rollbackRepos != null && valueMap.rollbackRepos.size() > 0) {
        triggerDeploy(valueMap, valueMap.rollbackRepos, false)
      } else if(valueMap.deployRepos != null && valueMap.deployRepos.size() > 0) {
        gitUtility.checkoutSourceRepos(valueMap.deployRepos, envMap.gitTool)
        //artifactUtility.downloadArtifacts(valueMap)
        triggerDeploy(valueMap, valueMap.deployRepos, true)
        auditMap.put("srcRepos", valueMap.deployRepos)
        auditMap.put("BuildPerformed", false)

      }
    currentBuild.result = "SUCCESS"
    echo "AuditMap - ${auditMap}"
    if(envMap.type != "prod") {
      artifactUtility.artifactPromotion(valueMap)
    }
    auditMetadataUtility.generateAuditReport(valueMap)
    buildMetrics.capturePipelineExecutionMetadata(valueMap.releaseName, utilFunctions.getCurrentDateAndTime())
    buildMetrics.processBuildMetricsJson()

    emailNotification.successEmailNotification(
      "${valueMap.releaseName} Pipeline is Successful for Job# ${env.BUILD_NUMBER}",
      valueMap.defaultEmailList)
  }
  catch (pipelineErr) {
    currentBuild.result = "FAILURE"
      emailNotification.failureEmailNotification(
      "${valueMap.releaseName} Pipeline Failed for Job# ${env.BUILD_NUMBER}",
      valueMap.defaultEmailList,
      pipelineErr)
    libMap.pipelineLogger.logMessage(libMap.loglevel_ERROR, "${valueMap.releaseName} Pipeline Failed for Job# ${env.BUILD_NUMBER}")
    throw pipelineErr
  }
}

def triggerDeploy(def valueMap, def repos, boolean isDeploy) {
  for(int i=0; i<envMap.allowedStages.size(); i++) {
    String stageName = envMap.allowedStages.getAt(i)
    stage("Deploy to ${stageName.toUpperCase()} environments") {
      String deploymentKey = "${stageName}DeploymentRequired"
      if (valueMap."${deploymentKey}" != null && valueMap."${deploymentKey}") {
        String targetKey = "target_${stageName}"
        String envKey = "env_${stageName}"
        if (isDeploy) {
          pipelineStage.pipelineStageOrchestrator(
            valueMap,
            valueMap."${envKey}",
            valueMap."${targetKey}",
            stageName,
            repos)
        } else {
          pipelineStage.pipelineStageOrchestratorForRollback(
            valueMap,
            valueMap."${envKey}",
            valueMap."${targetKey}",
            stageName)
        }
      }
    }
  }
}

return this;
