#!/bin/bash

set -e

LOG_FILE=/var/log/aws/codedeploy-agent/freestyle.log
APP=auth-check

echo "starting rm-app.sh" >> ${LOG_FILE}

if [ -d "/opt/$APP.bak" ]; then
    echo "rm-app.sh: remove old /opt/$APP.bak" >> ${LOG_FILE}
    rm -rf /opt/$APP.bak
fi

if [ -d "/opt/$APP" ]; then
    echo "rm-app.sh: backup old /opt/$APP" >> ${LOG_FILE}
    mv /opt/$APP /opt/$APP.bak
fi

echo "rm-app.sh: finished script" >> ${LOG_FILE}

exit 0
