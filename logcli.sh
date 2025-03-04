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

START_DATE="$1"
END_DATE="$2"
JOB_NAME="$3"
SECONDS=('00' '30' '59')

# Convert dates to epoch time for iteration
START_EPOCH=$(date -d "$START_DATE" +%s)
END_EPOCH=$(date -d "$END_DATE" +%s)

# Loop over days
for (( DAY_EPOCH=START_EPOCH; DAY_EPOCH<=END_EPOCH; DAY_EPOCH+=86400 )); do
    DATE=$(date -d "@$DAY_EPOCH" +%Y-%m-%d)
    echo "Processing logs for date: $DATE"

    for HOUR in {13..18}; do
        for MINUTE in {0..59}; do
            for (( S_IDX=0; S_IDX<${#SECONDS[@]}-1; S_IDX++ )); do
                FROM="${DATE}T$(printf "%02d" "$HOUR"):$(printf "%02d" "$MINUTE"):${SECONDS[$S_IDX]}Z"
                TO="${DATE}T$(printf "%02d" "$HOUR"):$(printf "%02d" "$MINUTE"):${SECONDS[$S_IDX + 1]}Z"

                ./logcli query --from="$FROM" --to="$TO" "{job=\"$JOB_NAME\"}" --forward --part-path-prefix . --parallel-duration=30s --parallel-max-workers=3 --merge-parts >> complete.log
                
                sleep 5s
            done
        done
    done
done

echo "Log extraction completed."