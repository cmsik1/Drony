import base64
from PIL import Image
from ultralytics import YOLO
import cv2  
import numpy as np

# YOLO 모델로 객체를 검출하고 경계 박스를 표시하는 함수
def detect_and_draw_boxes(image_data, yolo_model):

    # YOLO 모델에 넣기 위해 이미지 데이터를 PIL 형식으로 변환
    image = Image.open(image_data) 

    results = yolo_model(image)  # YOLO 모델을 사용하여 이미지에서 객체 감지 수행
    result_image = results[0].plot()  # 첫 번째 결과에서 경계 박스가 표시된 이미지 생성
    
    # 중앙 좌표를 저장할 리스트 초기화
    center_coordinates = []

    # 객체 인식 결과에서 각 경계 박스의 중앙 좌표 계산
    for box in results[0].boxes:
        # 경계 박스 좌표 (xmin, ymin, xmax, ymax) 가져오기
        xmin, ymin, xmax, ymax = box.xyxy[0]
        
        # 중앙 좌표 계산
        center_x = (xmin + xmax) / 2
        center_y = (ymin + ymax) / 2
        center_coordinates.append({"center_x": center_x.item(), "center_y": center_y.item()})
        confidence = box.conf.cpu().numpy()[0]  # 신뢰도
        #신뢰도, 경계상자 중앙좌표 출력
        #print(f"신뢰도: {confidence:.2f}, 경계 상자 중앙 좌표: ({center_x:.2f}, {center_y:.2f})")

    # 결과 이미지를 numpy 형식으로 변환
    result_image = cv2.cvtColor(np.array(result_image), cv2.COLOR_RGB2BGR)
    _, buffer = cv2.imencode('.jpg', result_image)  # numpy 형식 이미지를 JPG로 인코딩
    encoded_result_image = base64.b64encode(buffer).decode("utf-8")  # Base64로 인코딩하여 문자열로 변환
  
    # 중앙 좌표 정보와 함께 인코딩된 이미지 반환
    return encoded_result_image, center_coordinates# 인코딩된 이미지와 중앙 좌표 반환
