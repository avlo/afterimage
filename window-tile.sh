#!/bin/bash

HOME_DIR=/home/nick/temp
WINDOW_TILE_TEMP_DIR=$HOME_DIR/temp-window-tiles
EXISTING_TERMINAL_PIDS_FILE=$WINDOW_TILE_TEMP_DIR/term.pre
NEW_TERMINAL_PIDS_FILE=$WINDOW_TILE_TEMP_DIR/term.post
TERMINAL_CD=$WINDOW_TILE_TEMP_DIR/terminal_cd
TERMINAL_TITLE=$WINDOW_TILE_TEMP_DIR/terminal_title
TERMINAL_ENV=$WINDOW_TILE_TEMP_DIR/terminal_env

horizontal_terminal_count=2
vertical_terminal_count=2
zoom_factor=.7

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

create_docker_terminal_env() {
  file_name="$TERMINAL_ENV-$1"
  dock_up_alias="$2"
  echo "#!/bin/bash --init-file" > "$file_name"
  echo "$dock_up_alias" >> "$file_name"
  chmod 755 "$file_name"
}

create_app_terminal_env() {
  file_name="$TERMINAL_ENV-$1"
  app_up_alias="$2"
  echo "#!/bin/bash --init-file" > "$file_name"
  echo "$app_up_alias" >> "$file_name"
  chmod 755 "$file_name"
}

display_cd_term() {
  if [ -z "$1" ]; then
    return
  fi
  
  echo "-----------------"
  echo "           x: [$3]"
  echo "           y: [$4]"
  echo "         dir: [$1]"
  echo "       title: [$2]"
  echo "devenv param: [$5]"
  
  devenv="$TERMINAL_ENV-$5"
  echo "devenv file: [$devenv]"
  echo
  gnome-terminal --geometry=156x54+"$3"+"$4" --title="$2" --zoom="$zoom_factor" --working-directory="$1" -- bash -c "source $devenv;bash -i"
}

create_temp_dir() {
  mkdir -p $WINDOW_TILE_TEMP_DIR
}

get_existing_terminal_pids() {
  ps aux|grep bash|grep -v color | awk '{print $2}' > $EXISTING_TERMINAL_PIDS_FILE
}

get_new_terminal_pids() {
  ps aux|grep bash|grep -v color | awk '{print $2}' > $NEW_TERMINAL_PIDS_FILE
}

append_terminal_files() {
  if [ -e $2 ]
  then
      echo "$1" >> $2
  else
      echo "$1" > $2
  fi
}

sc_redis_6379() {
  append_terminal_files "SC redis 6379" $TERMINAL_TITLE
  append_terminal_files "/home/nick/git/superconductor" $TERMINAL_CD
  append_terminal_files "sc_redis_6379" $TERMINAL_ENV
  dock_up_alias="docker compose -f superconductor/redis/docker-compose-local_ws.yml up -d && docker container ls -a | grep superconductor-db-redis-local | cut -c 1-12 | xargs docker logs -f"
  dock_down_alias="docker compose -f superconductor/redis/docker-compose-local_ws.yml stop && docker compose -f superconductor/redis/docker-compose-local_ws.yml down --remove-orphans"
  create_docker_terminal_env "sc_redis_6379" "$dock_up_alias" "$dock_down_alias"
}

aimg_redis_6381() {
  append_terminal_files "aImg redis 6381" $TERMINAL_TITLE
  append_terminal_files "/home/nick/git/afterimage" $TERMINAL_CD
  append_terminal_files "aimg_redis_6381" $TERMINAL_ENV
  dock_up_alias="docker compose -f docker-compose-local_ws_6381.yml up -d && docker container ls -a | grep afterimage-db-redis-local_ws_6381 | cut -c 1-12 | xargs docker logs -f"
  dock_down_alias="docker compose -f docker-compose-local_ws_6381.yml down --remove-orphans"
  create_docker_terminal_env "aimg_redis_6381" "$dock_up_alias" "$dock_down_alias"
}

aimg_redis_6382() {
  append_terminal_files "aImg redis 6382" $TERMINAL_TITLE
  append_terminal_files "/home/nick/git/afterimage" $TERMINAL_CD
  append_terminal_files "aimg_redis_6382" $TERMINAL_ENV
  dock_up_alias="docker compose -f docker-compose-local_ws_6382.yml up -d && docker container ls -a | grep afterimage-db-redis-local_ws_6382 | cut -c 1-12 | xargs docker logs -f"
  dock_down_alias="docker compose -f docker-compose-local_ws_6382.yml down --remove-orphans"
  create_docker_terminal_env "aimg_redis_6382" "$dock_up_alias" "$dock_down_alias"
}

sc_app_5555() {
  append_terminal_files "SC 5555" $TERMINAL_TITLE
  append_terminal_files "/home/nick/git/superconductor" $TERMINAL_CD
  append_terminal_files "sc_app_5555" $TERMINAL_ENV
  app_up_alias="gradle superconductor-app-redis:bootRunLocalWs -Pserver.port=5555 -Psuperconductor.relay.url=ws://localhost:5555 -Plogging.level.com.prosilion.superconductor=debug -Plogging.level.com.prosilion.subdivisions=debug"
  create_app_terminal_env "sc_app_5555" "$app_up_alias"
}

aimg_app_5557() {
  append_terminal_files "aImg 5557" $TERMINAL_TITLE
  append_terminal_files "/home/nick/git/afterimage" $TERMINAL_CD
  append_terminal_files "aimg_app_5557" $TERMINAL_ENV  
  app_up_alias="gradle bootRunLocalWs -Pserver.port=5557 -Pafterimage.relay.url=ws://localhost:5557 -Pspring.data.redis.port=6381"
  create_app_terminal_env "aimg_app_5557" "$app_up_alias"
}

aimg_app_5558() {
  append_terminal_files "aImg 5558" $TERMINAL_TITLE
  append_terminal_files "/home/nick/git/afterimage" $TERMINAL_CD
  append_terminal_files "aimg_app_5558" $TERMINAL_ENV
  app_up_alias="gradle bootRunLocalWs -Pserver.port=5558 -Pafterimage.relay.url=ws://localhost:5558 -Pspring.data.redis.port=6382"
  create_app_terminal_env "aimg_app_5558" "$app_up_alias"
}

create_terminal_names() {
#  sc_redis_6379
  aimg_redis_6381
  aimg_redis_6382

#  sc_app_5555
  aimg_app_5557
  aimg_app_5558
}

create_terminals() {
  loop_limit=$(wc $TERMINAL_TITLE | awk '{ print $1 }')
  x_position=1
  y_position=1 
  j_counter=1
  j_iterations=$(($((loop_limit / horizontal_terminal_count)) + 1 ))
  for (( j = 0; j < j_iterations; j++ )); do # number of columns
    for (( k = 0; k < horizontal_terminal_count; k++ )); do # number of rows
      line_number=$((j_counter + k))
      cd_dir=$(sed "${line_number}b;d" $TERMINAL_CD)
      name=$(sed "${line_number}b;d" $TERMINAL_TITLE)
      term_env=$(sed "${line_number}b;d" $TERMINAL_ENV)
      display_cd_term "$cd_dir" "$name" "$x_position" "$y_position" "$term_env"
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
  docker stop $(docker ps -a -q)
  docker rm $(docker ps -a -q)
  docker volume prune -a -f
  docker network prune -f
	rm -rf $WINDOW_TILE_TEMP_DIR;
}

usage() {
  echo "Usage:  Press enter to start services"
  echo "  then press enter to close terminals, stop all apps & clean up all docker containers" 1>&2; exit 1; 
}

user_prompt_start_terminals() {
  get_screen_resolution
  while true; do
    read -p "create terminals:
  (enter/default) -> start): " -a args_array
    
    [[ ${#args_array[@]} -eq 0 ]] && 
      { create_terminal_names;
        create_app_terminal_names;
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
