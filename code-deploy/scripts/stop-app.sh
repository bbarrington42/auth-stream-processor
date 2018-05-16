#!/bin/bash

set -e

LOG_FILE=/var/log/aws/codedeploy-agent/freestyle.log

echo "stop-app.sh: starting script" >> ${LOG_FILE}

echo "stop-app.sh: stopping reporter java process" >> ${LOG_FILE}
if ([ -f /etc/init.d/reporter ]); then
	/etc/init.d/reporter stop >> ${LOG_FILE}
fi

# remove any old lock files
if ([ -f /var/lock/subsys/reporter ]); then
	echo "stop-app.sh: removing old lock files" >> ${LOG_FILE}
	rm -f /var/lock/subsys/reporter
fi

echo "stop-app.sh: finished script" >> ${LOG_FILE}

exit 0
