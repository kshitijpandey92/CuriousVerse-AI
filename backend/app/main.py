import asyncio
import os
from typing import Any

import httpx
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

app = FastAPI(title="CuriousVerse AI API", version="2.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"
RETRYABLE_STATUS = {429, 500, 502, 503, 504}


class ScriptRequest(BaseModel):
    topic: str = Field(min_length=3, max_length=300)


@app.get("/")
async def root() -> dict[str, str]:
    return {"name": "CuriousVerse AI", "status": "online", "docs": "/docs"}


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok", "service": "CuriousVerse AI", "version": "2.0.0"}


def extract_text(payload: dict[str, Any]) -> str:
    chunks: list[str] = []
    for step in payload.get("steps", []):
        if step.get("type") == "model_output":
            for part in step.get("content", []):
                if part.get("type") == "text" and part.get("text"):
                    chunks.append(part["text"])
    if not chunks:
        output_text = payload.get("output_text")
        if isinstance(output_text, str):
            chunks.append(output_text)
    return "\n".join(chunks).strip()


@app.post("/script")
async def generate_script(data: ScriptRequest) -> dict[str, str]:
    api_key = os.getenv("GEMINI_API_KEY", "").strip()
    if not api_key:
        raise HTTPException(status_code=503, detail="Gemini API key is not configured on the backend.")

    model = os.getenv("GEMINI_MODEL", "gemini-3.8-flash").strip()
    prompt = f"""Create an engaging, original YouTube video script about: {data.topic}

Return clear sections:
1. Three clickable but truthful title options
2. A compelling 10-second hook
3. A narration script for approximately 2-3 minutes
4. Scene-by-scene visual direction
5. Suggested on-screen captions
6. A closing call to action

Write for a curious general audience. Do not fabricate facts, statistics, quotes, or sources.
Clearly qualify uncertainty and avoid presenting speculation as fact."""

    body = {"model": model, "input": prompt, "store": False}
    headers = {"x-goog-api-key": api_key, "Content-Type": "application/json"}
    last_message = "Gemini is temporarily unavailable. Please try again."

    async with httpx.AsyncClient(timeout=httpx.Timeout(90.0, connect=15.0)) as client:
        for attempt in range(3):
            try:
                response = await client.post(GEMINI_URL, headers=headers, json=body)
            except httpx.TimeoutException:
                last_message = "Gemini timed out. Please try again."
                if attempt < 2:
                    await asyncio.sleep(1.5 * (attempt + 1))
                    continue
                raise HTTPException(status_code=504, detail=last_message)
            except httpx.HTTPError:
                raise HTTPException(status_code=502, detail="Could not connect to Gemini. Check backend network access.")

            if response.status_code in RETRYABLE_STATUS:
                try:
                    last_message = response.json().get("error", {}).get("message", last_message)
                except (ValueError, AttributeError):
                    pass
                if attempt < 2:
                    await asyncio.sleep(1.5 * (attempt + 1))
                    continue
                status = 503 if response.status_code in {429, 503} else 502
                raise HTTPException(status_code=status, detail=f"Gemini is busy after 3 attempts: {last_message}")

            if response.is_error:
                try:
                    detail = response.json().get("error", {}).get("message", "Gemini rejected the request.")
                except (ValueError, AttributeError):
                    detail = "Gemini rejected the request."
                raise HTTPException(status_code=502, detail=detail)

            try:
                payload = response.json()
            except ValueError:
                raise HTTPException(status_code=502, detail="Gemini returned invalid JSON.")

            script = extract_text(payload)
            if not script:
                raise HTTPException(status_code=502, detail="Gemini returned no script text.")
            return {"topic": data.topic, "script": script, "model": model}

    raise HTTPException(status_code=503, detail=last_message)
