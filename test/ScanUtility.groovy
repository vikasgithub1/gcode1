def runSonarScan(def valueMap) {

  def sonarProps = [:]
  sonarProps.put("sonar.userHome","${env.SonarCache}")
  sonarProps.put("sonar.projectVersion","${valueMap.releaseVersion}")
  sonarProps.put("sonar.sourceEncoding","UTF-8")
  sonarProps.put("sonar.language", "java")
  sonarProps.put("sonar.java.target", "1.8")
  sonarProps.put("sonar.java.source", "1.8")
  sonarProps.put("sonar.verbose", "true")

  for(int i=0; i<valueMap.buildRepos.size(); i++) {
    def repo = valueMap.buildRepos.getAt(i)
    if(repo.sourceDir.equals("process")) {
      runProcessRepoSonar(repo, sonarProps, valueMap)
    } else {
      sonarProps.put("sonar.projectKey","${valueMap.sonar.projectKey}_${repo.sourceDir}")
      sonarProps.put("sonar.projectName","${valueMap.sonar.projectName}_${repo.sourceDir}")
      sonarProps.put("sonar.sources", "sfta_${repo.sourceDir}/${repo.sourceDir}/src/main/java")
      sonarProps.put("sonar.java.binaries", "sfta_${repo.sourceDir}/${repo.sourceDir}/target/classes")
      def sonarScanScript = convertToJavaOptions(sonarProps)
      echo "${sonarScanScript}"

      withEnv([
              "PATH=/usr/local/bin:/bin:/usr/bin:/usr/atria/bin",
              "JAVA_HOME=${tool envMap.jdkVersion}",
              "PATH+MAVEN=${tool envMap.mavenVersion}/bin:${env.JAVA_HOME}/bin",
              "BUILD_LABEL=${env.BUILD_NUMBER}",
              "RELEASE_VERSION=${valueMap.releaseVersion}",
      ]) {
          sh "${env.SONAR_RUNNER_linux}/sonar-scanner -Dsonar.userHome=${env.SonarCache} ${sonarScanScript}"
      }
    }
  }
}

def runProcessRepoSonar(def processRepo, def sonarProps, def valueMap) {
  def processApps = ["pu", "po", "dl", "ms", "ad", "ri", "rs", "etl", "lm", "li", "se", "sc", "re", "hf", "rc", "va", "cr"]
  for (int i = 0; i < processApps.size(); i++) {
    sonarProps.put("sonar.projectKey","${valueMap.sonar.projectKey}_${processRepo.sourceDir}.${processApps.get(i)}")
    sonarProps.put("sonar.projectName","${valueMap.sonar.projectName}_${processRepo.sourceDir}.${processApps.get(i)}")
    sonarProps.put("sonar.sources", "sfta_${processRepo.sourceDir}/${processRepo.sourceDir}/${processApps.get(i)}/src")
    sonarProps.put("sonar.java.binaries", "sfta_${processRepo.sourceDir}/${processRepo.sourceDir}/${processApps.get(i)}/target/classes")
    def sonarScanScript = convertToJavaOptions(sonarProps)
    echo "${sonarScanScript}"
    withEnv([
            "PATH=/usr/local/bin:/bin:/usr/bin:/usr/atria/bin",
            "JAVA_HOME=${tool envMap.jdkVersion}",
            "PATH+MAVEN=${tool envMap.mavenVersion}/bin:${env.JAVA_HOME}/bin",
            "BUILD_LABEL=${env.BUILD_NUMBER}",
            "RELEASE_VERSION=${valueMap.releaseVersion}",
    ]) {
        sh "${env.SONAR_RUNNER_linux}/sonar-scanner -Dsonar.userHome=${env.SonarCache} ${sonarScanScript}"
    }
  }

}

def runFortifyScan(def valueMap) {

  for(int i=0; i<valueMap.buildRepos.size(); i++) {
    def repo = valueMap.buildRepos.getAt(i)
    def srcDirs = ""
    def dependDirs = ""
    if(repo.sourceDir.equals("process")) {
      srcDirs = "sfta_${repo.sourceDir}/${repo.sourceDir}/**/src/main/java"
      dependDirs = "sfta_${repo.sourceDir}/${repo.sourceDir}/**/target/**/WEB-INF/lib/*.jar"
    } else {
      srcDirs = "sfta_${repo.sourceDir}/${repo.sourceDir}/src/main/java"
      dependDirs = "sfta_${repo.sourceDir}/${repo.sourceDir}/target/${repo.sourceDir}/WEB-INF/lib/*.jar"
    }
    withEnv([
      "PATH=/usr/local/bin:/bin:/usr/bin:/usr/atria/bin",
      "JAVA_HOME=${tool envMap.jdkVersion}",
      "PATH+MAVEN=${tool envMap.mavenVersion}/bin:${env.JAVA_HOME}/bin",
      "PATH+FORTIFY_HOME=/fmac/test/ts/tools/fortify/HP-Fortify-3.20-Analyzers_and_Apps/bin",
      "M2_DIR=/fmac/test/ts/Jenkins-slave/MavenRepository"
      ]) {
        def FORTIFY_CONFIG_HOME = "${env.FORTIFY_LINUX}/../Core/config"
        def FORTIFY_REPORTS_HOME = "${FORTIFY_CONFIG_HOME}/reports"
        sh "${env.FORTIFY_LINUX}/sourceanalyzer -b A22497_SFTA_FORTIFY_${repo.sourceDir} -clean"
        sh "${env.FORTIFY_LINUX}/sourceanalyzer -b A22497_SFTA_FORTIFY_${repo.sourceDir} \"-machine-output\" -Xmx1500M -jdk 1.8 -cp ${dependDirs} ${srcDirs}"
        sh "${env.FORTIFY_LINUX}/sourceanalyzer -b A22497_SFTA_FORTIFY_${repo.sourceDir} -Xmx1500M \"-machine-output\" \"-format\" \"fpr\" -f A22497_SFTA_FORTIFY-${repo.sourceDir}.fpr -scan"
        sh "${env.FORTIFY_LINUX}/ReportGenerator -format xml -f A22497_SFTA_FORTIFY-${repo.sourceDir}.xml -source A22497_SFTA_FORTIFY-${repo.sourceDir}.fpr -template ${FORTIFY_REPORTS_HOME}/DefaultReportDefinition.xml"
        sh "${env.FORTIFY_LINUX}/ReportGenerator -format pdf -f A22497_SFTA_FORTIFY-${repo.sourceDir}.pdf -source A22497_SFTA_FORTIFY-${repo.sourceDir}.fpr -template ${FORTIFY_REPORTS_HOME}/DefaultReportDefinition.xml"
        //send email of fortify results
        def emailbody = readFile encoding: 'UTF-8', file: "src/resources/fortifyresults.html"
          emailbody = emailbody.replaceAll("<build_url>", "${env.BUILD_URL}")
          emailNotification.sendEmailWithAttachment(
          "Fortify Results for Job# ${env.BUILD_NUMBER}",
          emailbody,
          valueMap.devEmailList,
          "A22497_SFTA_FORTIFY-${repo.sourceDir}.pdf")
      }
    }
}

def convertToJavaOptions(def sonarMap) {
    String script = ""
    for( it in sonarMap) {
        script = script + " -D${it.key}=${it.value}"
    }
    return script
}

return this;
