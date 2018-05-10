/**
 * 
 */
def failureEmailNotification(String subject, String recipients, def error) {
    def emailbody = readFile encoding: 'UTF-8', file: "src/resources/buildfailure.html"
    emailbody = emailbody.replaceAll("<build_url>", "${env.BUILD_URL}")
    emailext (
            attachLog: true,
            body: "${emailbody}",
            mimeType: "text/html",
            replyTo: "${defaultFromAddress}",
            subject: "${subject}",
            to: "${recipients}"
    )
}

def successEmailNotification(String subject, String recipients) {
    def emailbody = readFile encoding: 'UTF-8', file: "src/resources/buildsuccess.html"
    emailbody = emailbody.replaceAll("<build_url>", "${env.BUILD_URL}")
    emailext (
            attachLog: true,
            body: "${emailbody}",
            mimeType: "text/html",
            replyTo: "${defaultFromAddress}",
            subject: "${subject}",
            to: "${recipients}"
    )
}

def sendEmail(String subject, String body, String recipients) {
    emailext(
            attachLog: true,
            body: "${body}",
            mimeType: "text/html",
            replyTo: "${defaultFromAddress}",
            subject: "${subject}",
            to: "${recipients}"
    )
}

def sendEmailWithAttachment(String subject, String body, String recipients, String attachment) {
    emailext(
            attachLog: true,
            attachmentsPattern: "**/${attachment}",
            body: "${body}",
            mimeType: "text/html",
            replyTo: "${defaultFromAddress}",
            subject: "${subject}",
            to: "${recipients}"
    )
}

return this;
