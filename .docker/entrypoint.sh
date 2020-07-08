#!/bin/bash
classpath="worm.jar:dependencies/*"
mainclass="com.kadir.twitterbots.worm.Worm"

java -Duser.timezone=$USER_TIMEZONE \
     -cp $classpath $mainclass
