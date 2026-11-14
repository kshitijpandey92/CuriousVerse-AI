from fastapi import FastAPI
app = FastAPI(title="CuriousVerse AI")

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/script")
def script(data: dict):
    topic = data.get("topic", "Unknown topic")
    return {"topic": topic, "message": "Connect Gemini API to generate the full script."}
