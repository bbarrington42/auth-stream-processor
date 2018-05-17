#!/bin/bash

set -e

LOG_FILE=/var/log/aws/codedeploy-agent/freestyle.log
APP=auth-processor

echo "start-app.sh: starting script" >> ${LOG_FILE}

echo "start-app.sh: starting $APP" >> ${LOG_FILE}
service $APP start >> $LOG_FILE

echo "start-app.sh: finished script" >> ${LOG_FILE}

exit 0
