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
