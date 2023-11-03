#!/bin/bash
# 미완성 스크립트라 다시 손봐야 함
cd /home/ubuntu/app
DOCKER_APP_NAME=dealight

# 실행중인 blue가 있는지
EXIST_BLUE=$(docker ps | grep ${DOCKER_APP_NAME}-blue)

# green이 실행중이면 blue up
if [ -z "$EXIST_BLUE" ]; then
  echo "Green 컨테이너가 실행 중이므로 Blue 컨테이너를 실행(UP)합니다..."
  docker compose -f docker-compose.blue.yml up -d

  sleep 30

  docker compose -f docker-compose.green.yml down
  docker image prune -af # 사용하지 않는 이미지 삭제

# blue가 실행중이면 green up
else
  echo "Blue 컨테이너가 실행 중이므로 Green 컨테이너를 실행(UP)합니다..."
  docker compose -f docker-compose.green.yml up -d

  sleep 30

  docker compose -f docker-compose.blue.yml down
  docker image prune -af
fi