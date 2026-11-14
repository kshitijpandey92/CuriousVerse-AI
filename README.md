# CuriousVerse AI

A premium Android creator-studio interface with a FastAPI backend for Gemini-powered YouTube script generation.

## Current working features
- Native Android app built with Kotlin and Jetpack Compose
- Premium dark creator-studio interface with Studio, Projects, Tools and Settings navigation
- Backend URL configuration saved locally on the device
- Android-to-FastAPI script generation request
- Gemini API key remains server-side in the `GEMINI_API_KEY` environment variable
- Backend health endpoint, input validation, readable errors and bounded retries for temporary Gemini capacity errors
- GitHub Actions builds the Android APK and runs backend tests

## Backend setup
From the `backend` directory:

```bash
python -m pip install -r requirements.txt
export GEMINI_API_KEY="your-key"
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Never commit your API key. Configure it as an environment secret in your hosting provider.

## Android setup
Install the APK artifact from a successful GitHub Actions run. Open Settings in the app and enter your deployed backend base URL (for example, `https://your-service.example.com`).

A Codespaces forwarded URL is temporary and should only be used for development.

## Deployment
The backend includes a Dockerfile. Deploy the `backend` directory as a Docker service and add `GEMINI_API_KEY` as a private environment variable. Optional: set `GEMINI_MODEL` (default: `gemini-3.8-flash`).

## Roadmap (not yet implemented)
- Narration generation and voice selection
- FFmpeg video assembly, subtitles and background audio
- Thumbnail generation and editing
- Google OAuth and YouTube upload
- Project persistence and publishing history

These roadmap items are not advertised as working until implemented and tested.
