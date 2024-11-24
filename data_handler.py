import os
import base64
import config
from datetime import datetime
from pytz import timezone
from fastapi import HTTPException
from pydantic import BaseModel
from ultralytics import YOLO

from deepLearning import *
from coordinate import *
# 이미지 저장 디렉토리 설정
SAVE_DIR = config.SAVE_DIR
if not os.path.exists(SAVE_DIR):
    os.makedirs(SAVE_DIR)

DETECTED_DIR =config.DETECTED_DIR
if not os.path.exists(DETECTED_DIR):
    os.makedirs(DETECTED_DIR)

# 데이터 모델 정의
class Data(BaseModel):
    latitude: float
    longitude: float
    altitude: float
    heading: float
    image: str  # Base64 인코딩된 이미지 데이터

model_path = YOLO("path/to/your/model/best.pt") 

# 수신 받은 데이터를 처리하는 함수
async def receive_data(data: Data):
    try:
        # 현재 시간 추가
        timestamp = datetime.now(timezone('Asia/Seoul')).strftime("%Y-%m-%d %H:%M:%S")

        # 받은 GPS 데이터 로그 출력
        print(f"[{timestamp}] Received GPS Data - Latitude: {data.latitude}, Longitude: {data.longitude}, Altitude: {data.altitude}, Heading: {data.heading}")

        # Base64 이미지 데이터를 디코딩하여 파일로 저장
        image_data = base64.b64decode(data.image)
        image_filename = os.path.join(SAVE_DIR, f"image_{timestamp.replace(':', '').replace(' ', '_')}.jpg")

        encoded_image, center_coordinates = detect_and_draw_boxes(image_data, model_path)
        
        if len(center_coordinates) > 0:
            # 첫 번째 객체의 중앙 좌표를 사용하여 좌표 계산
            CalculateCoordinate(center_coordinates[0]["center_x"], center_coordinates[0]["center_y"], data)
            
            # 검출된 이미지를 저장할 경로 생성
            detected_image_filename = os.path.join(DETECTED_DIR, f"detected_{timestamp.replace(':', '').replace(' ', '_')}.jpg")

            # 객체 검출된 이미지 저장
            with open(detected_image_filename, "wb") as detected_img_file:
                detected_img_file.write(encoded_image)

            print(f"사람이 인식되었습니다. 이미지저장 : {detected_image_filename}")

        else:
            print("사람이 인식되지 않았습니다.")         

        # 원본 이미지는 항상 저장
        with open(image_filename, "wb") as img_file:
            img_file.write(image_data)
        

    except Exception as e:
        # 예외 발생 시 오류 처리
        print(f"Error: {e}")
        raise HTTPException(status_code=500, detail="An error occurred while processing the data.")
    
    
