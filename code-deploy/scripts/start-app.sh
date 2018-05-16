#!/bin/bash

set -e

LOG_FILE=/var/log/aws/codedeploy-agent/freestyle.log

echo "start-app.sh: starting script" >> ${LOG_FILE}

echo "start-app.sh: starting reporter" >> ${LOG_FILE}
service reporter start >> $LOG_FILE

echo "start-app.sh: finished script" >> ${LOG_FILE}

exit 0
