#!/bin/bash

set -e

LOG_FILE=/var/log/aws/codedeploy-agent/freestyle.log
APP=auth-check

echo "stop-app.sh: starting script" >> ${LOG_FILE}

echo "stop-app.sh: stopping $APP" >> ${LOG_FILE}
if ([ -f /etc/init.d/$APP ]); then
	service $APP stop >> ${LOG_FILE}
fi

# remove any old lock files
if ([ -f /var/lock/subsys/$APP ]); then
	echo "stop-app.sh: removing old lock files" >> ${LOG_FILE}
	rm -f /var/lock/subsys/$APP
fi

echo "stop-app.sh: finished script" >> ${LOG_FILE}

exit 0
