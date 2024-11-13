import base64
import io
from PIL import Image
from ultralytics import YOLO

# YOLO 모델 로드 (필요한 모델 파일 경로를 지정)
model_path = "path/to/your/model/best.pt"  # YOLO 모델 파일 경로를 지정
model = YOLO(model_path)  # YOLO 모델 인스턴스 생성

# YOLO 모델로 객체를 검출하고 경계 박스를 표시하는 함수
def detect_and_draw_boxes(image_data):
    # YOLO 모델에 넣기 위해 이미지 데이터를 PIL 형식으로 변환
    image = Image.open(io.BytesIO(image_data))   

    results = model(image)  # YOLO 모델을 사용하여 이미지에서 객체 감지 수행
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

    # 경계 박스가 포함된 결과 이미지를 바이트 형태로 변환
    img_byte_arr = io.BytesIO()  # 바이트 배열을 생성
    result_image.save(img_byte_arr, format='jpg')  # 이미지를 jpg 형식으로 저장
    img_byte_arr = img_byte_arr.getvalue()  # img_byte_arr에 저장된 jpg 이미지를 바이트 데이터로 추출

    # 결과 이미지를 Base64로 인코딩하여 반환
    encoded_result_image = base64.b64encode(img_byte_arr).decode("utf-8")  # Base64로 인코딩하고 문자열로 디코딩

    # 중앙 좌표 정보와 함께 인코딩된 이미지 반환
    return encoded_result_image, center_coordinates  # 인코딩된 이미지와 중앙 좌표 반환
