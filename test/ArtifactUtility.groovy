/**
 * Upload artifacts
 * @param valueMap - Build environment values
 */
def uploadArtifacts(def valueMap) {
   //Initialize artifcatory server instance
    def artifactoryServer = Artifactory.server(envMap.artifactServerId)
    def buildInfo = Artifactory.newBuildInfo()
    buildInfo.setName "${envMap.jobName}"
    buildInfo.setNumber "${valueMap.releaseVersion}-${env.BUILD_NUMBER}"

    def uploadSpec =
            buildGenericUploadSpec(
                "${envMap.artifacoryReleaseLoc}",
                "${valueMap.releaseVersion}-${env.BUILD_NUMBER}",
                "${envMap.groupId}")
    artifactoryServer.upload(uploadSpec, buildInfo)
    artifactoryServer.publishBuildInfo(buildInfo)
    //valueMap.put("artifactVersion", "${valueMap.releaseVersion}-${env.BUILD_NUMBER}")
}

def buildGenericUploadSpec(String targetLocation, String version, String groupId) {
    def uploadSpec =
        groovy.json.JsonOutput.toJson(files:[
            [
              pattern : "sfta_process/process/distribution/phx-process1-" + version + ".tar",
              target  : targetLocation + "/com/freddiemac/phx/sfta/phx-sfta-process-root1/phx-process1/" + version + "/",
              recursive: false
            ],
            [
              pattern : "sfta_process/process/distribution/phx-process2-" + version + ".tar",
              target  : targetLocation + "/com/freddiemac/phx/sfta/phx-sfta-process-root2/phx-process2/" + version + "/",
              recursive: false
            ],
            [
              pattern : "sfta_gui/gui/target/phx-gui-" + version + ".tar",
              target  : targetLocation + "/com/freddiemac/phx/sfta/gui/phx-gui/" + version + "/",
              recursive: false
            ],
            [
              pattern : "sfta_orchestrator/orchestrator/target/phx-orchestrator-" + version + ".tar",
              target  : targetLocation + "/com/freddiemac/phx/sfta/orchestrator/phx-orchestrator/" + version + "/",
              recursive: false
            ],
            [
              pattern : "sfta_report/report/target/phx-report-" + version + ".tar",
              target  : targetLocation + "/com/freddiemac/phx/report/phx-report/" + version + "/",
              recursive: false
            ],
            [
              pattern : "sfta_service/service/cdw-service/target/phx-cdw-service-" + version + ".tar",
              target  : targetLocation + "/com/freddiemac/phx/shared/cdw/phx-cdw-service/" + version + "/",
              recursive: false
            ],
            [
                    pattern : "sfta_service/service/clean-service/target/phx-clean-service-" + version + ".tar",
                    target  : targetLocation + "/com/freddiemac/phx/shared/clean/phx-clean-service/" + version + "/",
                    recursive: false
            ],
            [
              pattern : "sfta_service/service/prds-service/target/prds-service-" + version + ".tar",
              target  : targetLocation + "/com/freddiemac/phx/shared/service/prds-service/" + version + "/",
              recursive: false
            ],
            [
              pattern : "sfta_service/service/sfta-tds-service/target/phx-sfta-tds-service-" + version + ".tar",
              target  : targetLocation + "/com/freddiemac/phx/shared/sfta/tds/phx-sfta-tds-service/" + version + "/",
              recursive: false
            ],
            [
              pattern : "sfta_cache/cache/target/phx-cache-" + version + ".tar",
              target  : targetLocation + "/com/freddiemac/phx/cache/phx-cache/" + version + "/",
              recursive: false
            ]

        ])
    return uploadSpec
}

def downloadArtifacts(def valueMap) {
  def artifactoryServer = Artifactory.server(envMap.artifactServerId)
  def downloadParallel = [:]
  for(int i=0; i<valueMap.deployRepos.size(); i++) {
    downloadParallel[valueMap.deployRepos.getAt(i).sourceDir] = downloadParallelRepos(valueMap.deployRepos.getAt(i), artifactoryServer, "${envMap.artifacoryReleaseLoc}")
  }
  downloadParallel.failFast = true
  parallel downloadParallel
}

def downloadParallelRepos(def repo, def artifactoryServer, String targetLocation) {
  def downloadSpec
  def localLoc = pwd()
  echo "localloc ${localLoc}"
  return {
    if(repo.sourceDir.equals("cache")) {
      downloadSpec =
          groovy.json.JsonOutput.toJson(files:[
              [
                pattern : targetLocation + "/com/freddiemac/A22497/NTAD/" + repo.sourceDir + "/" + repo.artifactVersion + "/" + repo.sourceDir + ".war",
                target  : localLoc + "/ntad_build/cache/",
                flat    : true
              ]

          ])
    } else if(repo.sourceDir.equals("process")) {
      downloadSpec =
          groovy.json.JsonOutput.toJson(files:[
              [
                pattern : targetLocation + "/com/freddiemac/A22497/NTAD/" + repo.sourceDir + "/" + repo.artifactVersion + "/sfta-process-" + repo.artifactVersion + ".tar.gz",
                target  : localLoc + "/ntad_build/process/",
                flat    : true
              ]

          ])
    } else {
      downloadSpec =
          groovy.json.JsonOutput.toJson(files:[
              [
                pattern : targetLocation + "/com/freddiemac/A22497/NTAD/" + repo.sourceDir + "/" + repo.artifactVersion + "/" + repo.sourceDir + ".war",
                target  : localLoc + "/ntad_build/ui/",
                flat    : true
              ]

          ])
    }
    artifactoryServer.download(downloadSpec)
  }
}

def artifactPromotion(def valueMap) {
  String inputMessage = "Artifact Promotion Approval Request"
  def promotTimeouts = valueMap.artifactPromotionApproval
  approval.promotionApproval(inputMessage, promotTimeouts, valueMap, "${promotTimeouts.approverGroup}")
  def uniqueVerList = []
  if(auditMap.BuildPerformed) {
    // same build version with multiple repos/apps
    uniqueVerList.add(auditMap.srcRepos.getAt(0).artifactVersion)
  } else {
    for (int i=0; i<auditMap.srcRepos.size(); i++) {
      uniqueVerList.add(auditMap.srcRepos.getAt(i).artifactVersion)
    }
  }
  def uniqueVerSet = uniqueVerList.toSet()
  if(valueMap.PromotionApproved) {
      def server = Artifactory.server(envMap.artifactServerId)
      uniqueVerSet.each { it ->
      //for (item in uniqueVerSet) {
        def promotionConfig = [
                'buildName'  : "${envMap.jobName}".toString(),
                'buildNumber': "${it}".toString(),
                'targetRepo' : "fm-staging-local".toString(),
                'sourceRepo' :  "${envMap.artifacoryReleaseLoc}".toString(),
                'comment'    : "Promoting ${envMap.jobName}@${it} for production deployment".toString(),
                'copy'       : true,
                'failFast'   : true
        ]
        server.promote(promotionConfig)
      }
  }
}


return this;
