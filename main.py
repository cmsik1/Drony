from fastapi import FastAPI, BackgroundTasks
from data_handler import receive_data
from models import Data
from image_handler import list_images, get_image
from fastapi.responses import HTMLResponse

# FastAPI 앱 초기화
app = FastAPI()

# return 제거(print로 표시)
# 비동기 처리를 위한 백그라운드 작업 추가
@app.post("/gps-data")
async def gps_data_endpoint(data: Data, background_tasks: BackgroundTasks):
    # 받은 데이터를 바로 저장하고 빠른 응답 반환
    background_tasks.add_task(receive_data, data)
    return {"status": "success", "message": "Data received successfully. Processing in background."}

# 이미지 목록 조회 엔드포인트
@app.get("/")
async def list_images_endpoint():
    return await list_images()

# 특정 이미지 반환 엔드포인트
@app.get("/images/{folder}/{image_name}")
async def get_image_endpoint(folder: str, image_name: str):
    return await get_image(image_name, folder=folder)

# HTML 파일을 반환하는 루트 경로 응답
# /result에서 html 파일 열기
@app.get("/result", response_class=HTMLResponse)
async def read_index():
    with open("map/map.html", "r", encoding="utf-8") as file:
        html_content = file.read()
    return HTMLResponse(content=html_content)

# FastAPI 애플리케이션 실행
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=5000, reload=True)