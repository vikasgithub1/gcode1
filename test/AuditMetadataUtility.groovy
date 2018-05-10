/**
 *
 */

def generateAuditReport(def valueMap) {
    String auditReportData = readFile encoding: 'UTF-8', file: "src/resources/auditreport.html"
    auditReportData = validateAuditData("<%=BuildNum%>", auditMap.BuildNum, auditReportData)
    auditReportData = validateAuditData("<%=JobName%>", auditMap.JobName, auditReportData)
    auditReportData = validateAuditData("<%=PipelineRepoName%>", auditMap.pipelineRepoName, auditReportData)
    auditReportData = validateAuditData("<%=PipelineBranchName%>", auditMap.BranchName, auditReportData)
    auditReportData = validateAuditData("<%=MetadataFileName%>", auditMap.MetadataFileName, auditReportData)
    auditReportData = validateAuditData("<%=ReleaseName%>", auditMap.ReleaseName, auditReportData)
    auditReportData = validateAuditData("<%=ChangeTicket%>", auditMap.ChangeTicket, auditReportData)
    auditReportData = validateAuditData("<%=ScheduledStartDateAndTime%>", auditMap.ScheduledStartDateAndTime, auditReportData)
    srcRepoDetails = auditMap.srcRepos
    String reposRecord = ""
    String buildVers = ""
    for(int j=0; j<srcRepoDetails.size(); j++){
        if(utilFunctions.isValueExist(srcRepoDetails.getAt(j).commitPoint)){
            reposRecord = reposRecord + "<tr><td>${srcRepoDetails.getAt(j).repoName}</td><td>${srcRepoDetails.getAt(j).branchOrTagname}</td><td>${srcRepoDetails.getAt(j).commitPoint}</td></tr>"
        }else{
        reposRecord = reposRecord + "<tr><td>${srcRepoDetails.getAt(j).repoName}</td><td>${srcRepoDetails.getAt(j).branchOrTagname}</td><N/A></tr>"
        }

        if (auditMap.BuildPerformed) {
          // same version applies to multiple apps
          buildVers = srcRepoDetails.getAt(j).artifactVersion
        } else {
          buildVers = buildVers + "${srcRepoDetails.getAt(j).sourceDir} - ${srcRepoDetails.getAt(j).artifactVersion}<br/>"
        }
    }
    auditReportData = validateAuditData("<%=AppDetails%>", reposRecord, auditReportData)
    auditReportData = validateAuditData("<%=BuildPerformed%>", auditMap.BuildPerformed.toString(), auditReportData)
    auditReportData = validateAuditData("<%=ArtifactoryVersion%>", buildVers, auditReportData)
    String auditEnv = ""
    if(auditMap.approverList) {
        def approverList = auditMap.approverList
        String record = ""
        for (int i = 0; i < approverList.size(); i++) {
            record = record + "<tr><td>${approverList.getAt(i).env}</td><td>${approverList.getAt(i).user}</td><td>${approverList.getAt(i).date}</td></tr>"
            if(!auditEnv?.trim()){
                auditEnv = "${approverList.getAt(i).env}"
            }else{
                auditEnv = auditEnv + "," + "${approverList.getAt(i).env}"
            }
        }
        auditReportData = validateAuditData("<%=deployments%>", record, auditReportData)
    }else{
        record = "<tr><td>N/A</td><td>N/A</td><td>N/A</td></tr>"
        auditReportData = validateAuditData("<%=deployments%>", record, auditReportData)
    }
    String envRecord = ""
    if(!auditEnv?.trim()){
        envRecord = "<tr><td>N/A</td><td>N/A</td><td>N/A</td><td>N/A</td></tr>"
    }else{
        def hostInfo = utilFunctions.shellCommandOutput("python ansible_scripts/loadInventory.py ${auditEnv}")
        def inventoryJson = new groovy.json.JsonSlurperClassic().parseText(hostInfo)
        echo "${inventoryJson}"
        String[] envs = auditEnv.split(",")
        echo "${envs}"
        for(int i=0; i<envs.size(); i++) {
            def hosts = inventoryJson.get(envs[i])
            envRecord = envRecord + "<tr><td>${envs[i]}</td>"
            for(int h=0 ; h<hosts.size(); h++){
                echo "${envs[i]} - ${hosts.getAt(h)}"
                envRecord = envRecord + "<td>${hosts.getAt(h).name}@${hosts.getAt(h).ip}</td>"
            }
            envRecord = envRecord + "</tr>"
        }
    }
    auditReportData = validateAuditData("<%=HostDetails%>", envRecord, auditReportData)
    if(valueMap.containsKey("PromotionApproved")){
        promotionRecord = "<tr><td>${valueMap.PromotionApproved}</td><td>${valueMap.PromotionApprovedBy}</td></tr>"
        auditReportData = validateAuditData("<%=PromoteDetails%>", promotionRecord, auditReportData)
    }else{
        promotionRecord = "<tr><td>N/A</td><td>N/A</td</tr>"
        auditReportData = validateAuditData("<%=PromoteDetails%>", promotionRecord, auditReportData)
    }
    writeFile file: "auditReport_${env.BUILD_NUMBER}.html", text: "${auditReportData}"
    archive "auditReport_${env.BUILD_NUMBER}.html"
    publishHTML(target: [
            allowMissing: false,
            alwaysLinkToLastBuild: false,
            keepAll: true,
            reportDir: '',
            reportFiles: "auditReport_${env.BUILD_NUMBER}.html",
            reportName: "Audit Report"])
    echo "$valueMap"
    utilFunctions.copyFilesTOServiceNow("auditReport_${env.BUILD_NUMBER}.html")
    unstash 'serviceNowUploads'
    def serviceNowUploadFiles = "${env.BUILD_NUMBER}_ServiceNowUploads"+".zip"
    sh "zip -qr ${serviceNowUploadFiles} ${workspace}/${env.BUILD_NUMBER}_ServiceNowUploads"
    if(valueMap.isServiceNowUploadRequired){
        serviceNowUtility.uploadFilesToServiceNowTicket(valueMap.ticketId, valueMap.ticketType, serviceNowUploadFiles)
    }
}

@NonCPS
def renderTemplate(input, binding) {
    def engine = new groovy.text.SimpleTemplateEngine()
    def template = engine.createTemplate(input)
//    template.make(binding).writeTo( new File("auditReport.html").withWriter('UTF-8'))
    String data = ""
    if(template) {
        data = template.make(binding).toString()
    }
    template = null
    engine = null
    return data
}

def validateAuditData(String param, String paramValue, String auditReportData) {
    if(!paramValue?.trim()){
        paramValue = "N/A"
    }
    auditReportData = auditReportData.replaceAll(param, paramValue)
    return  auditReportData
}

def addApproverToAuditMap(String environment, String approverName) {
    def approverDetails = [:]
    approverDetails.put("user", approverName)
    approverDetails.put("env", environment)
    approverDetails.put("date", utilFunctions.getCurrentDateAndTime())
    if(!auditMap.containsKey("approverList")) {
        def approverList = []
        auditMap.put("approverList", approverList)
    }
    auditMap.approverList.add(approverDetails)
}

@NonCPS
def getApproveUser(){
    def latestApprName = null
    def acts = currentBuild.rawBuild.getAllActions()
    for (act in acts) {
        if (act instanceof org.jenkinsci.plugins.workflow.support.steps.input.ApproverAction) {
            latestApprName = act.userName
        }
    }
    return latestApprName
}

return this;
