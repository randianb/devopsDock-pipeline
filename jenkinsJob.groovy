def buildInfoJson = sh(
    script: """
    curl -s --user \${JENKINS_USER}:\${JENKINS_TOKEN} \
    "${jenkinsUrl}/${jobPath}/${buildNumber}/api/json?tree=actions[parameters[*]]"
    """,
    returnStdout: true
).trim()

echo "Requesting URL: ${jenkinsUrl}/${jobPath}/${buildNumber}/api/json?tree=actions[parameters[*]]"
