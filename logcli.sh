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

./logcli.sh 2025-03-02 2025-03-03 abb-unibank-service-bridge-int
Processing logs for date: 2025-03-02
Unable to parse time parsing time "2025-03-02T13:00:0Z" as "2006-01-02T15:04:05.999999999Z07:00": cannot parse "0Z" as "05"
http://localhost:3100/loki/api/v1/query_range?direction=FORWARD&end=1740920459000000000&limit=1000&query=%7Bservice_name%3D%22abb-unibank-service-bridge-int%22%7D&start=1740920430000000000
error sending request Get "http://localhost:3100/loki/api/v1/query_range?direction=FORWARD&end=1740920459000000000&limit=1000&query=%7Bservice_name%3D%22abb-unibank-service-bridge-int%22%7D&start=1740920430000000000": dial tcp [::1]:3100: connect: connection refused
Query failed: run out of attempts while querying the server
Unable to parse time parsing time "2025-03-02T13:01:0Z" as "2006-01-02T15:04:05.999999999Z07:00": cannot parse "0Z" as "05"
http://localhost:3100/loki/api/v1/query_range?direction=FORWARD&end=1740920519000000000&limit=1000&query=%7Bservice_name%3D%22abb-unibank-service-bridge-int%22%7D&start=1740920490000000000
error sending request Get "http://localhost:3100/loki/api/v1/query_range?direction=FORWARD&end=1740920519000000000&limit=1000&query=%7Bservice_name%3D%22abb-unibank-service-bridge-int%22%7D&start=1740920490000000000": dial tcp [::1]:3100: connect: connection refused
Query failed: run out of attempts while querying the server
