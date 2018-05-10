
def executeSmokeTests(def valueMap, def environments, String[] targets, def receipients){
    def emailLinkUrl
    def isSmokeTestsExecuted = false
    
    node('windows-343') {
        deleteDir()
        def workspace = pwd()
        gitUtility.checkoutSingleRepo(valueMap.testrepos, envMap.gitTool_win)
        dir("${valueMap.testrepos.sourceDir}"){
            for (int t = 0; t < targets.size(); t++) {
                boolean executeTests = false
                for (int i = 0; i < environments.size(); i++) {
                    if (targets[t].equals(environments.getAt(i).env)) {
                        executeTests = environments.getAt(i).executeTests
                        break
                    }
                }
                if (executeTests) {
                    echo 'Waiting 30 minutes for deployment to complete prior starting smoke testing'
    		    sleep 2500
                    def M2_HOME = "${tool envMap.mavenVersion_win}"
                    withEnv(["JAVA_HOME=${tool envMap.jdkVersion_win}", "M2_HOME=${tool envMap.mavenVersion_win}"]) {
                        bat "${M2_HOME}\\bin\\mvn -f NtadPhoenix\\pom.xml clean install && exit %%ERRORLEVEL%%"
                    }
                    def reportDir = "NtadPhoenix/target/cucumber_reports/cucumber-html-reports/"
                    def reportName = "${targets[t].toUpperCase()} Smoke Test Results"
                    def emailLink = "${targets[t].toUpperCase()}_Smoke_Test_Results"
                    def serviceNowUploadZipName = "${targets[t].toUpperCase()}_Smoke_Test_Results.zip"
                    if(!emailLinkUrl?.trim()){
                        emailLinkUrl = "${env.BUILD_URL}$emailLink <br>"
                    } else{
                        emailLinkUrl = emailLinkUrl + "${env.BUILD_URL}$emailLink <br>"
                    }
                    publishHTML(target: [
                            allowMissing         : false,
                            alwaysLinkToLastBuild: false,
                            keepAll              : true,
                            reportDir            : reportDir,
                            reportFiles          : "feature-overview.html",
                            reportName           : "${reportName}"
                    ])
                    unstash 'serviceNowUploads'
                    bat "zip -qr ${serviceNowUploadZipName} NtadPhoenix\\target\\cucumber_reports\\cucumber-html-reports\\*"
                    bat "copy ${serviceNowUploadZipName} ${env.BUILD_NUMBER}_ServiceNowUploads\\${serviceNowUploadZipName}"
                    stash includes: "**\\${env.BUILD_NUMBER}_ServiceNowUploads\\", name: 'serviceNowUploads'
                    isSmokeTestsExecuted = true
                }
            }
        }
    }
    if(isSmokeTestsExecuted) {
        emailbody = "Hello,<br><br>${targets} smoke test is success. Please find the smoke test results at below url <br> ${emailLinkUrl} <br><br>Thank You<br>SFTA Pipeline"
        emailNotification.sendEmail(
                "SFTA SmokeTest Success",
                emailbody,
                receipients)
    }
}

def executeRegressionTests(def valueMap, def environments, String[] targets, def receipients) {
  def isRegressionTestsExecuted = false
  def emailLinkUrl
  node('TA') {
      deleteDir()
      def workspace = pwd()
      gitUtility.checkoutSingleRepo(valueMap.testrepos, envMap.gitTool_win)
      dir("${valueMap.testrepos.sourceDir}"){
          for (int t = 0; t < targets.size(); t++) {
              boolean executeTests = false
              for (int i = 0; i < environments.size(); i++) {
                  if (targets[t].equals(environments.getAt(i).env)) {
                      executeTests = environments.getAt(i).executeTests
                      break
                  }
              }
              if (executeTests) {
                  def M2_HOME = "${tool envMap.mavenVersion_win}"
                  withEnv(["JAVA_HOME=${tool envMap.jdkVersion_win}", "M2_HOME=${tool envMap.mavenVersion_win}"]) {
                      bat "${M2_HOME}\\bin\\mvn -f NtadSfta\\pom.xml clean install && exit %%ERRORLEVEL%%"
                  }
                  def reportDir = "NtadSfta/target/cucumber_reports/cucumber-html-reports/"
                  def reportName = "${targets[t].toUpperCase()} Regression Test Results"
                  def emailLink = "${targets[t].toUpperCase()}_Regression_Test_Results"
                  def serviceNowUploadZipName = "${targets[t].toUpperCase()}_Regression_Test_Results.zip"
                  if(!emailLinkUrl?.trim()){
                      emailLinkUrl = "${env.BUILD_URL}$emailLink <br>"
                  } else{
                      emailLinkUrl = emailLinkUrl + "${env.BUILD_URL}$emailLink <br>"
                  }
                  publishHTML(target: [
                          allowMissing         : false,
                          alwaysLinkToLastBuild: false,
                          keepAll              : true,
                          reportDir            : reportDir,
                          reportFiles          : "feature-overview.html",
                          reportName           : "${reportName}"
                  ])
                  unstash 'serviceNowUploads'
                  bat "zip -qr ${serviceNowUploadZipName} NtadSfta\\target\\cucumber_reports\\cucumber-html-reports\\*"
                  bat "copy ${serviceNowUploadZipName} ${env.BUILD_NUMBER}_ServiceNowUploads\\${serviceNowUploadZipName}"
                  stash includes: "**\\${env.BUILD_NUMBER}_ServiceNowUploads\\", name: 'serviceNowUploads'
                  isRegressionTestsExecuted = true
              }
          }
      }
  }
  if(isRegressionTestsExecuted) {
      emailbody = "Hello,<br><br>${targets} regression test is success. Please find the smoke test results at below url <br> ${emailLinkUrl} <br><br>Thank You<br>SFTA Pipeline"
      emailNotification.sendEmail(
              "SFTA Regression Test Success",
              emailbody,
              receipients)
  }

}

 return this;
