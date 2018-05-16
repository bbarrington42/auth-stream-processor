#!/bin/bash

set -e

LOG_FILE=/var/log/aws/codedeploy-agent/freestyle.log

echo "setup-app.sh: starting script" >> $LOG_FILE

echo "setup-app.sh: adding init script" >> $LOG_FILE
chkconfig --add reporter

echo "setup-app.sh: finished script" >> $LOG_FILE

exit 0

