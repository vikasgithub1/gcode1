/**
 *
 */

def loadPipleMetadataJson(String metadataFile) {
  def valueMap = [:]
  try {
    def isBuildMetadataFileExists = fileExists "${metadataFile}"
    if (!isBuildMetadataFileExists) {
      error "The " + metadataFile + " build metadata file does not exist in the repository"
      libMap.pipelineLogger.logMessage(libMap.loglevel_ERROR, "The " + metadataFile + " build metadata file does not exist in the repository")
    }
    //read build metadata
    def buildMetadataFile = readFile encoding: 'UTF-8', file: "${metadataFile}"
    stash includes: "${metadataFile}", name: 'buildMetadataJsonFile'
    //parse build meta data file content into json object
    def jsonText = new groovy.json.JsonSlurperClassic().parseText(buildMetadataFile)
    valueMap.put("releaseName", jsonText.releaseName)
    valueMap.put("releaseVersion", jsonText.releaseVersion)
    //valueMap.put("artifactVersion", jsonText.build.artifactVersion)
    valueMap.put("srcRepos", jsonText.build.repos)

    auditMap.put("ReleaseName", jsonText.releaseName)
    auditMap.put("MetadataFileName",metadataFileName)


    //capture build selection
    def buildRepos = []
    def rollbackRepos = []
    def deployRepos = []
    def srcRepos = jsonText.build.repos
    for (int i = 0; i < srcRepos.size(); i++) {
      if (srcRepos.getAt(i).buildFlag == true) {
        buildRepos.add(srcRepos.get(i))
      } else if (srcRepos.getAt(i).rollback.toPrevVersion == true) {
        rollbackRepos.add(srcRepos.get(i))
      } else if (utilFunctions.isValueExist(srcRepos.getAt(i).artifactVersion)) {
        deployRepos.add(srcRepos.get(i))
      }
    }
    valueMap.put("buildRepos", buildRepos)
    valueMap.put("rollbackRepos", rollbackRepos)
    valueMap.put("deployRepos", deployRepos)

    //capture deploy selection

    //environments section
    def environments = jsonText.environments
    def stages = []
    for(int i=0; i < environments.size(); i++) {
      stageName = environments.getAt(i).stage
      if(envMap.allowedStages.contains(stageName)) {
        def targets = environments.getAt(i).targets
        boolean approvalRequired = false
        if (targets.size() > 0) {
          def targetEnvs = []
          for (int j = 0; j < targets.size(); j++) {
            targetEnvs.add(targets.getAt(j).env)
            if (targets.getAt(j).approval) {
              approvalRequired = true
            }
          }
          String environmentNames = targetEnvs.join(",")
          valueMap.put("target_" + stageName, environmentNames)
          valueMap.put("env_" + stageName, targets)
          stages.add(stageName)
        }
        valueMap.put(stageName + "DeploymentRequired", valueMap.containsKey("target_" + stageName))
        valueMap.put(stageName + "DeploymentApprovalRequired", approvalRequired)
      }
      else {
        utilFunctions.error("${stageName} envirnoments are not allowed deploying from ${envMap.type} - ${envMap.jenkins_url} Jenkins environment.")
      }
    }

    //stages population
    valueMap.put("stages", stages)

    //approvals Section
    def timeouts = jsonText.timeouts
    valueMap.put("devApproval", timeouts.devApproval)
    valueMap.put("sitApproval", timeouts.sitApproval)
    valueMap.put("uatApproval", timeouts.uatApproval)
    valueMap.put("artifactPromotionApproval", timeouts.artifactPromotionApproval)

    //notification section
    def notifications = jsonText.notifications
    valueMap.put("defaultEmailList", notifications.devEmailList)
    valueMap.put("devEmailList", notifications.devEmailList)
    valueMap.put("sitEmailList", notifications.sitEmailList)
    valueMap.put("uatEmailList", notifications.uatEmailList)

    //servicenow section
    valueMap.put("servicenow", jsonText.servicenow)

    //validation test repo details
    valueMap.put("testrepos", jsonText.testrepos)

    def scans = jsonText.scans
    if(scans != null) {
        for (int i = 0; i < scans.size(); i++) {
            if (scans.getAt(i).execute) {
                valueMap.put(scans.getAt(i).type, scans.getAt(i))
            }
        }
    }

  }
  catch(failedToLoadMetada) {
    err = failedToLoadMetada
    currentBuild.result = "FAILURE"
    throw err
  }
  return valueMap
}

def filterAccess(def targets, boolean isSudo, String[] filteredEnvs) {
  def envs = []
  def data = []
  for(int i=0; i<filteredEnvs.size(); i++) {
    data.add(filteredEnvs[i])
  }
  for(int i=0;i<targets.size();i++) {
    if(targets.getAt(i).sudo == isSudo && data.contains(targets.getAt(i).env)) {
      envs.add(targets.getAt(i).env)
    }
  }
  println envs.join(',')
  return envs.join(',')
}

return this;
