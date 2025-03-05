def crumbResponse = sh(
    script: """
    curl -s --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
    "${jenkinsUrl}/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,':',//crumb)"
    """,
    returnStdout: true
).trim()

if (!crumbResponse.contains(":")) {
    error "Failed to retrieve Jenkins crumb. Response: ${crumbResponse}"
}

def crumbHeader = crumbResponse.split(":")[0]
def crumbValue = crumbResponse.split(":")[1]