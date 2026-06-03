import uvicorn
from fastapi import FastAPI, UploadFile, File, Form
from fastapi.middleware.cors import CORSMiddleware
import nemo.collections.asr as nemo_asr
import shutil
import os
import torch
import re

# --- CẤU HÌNH ---
# Tên mô hình trên HuggingFace
MODEL_NAME = "nvidia/parakeet-ctc-0.6b-vi"
# Cổng server (Port)
PORT = 8000
# File tạm để lưu âm thanh trước khi xử lý
TEMP_FILE = "temp_audio_input.wav"

app = FastAPI()

# Cấu hình CORS để React Frontend gọi được trực tiếp từ localhost
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

asr_model = None

@app.on_event("startup")
async def startup_event():
    """Hàm này chạy 1 lần duy nhất khi bật server để load Model"""
    global asr_model
    print(f"⏳ Đang tải mô hình {MODEL_NAME}... (Lần đầu sẽ mất vài phút)")
    
    # Tự động chọn GPU nếu có, không thì dùng CPU
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    
    # Load model từ NVIDIA NeMo
    asr_model = nemo_asr.models.ASRModel.from_pretrained(model_name=MODEL_NAME)
    asr_model.to(device)
    
    print(f"✅ Mô hình đã sẵn sàng trên thiết bị: {device}")
    print(f"🚀 Server đang chạy tại: http://localhost:{PORT}")

def calculate_score_and_details(target: str, transcribed: str):
    if not target:
        return 0, []
    
    # Chuẩn hoá từ
    target_words = re.findall(r'\w+', target.lower())
    transcribed_words = re.findall(r'\w+', transcribed.lower())
    if not target_words:
        return 0, []
    
    word_details = []
    matched_count = 0
    temp_transcribed = list(transcribed_words)
    
    for t_word in target_words:
        if t_word in temp_transcribed:
            word_details.append({"word": t_word, "status": "correct", "score": 100})
            temp_transcribed.remove(t_word)
            matched_count += 1
        else:
            word_details.append({"word": t_word, "status": "wrong", "score": 0})
            
    score = int((matched_count / len(target_words)) * 100)
    return score, word_details

@app.post("/transcribe")
@app.post("/api/v1/transcribe")
async def transcribe_audio(
    file: UploadFile = File(None),
    audio: UploadFile = File(None),
    target: str = Form(None)
):
    """API nhận file audio và trả về text nhận diện"""
    try:
        # Chọn file tải lên (hỗ trợ cả field name 'audio' và 'file')
        uploaded_file = audio if audio is not None else file
        if not uploaded_file:
            return {"status": "error", "message": "No audio file provided"}

        # 1. Lưu file upload xuống ổ cứng tạm thời
        with open(TEMP_FILE, "wb") as buffer:
            shutil.copyfileobj(uploaded_file.file, buffer)

        # 2. Gọi model để nhận dạng
        # Hỗ trợ cả phiên bản NeMo cũ (paths2audio_files) và mới (audio)
        try:
            transcriptions = asr_model.transcribe(audio=[TEMP_FILE])
        except TypeError:
            try:
                transcriptions = asr_model.transcribe(paths2audio_files=[TEMP_FILE])
            except Exception as inner_e:
                print(f"Lỗi khi transcribe: {str(inner_e)}")
                raise inner_e
        
        # Xử lý kết quả trả về (có thể là tuple, list các strings hoặc list các Hypothesis)
        if isinstance(transcriptions, tuple):
            transcriptions = transcriptions[0]
            
        text_result = ""
        if transcriptions:
            first_res = transcriptions[0]
            if isinstance(first_res, str):
                text_result = first_res
            elif hasattr(first_res, 'text'):
                text_result = first_res.text
            else:
                text_result = str(first_res)

        # 3. Tính điểm phát âm nếu có target text
        score, word_details = 0, []
        if target:
            score, word_details = calculate_score_and_details(target, text_result)

        # Trả về cả 2 định dạng (cho cả FE cũ/mới và BE Java)
        return {
            "success": True,
            "status": "success",
            "text": text_result,
            "data": {
                "transcribed": text_result,
                "score": score,
                "word_details": word_details,
                "record_id": None
            }
        }

    except Exception as e:
        print(f"Lỗi: {str(e)}")
        return {"status": "error", "message": str(e)}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=PORT)