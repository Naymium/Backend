#!/usr/bin/env python3
import argparse
import json
import sys
import os
import io
import csv
import base64

import numpy as np
import joblib
import h5py  # tensorflow h5 로드용
from tensorflow.keras.models import load_model, Model
from xgboost import XGBClassifier

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt


# =========================================================
# 0. 경로 설정 (루트 또는 ml 폴더 둘 다 지원)
# =========================================================
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ML_DIR = os.path.join(BASE_DIR, "ml")

def _resolve(path_in_ml):
    """
    1) BASE_DIR/path_in_ml  (예: Nyamium/imputer.joblib)
    2) ML_DIR/path_in_ml    (예: Nyamium/ml/imputer.joblib)
    두 군데 중 존재하는 쪽을 골라줌.
    """
    p1 = os.path.join(BASE_DIR, path_in_ml)
    p2 = os.path.join(ML_DIR, path_in_ml)
    if os.path.exists(p1):
        return p1
    if os.path.exists(p2):
        return p2
    # 둘 다 없으면 일단 p2 반환하고 나중에 에러
    return p2


PATH_IMPUTER          = _resolve("imputer.joblib")
PATH_SCALER           = _resolve("scaler.joblib")
PATH_SCALER_AE_ERR    = _resolve("scaler_ae_error_TRAIN.joblib")
PATH_SCALER_XGB_PROB  = _resolve("scaler_xgb_prob_TRAIN.joblib")
PATH_AUTOENCODER_H5   = _resolve("autoencoder_model.h5")
PATH_XGB_JSON         = _resolve("xgb_model.json")


# =========================================================
# 1. 모델 및 전처리 로딩 (프로세스 시작 시 1회)
# =========================================================
print("[predict.py] Loading models & preprocessors...", file=sys.stderr)

try:
    imputer = joblib.load(PATH_IMPUTER)
    scaler = joblib.load(PATH_SCALER)
    scaler_ae_error = joblib.load(PATH_SCALER_AE_ERR)
    scaler_xgb_prob = joblib.load(PATH_SCALER_XGB_PROB)
except Exception as e:
    print(f"[predict.py] ❌ Error loading joblib files: {e}", file=sys.stderr)
    sys.exit(1)

try:
    # 그냥 경로로 로드 (내부에서 h5py 사용)
    autoencoder = load_model(PATH_AUTOENCODER_H5, compile=False)

    # encoder 추출: 1) 'latent'라는 이름의 레이어가 있으면 그걸 사용
    #               2) 없으면 마지막에서 두 번째 레이어 사용
    latent_layer = None
    try:
        latent_layer = autoencoder.get_layer("latent")
    except Exception:
        pass

    if latent_layer is not None:
        encoder = Model(inputs=autoencoder.input, outputs=latent_layer.output)
        print("[predict.py] encoder: using layer named 'latent'", file=sys.stderr)
    else:
        encoder = Model(inputs=autoencoder.input, outputs=autoencoder.layers[-2].output)
        print("[predict.py] encoder: using autoencoder.layers[-2]", file=sys.stderr)

except Exception as e:
    print(f"[predict.py] ❌ Error loading autoencoder: {e}", file=sys.stderr)
    sys.exit(1)

try:
    xgb_model = XGBClassifier()
    xgb_model.load_model(PATH_XGB_JSON)
except Exception as e:
    print(f"[predict.py] ❌ Error loading XGB model: {e}", file=sys.stderr)
    sys.exit(1)

print("[predict.py] ✅ All models loaded.", file=sys.stderr)


# =========================================================
# 2. 신규 데이터 예측 함수 (Notebook 로직 래핑)
# =========================================================
def predict_new_data(X_raw: np.ndarray):
    """
    X_raw: shape (N, 8)  [e1,e2,e3,e4,l1,l2,l3,l4]
    return:
      preds       : (N,) int 0/1
      final_score : (N,) float (soft voting 점수)
      ae_error    : (N,) float
      xgb_prob    : (N,) float
    """

    # 1) 전처리: 결측치 -> imputer, 스케일링 -> scaler
    X_imputed = imputer.transform(X_raw)
    X_scaled  = scaler.transform(X_imputed)

    # 2-A) Autoencoder 재구성 오차
    X_recon   = autoencoder.predict(X_scaled, verbose=0)
    ae_error  = np.mean((X_scaled - X_recon) ** 2, axis=1)

    # 2-B) Encoder latent -> XGB 확률
    latent   = encoder.predict(X_scaled, verbose=0)
    xgb_prob = xgb_model.predict_proba(latent)[:, 1]

    # 3) 각 점수를 0~1로 스케일링
    ae_scaled  = scaler_ae_error.transform(ae_error.reshape(-1, 1)).ravel()
    xgb_scaled = scaler_xgb_prob.transform(xgb_prob.reshape(-1, 1)).ravel()

    # 4) Soft Voting (가중치는 노트북 기준 0.3 / 0.7 가정)
    final_score = 0.3 * ae_scaled + 0.7 * xgb_scaled

    # 5) threshold 기준 판정 (0.5 가정)
    threshold = 0.5
    preds = (final_score >= threshold).astype(int)

    return preds, final_score, ae_error, xgb_prob


# =========================================================
# 3. JSON 입력 (--file) 처리: PredictService 용
# =========================================================
def handle_json_file(json_path: str):
    """
    입력: PredictService 가 만든 JSON 파일 경로
      {
        "results": {
          "resultList": [
            {
              "e1":..., "e2":..., ..., "l4":...,
              "rangingError":..., "delta":..., "fd":..., "sigma":...
            },
            ...
          ]
        },
        "dataNum": ...,
        "fileName": ...
      }

    출력: 동일 구조 + prediction / probability 필드 추가
    """

    with open(json_path, "r", encoding="utf-8") as f:
        root = json.load(f)

    if "results" not in root or "resultList" not in root["results"]:
        raise RuntimeError("입력 JSON에 results.resultList 가 없습니다.")

    result_list = root["results"]["resultList"]

    X = []
    for r in result_list:
        X.append([
            float(r["e1"]),
            float(r["e2"]),
            float(r["e3"]),
            float(r["e4"]),
            float(r["l1"]),
            float(r["l2"]),
            float(r["l3"]),
            float(r["l4"]),
        ])

    X = np.array(X, dtype=float)

    preds, scores, ae_error, xgb_prob = predict_new_data(X)

    for r, pred, score, ae_e, prob_xgb in zip(result_list, preds, scores, ae_error, xgb_prob):
        pred = int(pred)
        r["pred"] = pred
        r["prediction"] = "ABNORMAL" if pred == 1 else "NORMAL"
        r["probability"] = float(score)
        r["prob"] = float(score)
        # 디버깅용 보조 값
        r["ae_error"] = float(ae_e)
        r["xgb_prob"] = float(prob_xgb)

    if "dataNum" not in root:
        root["dataNum"] = len(result_list)
    root.setdefault("fileName", None)

    json.dump(root, sys.stdout, ensure_ascii=False)
    sys.stdout.write("\n")


# =========================================================
# 4. CSV 입력 (--stdin-csv + --plots-out-base64): GraphService 용
# =========================================================
def handle_csv_from_stdin_and_plot():
    """
    GraphService.runPythonAndGetBase64JsonWithCsv 에서 사용하는 모드
      - STDIN 으로 CSV (헤더 포함)
      - 각 row 에 e1..l4, prediction, probability 가 들어있을 수 있음
    출력:
      {
        "normal_png": "base64...",
        "abnormal_png": "base64...",
        "normal_count": ...,
        "abnormal_count": ...
      }
    """

    csv_text = sys.stdin.read()
    if not csv_text.strip():
        raise RuntimeError("STDIN CSV 가 비어 있습니다.")

    reader = csv.DictReader(io.StringIO(csv_text))
    rows = list(reader)
    if not rows:
        raise RuntimeError("CSV 데이터가 없습니다.")

    X = []
    existing_pred = []
    existing_prob = []

    for row in rows:
        X.append([
            float(row["e1"]),
            float(row["e2"]),
            float(row["e3"]),
            float(row["e4"]),
            float(row["l1"]),
            float(row["l2"]),
            float(row["l3"]),
            float(row["l4"]),
        ])

        p = (row.get("prediction") or "").strip()
        pr = (row.get("probability") or "").strip()
        existing_pred.append(p)
        existing_prob.append(pr)

    X = np.array(X, dtype=float)
    preds, scores, ae_error, xgb_prob = predict_new_data(X)

    normal_scores = []
    abnormal_scores = []

    for db_pred, db_prob, model_pred, model_score in zip(
            existing_pred, existing_prob, preds, scores):
        if db_pred in ("NORMAL", "ABNORMAL"):
            is_abnormal = (db_pred == "ABNORMAL")
            score_val = float(db_prob) if db_prob not in ("", None) else float(model_score)
        else:
            is_abnormal = bool(int(model_pred))
            score_val = float(model_score)

        if is_abnormal:
            abnormal_scores.append(score_val)
        else:
            normal_scores.append(score_val)

    normal_count = len(normal_scores)
    abnormal_count = len(abnormal_scores)

    def plot_hist(scores, title):
        fig, ax = plt.subplots(figsize=(4, 3))
        if scores:
            ax.hist(scores, bins=20)
        ax.set_title(title)
        ax.set_xlabel("score")
        ax.set_ylabel("count")
        buf = io.BytesIO()
        fig.tight_layout()
        fig.savefig(buf, format="png")
        plt.close(fig)
        return base64.b64encode(buf.getvalue()).decode("ascii")

    normal_png_b64 = plot_hist(normal_scores, "NORMAL")
    abnormal_png_b64 = plot_hist(abnormal_scores, "ABNORMAL")

    out = {
        "normal_png": normal_png_b64,
        "abnormal_png": abnormal_png_b64,
        "normal_count": normal_count,
        "abnormal_count": abnormal_count,
    }

    json.dump(out, sys.stdout, ensure_ascii=False)
    sys.stdout.write("\n")


# =========================================================
# 5. main: Spring ProcessBuilder 와의 인터페이스
#    - PredictService:  python3 predict.py --file <json> --scaler ... --model ...
#    - GraphService:    python3 predict.py --stdin-csv --plots-out-base64
# =========================================================
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--file", help="입력 JSON 경로 (PredictService용)")
    parser.add_argument("--stdin-csv", action="store_true", help="STDIN CSV 입력 (GraphService용)")
    parser.add_argument("--plots-out-base64", action="store_true", help="그래프 base64 출력 (GraphService용)")
    # 호환성용 dummy 인자 (자바에서 던지지만 파이썬에서는 사용하지 않음)
    parser.add_argument("--scaler")
    parser.add_argument("--model")

    args = parser.parse_args()

    # GraphService 모드 우선 처리
    if args.stdin_csv:
        handle_csv_from_stdin_and_plot()
        return

    # PredictService 모드
    if args.file:
        handle_json_file(args.file)
        return

    parser.error("반드시 --file 또는 --stdin-csv 중 하나를 지정해야 합니다.")


if __name__ == "__main__":
    main()
