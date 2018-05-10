/**
 *
 */

def info(String message) {
  wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
    echo "\033[1;33m[INFO] ${message}   \033[0m "
  }
}

def error(String message) {
  wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
    echo "\033[1;31m[ERROR]   ${message} \033[0m "
  }
}

def warn(String message) {
  wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
    echo "\033[1;35m[WARN]   ${message} \033[0m "
  }
}

def success(String message) {
  wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
    echo "\033[1;32m[SUCCESS] ${message} \033[0m "
  }
}

def isValueExist(String value) {
  if(value == null || value.trim().length() == 0 || value.equals("\"\"")) {
    return false;
  }
  return true;
}

def getCurrentDateAndTime() {
  def currentDate = new Date()
  java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  return simpleDateFormat.format(currentDate)
}

def getCurrentDate() {
  def currentDate = new Date()
  java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("yyyyMMdd")
  return simpleDateFormat.format(currentDate)
}

def shellCommandOutput(command) {
    def uuid = java.util.UUID.randomUUID()
    def filename = "cmd-${uuid}"
    echo filename
    sh ("${command} > ${filename}")
    def result = readFile(filename).trim()
    sh "rm ${filename}"
    return result
}

def copyFilesTOServiceNow(String fileName){
    unstash 'serviceNowUploads'
    sh "cp ${fileName} ${env.BUILD_NUMBER}_ServiceNowUploads/"
    stash includes: "**/${env.BUILD_NUMBER}_ServiceNowUploads/", name: 'serviceNowUploads'
}


return this;
