#!/bin/bash

set -e

LOG_FILE=/var/log/aws/codedeploy-agent/freestyle.log

echo "rm-app.sh: starting script" >> ${LOG_FILE}

if [ -d "/opt/reporter.bak" ]; then
    echo "rm-app.sh: remove old /opt/reporter.bak" >> ${LOG_FILE}
    rm -rf /opt/reporter.bak
fi

if [ -d "/opt/reporter" ]; then
    echo "rm-app.sh: backup old /opt/reporter" >> ${LOG_FILE}
    mv /opt/reporter /opt/reporter.bak
fi

echo "rm-app.sh: finished script" >> ${LOG_FILE}

exit 0
