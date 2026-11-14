import os

import httpx
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

app = FastAPI(title="CuriousVerse AI")

GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"
GEMINI_MODEL = "gemini-3.8-flash"


class ScriptRequest(BaseModel):
    topic: str = Field(min_length=3, max_length=300)


@app.get("/health")
def health():
    return {"status": "ok", "service": "CuriousVerse AI"}


@app.post("/script")
async def generate_script(data: ScriptRequest):
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        raise HTTPException(
            status_code=503,
            detail="Gemini API key is not configured on the backend.",
        )

    prompt = f"""
Create an engaging YouTube video script about: {data.topic}

Write for a curious general audience. Make it original, accurate, and easy to narrate.
Return these sections with clear headings:
1. Catchy YouTube title
2. Short hook (first 10 seconds)
3. Full narration script (about 2-3 minutes)
4. Suggested visual ideas for each major scene
5. Closing call to action

Do not invent facts, statistics, quotes, or sources. If a claim is uncertain, qualify it.
"""

    try:
        async with httpx.AsyncClient(timeout=90.0) as client:
            response = await client.post(
                GEMINI_URL,
                headers={
                    "x-goog-api-key": api_key,
                    "Content-Type": "application/json",
                },
                json={
                    "model": GEMINI_MODEL,
                    "input": prompt,
                    "store": False,
                },
            )
    except httpx.TimeoutException as exc:
        raise HTTPException(status_code=504, detail="Gemini took too long to respond. Please try again.") from exc
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail="Could not connect to Gemini.") from exc

    if response.is_error:
        # Do not return request headers or the API key to the client.
        try:
            error_message = response.json().get("error", {}).get("message", "Gemini request failed.")
        except ValueError:
            error_message = "Gemini request failed."
        raise HTTPException(status_code=502, detail=error_message)

    try:
        payload = response.json()
        text_parts = []
        for step in payload.get("steps", []):
            if step.get("type") == "model_output":
                for part in step.get("content", []):
                    if part.get("type") == "text" and part.get("text"):
                        text_parts.append(part["text"])
        script_text = "\\n".join(text_parts).strip()
    except (ValueError, AttributeError, TypeError) as exc:
        raise HTTPException(status_code=502, detail="Gemini returned an unreadable response.") from exc

    if not script_text:
        raise HTTPException(status_code=502, detail="Gemini did not return any script text.")

    return {"topic": data.topic, "script": script_text, "model": GEMINI_MODEL}
