#!/bin/bash

# Usage: ./logcli.sh "<START_DATE> <START_TIME>" "<END_DATE> <END_TIME>" <TYPE> <JOB_NAME>
if [[ $# -ne 4 ]]; then
    echo "Usage: $0 \"<START_DATE> <START_TIME>\" \"<END_DATE> <END_TIME>\" <TYPE> <JOB_NAME>"
    echo "Example: $0 \"2025-03-04 13:45\" \"2025-03-06 18:30\" service service_name"
    exit 1
fi

# Loki server URL (modify if needed)
LOKI_URL="http://localhost:3100"

START_DATETIME="$1"
END_DATETIME="$2"
TYPE="$3"
JOB_NAME="$4"

SECONDS=('00' '30' '59')

# Convert dates to epoch time (handles both date and time)
START_EPOCH=$(date -d "$START_DATETIME" +%s)
END_EPOCH=$(date -d "$END_DATETIME" +%s)

# Check if Loki is reachable
if ! curl -s --head --fail "$LOKI_URL/loki/api/v1/status/buildinfo" > /dev/null; then
    echo "Error: Unable to reach Loki at $LOKI_URL"
    exit 1
fi

# Iterate through the exact time range
for (( CUR_EPOCH=START_EPOCH; CUR_EPOCH<=END_EPOCH; CUR_EPOCH+=60 )); do
    CUR_DATE=$(date -d "@$CUR_EPOCH" +%Y-%m-%d)
    CUR_TIME=$(date -d "@$CUR_EPOCH" +%H:%M)

    echo "Processing logs for: $CUR_DATE $CUR_TIME"

    for (( S_IDX=0; S_IDX<${#SECONDS[@]}-1; S_IDX++ )); do
        FROM="${CUR_DATE}T${CUR_TIME}:${SECONDS[$S_IDX]}Z"
        TO="${CUR_DATE}T${CUR_TIME}:${SECONDS[$S_IDX + 1]}Z"

        ./logcli --addr="$LOKI_URL" query --from="$FROM" --to="$TO" "{$TYPE=\"$JOB_NAME\"}" \
            --forward --part-path-prefix . --parallel-duration=30s --parallel-max-workers=3 --merge-parts >> complete.log

        sleep 1s  # Reduced sleep to improve efficiency
    done
done

echo "Log extraction completed."
