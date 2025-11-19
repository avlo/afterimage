#!/bin/bash

DIR=~/temp/scan-containers
OUTPUT_FILE_PREFIX=$DIR/outfile-scan
EXISTING_TERMINAL_PIDS_FILE=$DIR/term.pre
NEW_TERMINAL_PIDS_FILE=$DIR/term.post
CONTAINER_IDS_AND_NAMES=$DIR/container_ids_and_names
CONTAINER_IDS=$DIR/container_ids
CONTAINER_NAMES=$DIR/container_names

ls_docker_containers() {
	docker container ls --format "{{.ID}} {{.Names}}"|grep -e super -e after
}

display_pid_term() {
  echo "--------"
  echo "    x: [$3]"
  echo "    y: [$4]"
  echo "   id: [$1]"
  echo "title: [$2]"
#  gnome-terminal --geometry=115x50+"$3"+"$4" --title="$2" --zoom=.6 -- docker logs -f "$1" > $OUTPUT_FILE_PREFIX."$2" & tail -f "$OUTPUT_FILE_PREFIX."$2"
  gnome-terminal --geometry=115x50+"$3"+"$4" --title="$2" --zoom=.6 -- bash -c "docker logs -f '$1' && read"
}

get_existing_terminal_pids() {
  ps aux|grep bash|grep -v color | awk '{print $2}' > $EXISTING_TERMINAL_PIDS_FILE
}

get_new_terminal_pids() {
  ps aux|grep bash|grep -v color | awk '{print $2}' > $NEW_TERMINAL_PIDS_FILE
}

get_container_pids() {
#  awk '{ print $1, $13 }' simulated-docker-run.java > container_ids_and_names
  ls_docker_containers | awk '{ print $1, $2 }' | sed 's/ [^-]*-/ /' > $CONTAINER_IDS_AND_NAMES
  awk '{ print $1 }' $CONTAINER_IDS_AND_NAMES > $CONTAINER_IDS
  awk '{ print $2 }' $CONTAINER_IDS_AND_NAMES > $CONTAINER_NAMES 
}

create_terminals() {
  loop_limit=$(wc $CONTAINER_IDS_AND_NAMES | awk '{ print $1 }')
  x_position=1
  y_position=1 
  horizontal_terminal_count=4
  j_counter=1
  j_iterations=$(($((loop_limit / horizontal_terminal_count)) + 1 ))
  for (( j = 0; j < j_iterations; j++ )); do # number of columns
    for (( k = 0; k < horizontal_terminal_count; k++ )); do # number of rows
      line_number=$((j_counter + k))
      pid=$(sed "${line_number}b;d" $CONTAINER_IDS)
      name=$(sed "${line_number}b;d" $CONTAINER_NAMES)
      display_pid_term "$pid" "$name" "$x_position" "$y_position"
      x_position=$((x_position + 480)) # increment x position by 480
    done
    j_counter=$((j_counter + horizontal_terminal_count))
    x_position=1
    y_position=$((y_position + 500)) # increment y position by 500
  done
}

kill_terminals() {
  grep -v -f $EXISTING_TERMINAL_PIDS_FILE $NEW_TERMINAL_PIDS_FILE | xargs kill -9
}
  
cleanup() {
	rm $CONTAINER_IDS_AND_NAMES 
	rm $CONTAINER_IDS
	rm $CONTAINER_NAMES
	rm $EXISTING_TERMINAL_PIDS_FILE
	rm $NEW_TERMINAL_PIDS_FILE
}

usage() { echo "Usage:  once containers have properly started, press any key to launch terminals then any key to close terminals" 1>&2; exit 1; }

user_prompt_start_terminals() {
  while true; do
    read -p "container scan:
  (enter/default) -> start): " -a args_array
    
    [[ ${#args_array[@]} -eq 0 ]] && 
      { get_container_pids;
        create_terminals;
			  get_new_terminal_pids;	
        return; }
  done
}

user_prompt_stop_terminals() {
  while true; do
      read -p "kill terminals:
    (enter/default) -> start): " -a args_array
      
      [[ ${#args_array[@]} -eq 0 ]] && 
        { kill_terminals;
          return; }
  done
}

###########     main    ################
get_existing_terminal_pids
user_prompt_start_terminals
user_prompt_stop_terminals
cleanup
