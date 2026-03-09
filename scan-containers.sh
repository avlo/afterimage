#!/bin/bash

#HOME_DIR=~/temp/scan-containers
HOME_DIR=.
SCAN_TEMP_DIR=$HOME_DIR/temp-scan-pids
EXISTING_TERMINAL_PIDS_FILE=$SCAN_TEMP_DIR/term.pre
NEW_TERMINAL_PIDS_FILE=$SCAN_TEMP_DIR/term.post
CONTAINER_IDS_AND_NAMES=$SCAN_TEMP_DIR/container_ids_and_names
CONTAINER_IDS=$SCAN_TEMP_DIR/container_ids
CONTAINER_NAMES=$SCAN_TEMP_DIR/container_names

suffix=scan-suffix-for-sublime.java

horizontal_terminal_count=2
vertical_terminal_count=2
zoom_factor=.6

display_resolution_operands() {
  words=("$@")
  for element in $words;do
    numeric=$(echo "$element" | grep -oE '[0-9]+([.][0-9]+)?')
    if [[ -n "$numeric" ]]; then
      printf "$(tput bold) %s $(tput sgr0)" "$numeric"
    else
      printf $element
    fi
  done
  echo
}


get_screen_resolution() {
  screen_x_resolution=$(xrandr --current | grep '*' | uniq | awk '{print $1}' | cut -d 'x' -f1)
  screen_y_resolution=$(xrandr --current | grep '*' | uniq | awk '{print $1}' | cut -d 'x' -f2)
  
  x_position_increment=$((screen_x_resolution / horizontal_terminal_count))
  y_position_increment=$((screen_y_resolution / vertical_terminal_count))
  
  terminal_width=$((x_position_increment / horizontal_terminal_count))
  echo "terminal_width: $terminal_width"
  
  delta=$(echo "scale=2; 1 - $zoom_factor" | bc)
  echo "delta: $delta"

  deltatwo=$(echo "scale=2; $delta * .1" | bc)
  echo "delta2: $deltatwo"

  zoom_factor_decimal=$(echo "scale=2; $x_position_increment * $deltatwo" | bc)
  scaling_zoom_factor=${zoom_factor_decimal/\.*/}
  echo "zoom_factor_decimal: $zoom_factor_decimal"
  echo "scaling_zoom_factor: $scaling_zoom_factor"
  x_geometry=$((terminal_width + scaling_zoom_factor))

  echo "x_geometry: $x_geometry"
  echo "y_geometry: $y_geometry"
#  x_geometry=308
#  y_geometry=50
  
  display_resolution_operands "x-resolution: $screen_x_resolution, x-increment: $x_position_increment, x-geometry: $x_geometry"
  display_resolution_operands "y-resolution: $screen_y_resolution, y-increment: $y_position_increment, y-geometry: $y_geometry"
  echo ""
}

ls_docker_containers() {
	docker container ls --format "{{.ID}} {{.Names}}"|grep -e afterimage-app -e superconductor-afterimage
}

display_pid_term() {
  if [ -z "$1" ]; then
    return
  fi
  
  echo "--------"
  echo "    x: [$3]"
  echo "    y: [$4]"
  echo "   id: [$1]"
  echo "title: [$2]"

  gnome-terminal --geometry=308x100+"$3"+"$4" --title="$2" --zoom="$zoom_factor" -- bash -c "docker logs -f '$1' && read"
  (docker logs -f "$1" > "$2_$suffix") &
}

create_temp_dir() {
  mkdir -p $SCAN_TEMP_DIR
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
  j_counter=1
  j_iterations=$(($((loop_limit / horizontal_terminal_count)) + 1 ))
  for (( j = 0; j < j_iterations; j++ )); do # number of columns
    for (( k = 0; k < horizontal_terminal_count; k++ )); do # number of rows
      line_number=$((j_counter + k))
      pid=$(sed "${line_number}b;d" $CONTAINER_IDS)
      name=$(sed "${line_number}b;d" $CONTAINER_NAMES)
      display_pid_term "$pid" "$name" "$x_position" "$y_position"
      x_position=$((x_position + x_position_increment)) # increment x position by 480
    done
    j_counter=$((j_counter + horizontal_terminal_count))
    x_position=1
    y_position=$((y_position + y_position_increment)) # increment y position by 500
  done
}

kill_terminals() {
  grep -v -f $EXISTING_TERMINAL_PIDS_FILE $NEW_TERMINAL_PIDS_FILE | xargs kill -9
}
  
cleanup() {
#	rm $CONTAINER_IDS_AND_NAMES 
#	rm $CONTAINER_IDS
#	rm $CONTAINER_NAMES
#	rm $EXISTING_TERMINAL_PIDS_FILE
#	rm $NEW_TERMINAL_PIDS_FILE
	rm -rf $SCAN_TEMP_DIR;
}

usage() { echo "Usage:  once containers have properly started, press any key to launch terminals then any key to close terminals" 1>&2; exit 1; }

user_prompt_start_terminals() {
  get_screen_resolution
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
create_temp_dir
get_existing_terminal_pids
user_prompt_start_terminals
user_prompt_stop_terminals
cleanup
