# app.py
import uvicorn
import base64
import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from datetime import datetime  # datetime 모듈 import 추가
from pytz import timezone
from fastapi.responses import HTMLResponse

from deepLearning import detect_and_draw_boxes

app = FastAPI()

# 이미지 저장 경로 설정
SAVE_DIR = "received_images"  # SAVE_DIR 경로 설정
if not os.path.exists(SAVE_DIR):
    os.makedirs(SAVE_DIR)  # 디렉토리 생성


# 루트 경로 응답 (테스트용)
@app.get("/")
def read_root():
    return {"message": "root test success!"}

# HTML 파일을 반환하는 루트 경로 응답
@app.get("/", response_class=HTMLResponse)
async def read_index():
    with open("map/map.html", "r", encoding="utf-8") as file:
        html_content = file.read()
    return HTMLResponse(content=html_content)


# GPS 및 이미지 데이터를 받을 모델 정의
class GPSData(BaseModel):
    latitude: float
    longitude: float
    altitude: float
    heading: float
    image: str  # Base64 인코딩된 이미지 데이터

from coordinate import *
# /gps-data 엔드포인트로 POST 요청을 받는 API 생성
@app.post("/gps-data")
async def receive_gps_data(data: GPSData):
    try:
        # 받은 GPS 데이터만 로그 출력
        print(
            f"Received GPS Data - Latitude: {data.latitude}, Longitude: {data.longitude}, Altitude: {data.altitude}, Heading: {data.heading}")

        # Base64 이미지 데이터를 파일로 저장
        image_data = base64.b64decode(data.image)
        timestamp = datetime.now(timezone('Asia/Seoul')).strftime("%Y%m%d_%H%M%S")
        image_filename = os.path.join(SAVE_DIR, f"image_{timestamp}.jpg")

        with open(image_filename, "wb") as img_file:
            img_file.write(image_data)
        
        # 딥러닝 후 경계상자가 표시된 이미지(yolo_image_base64), 경계상자의 중앙좌표 반환(center_coordinates)
        yolo_image_base64, center_coordinates = detect_and_draw_boxes(image_data)
        
        #좌표 계산(객체 좌표로 수정해야 함)
        #CalculateCoordinate(5000, 300, data)
        # 경계상자의 갯수 = len(center_coordinates)
        CalculateCoordinate(center_coordinates[0]["center_x"], center_coordinates[0]["center_y"])
        
        print(f"Image saved as {image_filename}")

        return {"status": "success", "message": "Data and image received successfully"}

    except Exception as e:
        # 예외가 발생하면 오류 메시지 출력
        print(f"Error: {e}")
        raise HTTPException(status_code=500, detail="An error occurred while processing the data.")


# 이 파일이 직접 실행될 때 main() 함수 호출
if __name__ == "__main__":
    uvicorn.run("app:app", host="0.0.0.0", port=5000, reload=True)
