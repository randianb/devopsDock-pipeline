#!/bin/bash
seconds=('00' '30' '59')
for i in {13..18}
do
  for j in {0..59}
  do
    for k in "${seconds[@]}"
    do
        if [[$j -lt 10 ]]; then
            if [[$k -lt 2]]; then
                ./logcli  query --from="2024-12-09T$i:0$j:${seconds[$k]}Z" --to="2024-12-09T$i:0$j:${seconds[$k + 1]}Z" '{job="instance-name"}' --forward --part-path-prefix .  --parallel-duration=30s --parallel-max-workers=3 --merge-parts >> complete.log
            fi
        else
            if [[$k -lt 2]]; then
                ./logcli  query --from="2024-12-09T$i:$j:${seconds[$k]}Z" --to="2024-12-09T$i:$j:${seconds[$k + 1]}Z" '{job="instance-name"}' --forward --part-path-prefix .  --parallel-duration=30s --parallel-max-workers=3 --merge-parts >> complete.log
            fi
        fi
        sleep 5s
    done
  done
done


≈==================

#!/bin/bash

# Usage: ./script.sh <START_DATE> <END_DATE> <JOB_NAME>
if [[ $# -ne 3 ]]; then
    echo "Usage: $0 <START_DATE> <END_DATE> <JOB_NAME>"
    echo "Example: $0 2025-03-04 2025-03-06 my-job-name"
    exit 1
fi

# Loki server URL (modify if needed)
LOKI_URL="http://localhost:3100"

START_DATE="$1"
END_DATE="$2"
JOB_NAME="$3"
SECONDS=('00' '30' '59')

# Convert dates to epoch time
START_EPOCH=$(date -d "$START_DATE" +%s)
END_EPOCH=$(date -d "$END_DATE" +%s)

# Check if Loki is reachable
if ! curl -s --head --fail "$LOKI_URL/loki/api/v1/status/buildinfo" > /dev/null; then
    echo "Error: Unable to reach Loki at $LOKI_URL"
    exit 1
fi

# Loop over days
for (( DAY_EPOCH=START_EPOCH; DAY_EPOCH<=END_EPOCH; DAY_EPOCH+=86400 )); do
    DATE=$(date -d "@$DAY_EPOCH" +%Y-%m-%d)
    echo "Processing logs for date: $DATE"

    for HOUR in {13..18}; do
        for MINUTE in {0..59}; do
            for (( S_IDX=0; S_IDX<${#SECONDS[@]}-1; S_IDX++ )); do
                FROM="${DATE}T$(printf "%02d:%02d:%02d" "$HOUR" "$MINUTE" "${SECONDS[$S_IDX]}")Z"
                TO="${DATE}T$(printf "%02d:%02d:%02d" "$HOUR" "$MINUTE" "${SECONDS[$S_IDX + 1]}")Z"

                ./logcli --addr="$LOKI_URL" query --from="$FROM" --to="$TO" "{job=\"$JOB_NAME\"}" \
                    --forward --part-path-prefix . --parallel-duration=30s --parallel-max-workers=3 --merge-parts >> complete.log

                sleep 5s
            done
        done
    done
done

echo "Log extraction completed."