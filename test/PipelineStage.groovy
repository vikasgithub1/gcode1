/**
 *
 *
 */

def pipelineStageOrchestrator(def valueMap, def environments, def targetEnvs, String stageName, def deployApps) {
    String key = "${stageName}Approval"
    def timeouts = valueMap."${key}"
    String emailkey = "${stageName}EmailList"
    def recipients =  valueMap."${emailkey}"
    String approvedEnvList = deploymentApprovals(environments, targetEnvs.split(","), timeouts, , false, recipients)
    if(approvedEnvList != null && approvedEnvList.length() > 0) {
      buildMetrics.capturePipelineStageData(stageName, utilFunctions.getCurrentDateAndTime(), "", "${approvedEnvList}")
      stage("Deploy to ${stageName.toUpperCase()} environments") {
        buildMetrics.capturePipelineStageEventsData(stageName, utilFunctions.getCurrentDateAndTime(), "", "Deploy")
        deploy.deployGenericArtifacts(valueMap, approvedEnvList, stageName, deployApps)
        buildMetrics.capturePipelineStageEventsData(stageName, utilFunctions.getCurrentDateAndTime(), "SUCCESS", "Deploy")
      }
      stage("Bounce ${stageName.toUpperCase()} environments") {
        buildMetrics.capturePipelineStageEventsData(stageName, utilFunctions.getCurrentDateAndTime(), "", "Bounce")
        def filterBounceEnvs = loadPipelineMetadata.filterAccess(environments, false, approvedEnvList.split(","))
        if(utilFunctions.isValueExist(filterBounceEnvs)) {
          deploy.restartServer(valueMap, filterBounceEnvs, stageName, deployApps)
        }
        def sudoFilterBounceEnvs = loadPipelineMetadata.filterAccess(environments, true, approvedEnvList.split(","))
        echo "Sudo envs ${sudoFilterBounceEnvs}"
        if(utilFunctions.isValueExist(sudoFilterBounceEnvs)) {
          deploy.restartServerUsingSudo(valueMap, sudoFilterBounceEnvs, stageName, deployApps)
        }
        buildMetrics.capturePipelineStageEventsData(stageName, utilFunctions.getCurrentDateAndTime(), "SUCCESS", "Bounce")
      }
	 
		deploy.restartServer(valueMap, approvedEnvList, stageName, valueMap.buildRepos)
	    stage("Execute Smoke Tests On ${stageName.toUpperCase()} environments") {
        integrationTestUtility.executeSmokeTests(valueMap, environments, approvedEnvList.split(","), recipients )
        buildMetrics.capturePipelineStageEventsData(stageName, utilFunctions.getCurrentDateAndTime(), "SUCCESS", "SmokeTest")
      }
      buildMetrics.capturePipelineStageData(stageName, utilFunctions.getCurrentDateAndTime(), "SUCCESS", "${approvedEnvList}")
    }

}

def pipelineStageOrchestratorForRollback(def valueMap, def environments, def targetEnvs, String stageName) {
    String key = "${stageName}Approval"
    def timeouts = valueMap."${key}"
    String emailkey = "${stageName}EmailList"
    def receipients =  valueMap."${emailkey}"
    String approvedEnvList = deploymentApprovals(environments, targetEnvs.split(","), timeouts, true, receipients)
    //If no environments are approved for deployment
    if(approvedEnvList != null && approvedEnvList.length() > 0) {
        deploy.rollback(valueMap, approvedEnvList, stageName)
        //deploy.restartServer(valueMap, approvedEnvList, stageName, valueMap.rollbackRepos)
    }
}

def deploymentApprovals(def environments, def targetEnvs, def approvalTimeout, boolean isRollback, def recipients) {
    def stepsForParallel = [:]
    def filterEnvs = []
    for(int i=0; i<targetEnvs.size(); i++) {
        stepsForParallel[targetEnvs[i]] =
            approval.sendNotificationAndWaitForApproval(targetEnvs[i], environments, filterEnvs, approvalTimeout, isRollback, recipients)
    }
    parallel stepsForParallel
    return filterEnvs.join(",")
}

return this;
