#!/bin/bash

set -e

LOG_FILE=/var/log/aws/codedeploy-agent/freestyle.log
APP=auth-check

echo "validate-app.sh: starting script" >> ${LOG_FILE}

echo "validate-app.sh: check if $APP running " >> ${LOG_FILE}

if ! ps -ef | grep -q "[o]pt/$APP"; then
    echo "validate-app.sh: finished script with failure" >> ${LOG_FILE}
    exit 1
fi

echo "validate-app.sh: finished script" >> ${LOG_FILE}

exit 0