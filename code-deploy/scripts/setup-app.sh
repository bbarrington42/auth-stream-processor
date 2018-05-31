#!/bin/bash

set -x

LOG_FILE=/var/log/aws/codedeploy-agent/freestyle.log
APP=auth-processor

echo "setup-app.sh: starting script" >> $LOG_FILE

echo "setup-app.sh: adding init script" >> $LOG_FILE
chkconfig --add $APP

echo "setup-app.sh: finished script" >> $LOG_FILE

exit 0

