import numpy as np
import joblib
import h5py
from tensorflow.keras.models import load_model, Model
from xgboost import XGBClassifier

# ====================================================
# 1. 모델 및 스케일러 로드 (서버 시작 시 1회 실행)
# ====================================================
print("Loading models...")

# (1) 전처리 도구 로드
try:
    imputer = joblib.load('imputer.joblib')
    scaler = joblib.load('scaler.joblib')
    scaler_ae_error = joblib.load('scaler_ae_error_TRAIN.joblib')
    scaler_xgb_prob = joblib.load('scaler_xgb_prob_TRAIN.joblib')

    # (2) Autoencoder 모델 로드
    # h5py를 사용하여 로드 (버전 호환성 유지)
    with h5py.File('autoencoder_model.h5', 'r') as f:
        autoencoder = load_model(f)
    
    # 인코더 부분만 따로 추출 (XGBoost 입력용)
    encoder = Model(inputs=autoencoder.input, outputs=autoencoder.get_layer("latent").output)

    # (3) XGBoost 모델 로드
    xgb_model = XGBClassifier()
    xgb_model.load_model('xgb_model.json')

    print("✅ All models loaded successfully!")

except Exception as e:
    print(f"❌ 모델 로드 실패: {e}")
    # 실제 운영 환경에서는 여기서 서버 실행을 중단하거나 알림을 보내야 함


# ====================================================
# 2. 예측 함수 (API 요청이 들어올 때마다 실행)
# ====================================================
def predict_new_data(new_data_list):
    """
    백엔드로 들어온 새로운 데이터를 판별하는 함수
    :param new_data_list: (N, 8) 형태의 2차원 리스트 또는 배열 (피처 8개)
    :return: 판별 결과 리스트 (0: 정상, 1: 이상), 최종 점수 리스트
    """
    # 입력 데이터를 numpy 배열로 변환
    X_input = np.array(new_data_list)
    
    # 1. 전처리 (학습 때와 동일하게 적용)
    # 결측치 처리
    X_imputed = imputer.transform(X_input)
    # 스케일링 (StandardScaler)
    X_scaled = scaler.transform(X_imputed)
    
    # 2. 모델 예측
    # (A) Autoencoder 재구성 오차 계산
    X_recon = autoencoder.predict(X_scaled, verbose=0)
    ae_error = np.mean((X_scaled - X_recon) ** 2, axis=1)
    
    # (B) XGBoost 확률 계산 (Latent vector 추출 후 예측)
    latent_vector = encoder.predict(X_scaled, verbose=0)
    xgb_prob = xgb_model.predict_proba(latent_vector)[:, 1]
    
    # 3. 점수 결합 (Soft Voting)
    # 각 점수를 0~1로 스케일링
    scaled_ae_score = scaler_ae_error.transform(ae_error.reshape(-1, 1)).flatten()
    scaled_xgb_score = scaler_xgb_prob.transform(xgb_prob.reshape(-1, 1)).flatten()
    
    # 가중치 적용 (AE: 0.3, XGB: 0.7)
    final_score = 0.3 * scaled_ae_score + 0.7 * scaled_xgb_score
    
    # 4. 최종 판정 (임계값 0.5)
    threshold = 0.5
    final_pred = (final_score >= threshold).astype(int)
    
    return final_pred, final_score


# ====================================================
# 3. 사용 예시 (테스트)
# ====================================================
if __name__ == "__main__":
    # 가상의 신규 데이터 2개 (feature 8개씩)
    mock_input_data = [
        [0.1, 0.2, 0.5, 0.1, 0.0, 0.9, 0.2, 0.3], # 데이터 1
        [5.0, 2.1, 0.0, 9.2, 1.1, 0.2, 8.5, 0.1]  # 데이터 2 (이상치 가정)
    ]

    predictions, scores = predict_new_data(mock_input_data)

    print("\n--- Prediction Results ---")
    for i, (pred, score) in enumerate(zip(predictions, scores)):
        status = "🚨 ANOMALY" if pred == 1 else "✅ NORMAL"
        print(f"Data {i+1}: Score = {score:.4f} -> {status}")