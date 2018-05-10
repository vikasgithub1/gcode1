/**
 *
 */
def loadConfiguration() {
  def valueMap = [:]
  def currentEnvURL = "${env.JENKINS_URL}"
  def environmentConfigFile = readFile encoding: 'UTF-8', file: "src/resources/environment_config.json"
  def environmentConfigText = new groovy.json.JsonSlurperClassic().parseText(environmentConfigFile)
  valueMap.put("artifacoryReleaseLoc", environmentConfigText.artifacoryReleaseLoc)
  valueMap.put("groupId", environmentConfigText.groupId)
  valueMap.put("pipelineRepoName", environmentConfigText.pipelineRepoName)
  valueMap.put("gitTool", environmentConfigText.gitTool)
  valueMap.put("gitTool_win", environmentConfigText.gitTool_win)
  valueMap.put("jdkVersion", environmentConfigText.jdkVersion)
  valueMap.put("mavenVersion", environmentConfigText.mavenVersion)
  valueMap.put("jdkVersion_win", environmentConfigText.jdkVersion_win)
  valueMap.put("mavenVersion_win", environmentConfigText.mavenVersion_win)
  jobName = "${env.JOB_NAME}".split("/").last().replaceAll(" ","_")
  valueMap.put("jobName", jobName)
  auditMap.put("JobName",jobName)
  auditMap.put("pipelineRepoName", environmentConfigText.pipelineRepoName)


  jenkinsEnvs = environmentConfigText.jenkins_envs
  for(int i=0; i < jenkinsEnvs.size(); i++) {
    echo "${currentEnvURL} - ${jenkinsEnvs.getAt(i).jenkins_url}"
    if(new java.net.URL(currentEnvURL).equals(new java.net.URL(jenkinsEnvs.getAt(i).jenkins_url))) {
      valueMap.put("type",jenkinsEnvs.getAt(i).type)
      valueMap.put("artifactServerId",jenkinsEnvs.getAt(i).artifactServerId)
      valueMap.put("icmSymphonyClusterDeployerId",jenkinsEnvs.getAt(i).icmSymphonyClusterDeployerId)
      valueMap.put("ectmcmg2CredentialsId", jenkinsEnvs.getAt(i).ectmcmg2CredentialsId)
      valueMap.put("ectmcmg2CredentialsIdWithPassword", jenkinsEnvs.getAt(i).ectmcmg2CredentialsIdWithPassword)
      valueMap.put("serviceNowAccountName", jenkinsEnvs.getAt(i).serviceNowAccountName)
      valueMap.put("pipelineRepoName", jenkinsEnvs.getAt(i).pipelineRepoName)
      valueMap.put("allowedStages", jenkinsEnvs.getAt(i).allowedStages)
      valueMap.put("linuxNAS", jenkinsEnvs.getAt(i).linuxNAS)
      break;
    }
  }

  return valueMap
}

return this;
