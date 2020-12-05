#!/bin/bash -x

CMD=$1

BOX=root@isp1.6350.lowes.com


export PATH=.:/bin:/usr/local/bin

eval $(keychain --eval ~/.ssh/id_rsa)

SSH_ARGS="-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no"
SSH="ssh $SSH_ARGS"
SCP="scp -r $SSH_ARGS"

WORKSPACE="$($SSH $BOX 'pwd')/workspace"

# test connection
$SSH $BOX true || exit 1
echo "Connection test successful"


case $CMD in
"load")
  $SSH $BOX "mkdir -p $WORKSPACE/target"
  $SCP $PWD/target $BOX:$WORKSPACE
  $SSH $BOX "ls -ld \$(find $WORKSPACE/target -type f)"
  ;;
"run")
  $SSH $BOX "cd $WORKSPACE; nohup java -classpath $WORKSPACE/target/classes  com.lagodiuk.agent.evolution.Runner $WORKSPACE/world.xml > $WORKSPACE/Runner.out 2>&1 &"
  $SSH $BOX "ps -ef | grep com[.]lagodiuk.agent.evolution.Runner"
  ;;
"status")
  $SSH $BOX "ps -ef | grep com[.]lagodiuk.agent.evolution.Runner"
  $SSH $BOX "head $WORKSPACE/world.xml && echo ... && tail -3 $WORKSPACE/world.xml"
  ;;
"get")
  $SCP $BOX:$WORKSPACE/world.xml $PWD
  ls -ld $PWD/world.xml
  bash -c "head $PWD/world.xml && echo ... &&  tail -3 $PWD/world.xml"
  ;;
"abort")
  $SSH $BOX "rm $WORKSPACE/world.xml"
  echo "Told the program to Abort"
  ;;
"install-java")
  $SSH $BOX "apt-get install default-jdk"
  ;;
*)
  printf "Error: wrong parameters\n"
  printf "Usage:\n       $0 [load|run|status|get]\n"
  exit 1
  ;;
esac


echo "$0 Finished" > /dev/null
