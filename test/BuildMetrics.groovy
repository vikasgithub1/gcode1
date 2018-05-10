import java.text.SimpleDateFormat

/**
 *
 */

def capturePipelineExecutionMetadata( String releaseName, String currentDateTime){
    def buildMetadataMap = [:]
    buildMetadataMap.put("buildNumber",env.BUILD_NUMBER)
    buildMetadataMap.put("jobName",jobName)
    buildMetadataMap.put("releaseName",releaseName)
    buildMetadataMap.put("executionStartDateTime", currentDateTime)
    buildMetadataMap.put("buildPerformed", auditMap.BuildPerformed)

    String buildVers = ""
    for (int j=0; j<auditMap.srcRepos.size(); j++) {
      if (auditMap.BuildPerformed) {
        // same version applies to multiple apps
        buildVers = srcRepoDetails.getAt(j).artifactVersion
      } else {
        buildVers = buildVers + "${auditMap.srcRepos.getAt(j).sourceDir}-${auditMap.srcRepos.getAt(j).artifactVersion}, "
      }
    }
    buildMetadataMap.put("artifactVersion", buildVers)

    metricsMap.put("pipelineMetadata", buildMetadataMap)
}

def capturePipelineStageData(String stageName, String currentDateTime, String status, String environment){
    def stageDataMap
    if(metricsMap.containsKey(stageName)){
        stageDataMap = metricsMap."${stageName}"
        stageDataMap.put("endDateTime",currentDateTime)
        stageDataMap.put("status",status)
    } else{
        stageDataMap = [:]
        stageDataMap.put("stageName",stageName)
        stageDataMap.put("startDateTime",currentDateTime)

        if(environment?.trim())
            stageDataMap.put("environment",environment)
    }
    metricsMap.put(stageName, stageDataMap)
}

def capturePipelineStageEventsData(String stageName,String currentDateTime, String status,String eventName){
    def eventDataMap
    def eventStageDataMap
    if(metricsMap.containsKey(stageName)){
        eventStageDataMap = metricsMap."${stageName}"
        if(eventStageDataMap.containsKey(eventName)){
            eventDataMap = eventStageDataMap."${eventName}"
            eventDataMap.put("endDateTime",currentDateTime)
            eventDataMap.put("status",status)
        }else{
            eventDataMap = [:]
            eventDataMap.put("eventName",eventName)
            eventDataMap.put("startDateTime",currentDateTime)
        }
        eventStageDataMap.put(eventName,eventDataMap)
    }
    metricsMap.put(stageName, eventStageDataMap)

}

def generateBuildMetricsJson(){
    buildMetricsJsonList = []
    isNextEntry = false
    buildMetricsJsonStart = '{"pipelineExecution":{'
    buildMetricsJsonEnd = '}}'
    pipelineStagesJsonStart = '"pipelineStages":['
    pipelineStagesJsonEnd = ']'
    pipelineExecutionData = ""
    pipelineStageDataStart ="{"
    pipelineStageDataEnd ="}"
    pipelineStageEventData=""
    pipelineData = ""
    pipelineStageData=""
    buildMetricsJsonList.add(buildMetricsJsonStart)

    primaryCounter = 0
    for(entry in metricsMap) {
        primaryCounter++
        buildMetricsJsonList.add(processMapData(entry.key, entry.value))
        if(primaryCounter < metricsMap.iterator().size()){
            buildMetricsJsonList.add(",")
        }
        if (!buildMetricsJsonList.contains("${pipelineStagesJsonStart}"))
            buildMetricsJsonList.add("${pipelineStagesJsonStart}")

        if (primaryCounter == metricsMap.iterator().size()) {
            buildMetricsJsonList.add("${pipelineStagesJsonEnd}")
            buildMetricsJsonList.add("${buildMetricsJsonEnd}")
        }
    }
    for(listItem in buildMetricsJsonList){
        pipelineData = pipelineData + listItem
    }
    echo "Final json = ${pipelineData}"
    return pipelineData
}

def getCurrentDateAndTime(){
    def currentDate = new Date()
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    return simpleDateFormat.format(currentDate)
}

def processMapData(String currentKey, def currentMap){
    pipelineJsonData = ""
    pipelineStageEventsList = []
    pipelineStageEventDataStart='"events":['
    pipelineStageEventDataEnd="]"
    counter = 0
    eventCounter = 0

    if(currentKey == "pipelineMetadata"){
        pipelineJsonData = "\"overAllStatus\"" + ":" + "\"${currentBuild.result}\"" + ","
        pipelineJsonData = pipelineJsonData + "\"executionEndDateTime\"" + ":" + "\"" + getCurrentDateAndTime() + "\"" + ","
        for(item in currentMap){
            counter++
            pipelineJsonData = pipelineJsonData + "\"${item.key}\"" + ":" + "\"${item.value}\""
            if(counter < currentMap.iterator().size()){
                pipelineJsonData = pipelineJsonData + ","
            }
        }
    }else{
        pipelineJsonData = pipelineJsonData + pipelineStageDataStart
        if(!currentBuild.result.equals("SUCCESS")){
            currentProcessedMap = processStageMapAndUpdateStatus(currentMap)
        } else{
            currentProcessedMap = currentMap
        }
        for(item in currentProcessedMap){
            counter++
            if(item.value instanceof String){
                pipelineJsonData = pipelineJsonData + "\"${item.key}\"" + ":" + "\"${item.value}\""
                if(pipelineStageEventsList.size() == 0){
                    if(counter < currentProcessedMap.iterator().size()){
                        pipelineJsonData = pipelineJsonData + ","
                    }
                } else{
                    if(counter <= currentProcessedMap.iterator().size()){
                        pipelineJsonData = pipelineJsonData + ","
                    }
                }
            }else if(item.value instanceof Map){
                pipelineStageEventsList.add(item.value)
            }
        }
        if(pipelineStageEventsList.size() > 0){
            pipelineJsonData = pipelineJsonData + pipelineStageEventDataStart
            for(eventListItem in pipelineStageEventsList){
                eventItemCounter = 0
                currentActiveMap = eventListItem
                eventCounter++
                for(eventItem in currentActiveMap){
                    if(eventItemCounter == 0){
                        pipelineJsonData = pipelineJsonData + "{"
                    }
                    eventItemCounter++
                    pipelineJsonData = pipelineJsonData + "\"${eventItem.key}\"" + ":" + "\"${eventItem.value}\""
                    if(eventItemCounter < currentActiveMap.iterator().size())
                        pipelineJsonData = pipelineJsonData + ","
                    if(eventItemCounter == currentActiveMap.iterator().size())
                        pipelineJsonData = pipelineJsonData + "}"
                }
                if(eventCounter < pipelineStageEventsList.size())
                    pipelineJsonData = pipelineJsonData + ","
                if(eventCounter == pipelineStageEventsList.size())
                    pipelineJsonData = pipelineJsonData + pipelineStageEventDataEnd
            }
        }
        pipelineJsonData = pipelineJsonData + pipelineStageDataEnd
    }
    // echo "Pipeline json data = ${pipelineJsonData}"
    return pipelineJsonData
}

def processStageMapAndUpdateStatus(def currentMap){
    echo "processing map to update status"
    unprocessedMap = currentMap
    if(!unprocessedMap.containsKey("status")){
        unprocessedMap.put("status", currentBuild.result)
        for(item in unprocessedMap){
            if(item.value instanceof Map){
                currentKey = item.key
                currentActiveMap = item.value
                if(!currentActiveMap.containsKey("status")){
                    currentActiveMap.put("status", currentBuild.result)
                    break
                }
            }
        }
    }
    return currentMap
}

def processBuildMetricsJson(){
    String buildMetricsJson = generateBuildMetricsJson()
    buildMetricsDataFileName = "${env.BUILD_NUMBER}_buildMetrics.json"
    writeFile file: "${buildMetricsDataFileName}", text: "${buildMetricsJson}"
    year = new SimpleDateFormat("yyyy").format(new Date());
    month = new SimpleDateFormat("MMM").format(new Date());
    copyFilesToNAS("${buildMetricsDataFileName}", "buildmetrics/${year}/${month}")
    sendSplunkFile(excludes: '',
        includes: "${buildMetricsDataFileName}",
        publishFromSlave: true, sizeLimit: '10MB',
        event_tag: "build_metrics",
        sourcetype: "json_data",
        index:"build_metrics")

}

def copyFilesToNAS(String fileName, String destinationFolderName){
    def workspace = pwd()
    if(envMap.type != "prod"){
        sh "mkdir -p ${envMap.linuxNAS}/${jobName}/${destinationFolderName} && cp ${workspace}/${fileName} ${envMap.linuxNAS}/${jobName}/${destinationFolderName}/"
    }
}

return this;
