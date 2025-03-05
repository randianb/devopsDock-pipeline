def parameters = buildInfo.actions.findAll { it.parameters }?.collectMany { it.parameters } ?: []

if (parameters.isEmpty()) {
    error "No parameters found in the last successful build. API response: ${buildInfoJson}"
}