import os
from datetime import datetime, time
import uuid
import random

import uvicorn
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from pyfcm import FCMNotification
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session

from schema.models import NotificationRequest, SchedulingRequest, FrequencyType, TokenRegistrationRequest
from utils.scheduler import initialize_scheduler
from db import ScheduledNotification, Tones, TonePrompts, ToneEnum, TokenTonePreferences, Base

# Load environment variables
load_dotenv()

# Initialize FastAPI App
app = FastAPI(
    title="Nudger",
    description="Welcome to Nudger! This API allows you to register device tokens and send push notifications."
)

# Mount static files for mood images
app.mount("/moods", StaticFiles(directory="moods"), name="moods")

# Configure CORS
origins = [
    "http://localhost:3000",  # Default Reflex frontend port
    "http://127.0.0.1:3000", # Also allow this variant
    # Add any other origins if needed (e.g., deployed frontend URL)
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Database configuration
DB_USER = os.getenv("DB_USER")
DB_PASSWORD = os.getenv("DB_PASSWORD")
DB_HOST = os.getenv("DB_HOST")
DB_PORT = os.getenv("DB_PORT")
DB_NAME = os.getenv("DB_NAME")

DATABASE_URL = f"postgresql+psycopg2://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
engine = create_engine(DATABASE_URL, echo=False)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def get_db():
    """Dependency to get database session."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@app.on_event("startup")
async def startup_event():
    """Start the scheduler when the FastAPI app starts."""
    if not scheduler.running:
        scheduler.start()
        print("APScheduler started on app startup.")

@app.on_event("shutdown") 
async def shutdown_event():
    """Shutdown the scheduler when the FastAPI app shuts down."""
    if scheduler.running:
        scheduler.shutdown()
        print("APScheduler shut down.")

fcm_service_account_file = os.getenv("FCM_SERVICE_ACCOUNT_FILE")
fcm_project_id = os.getenv("FCM_PROJECT_ID")

# Temporary solution:
# In-memory storage for registered tokens
registered_tokens = set()

# We'll store token-tone preferences directly in a simple table or use a workaround
# For now, we'll create a simple token-based storage approach

# Initialize scheduler
scheduler = initialize_scheduler()

try:
    fcm = FCMNotification(service_account_file=fcm_service_account_file, project_id=fcm_project_id)
except Exception as e:
    print(f"Error initializing FCM. Ensure '{fcm_service_account_file}' is correct and project_id is set if needed: {e}")
    fcm = None


def send_scheduled_notification(token: str, title: str, body: str = "Scheduled notification"):
    """
    Job function that sends a scheduled notification.
    This function will be called by APScheduler at the scheduled time.
    Now uses tone-based prompts from the database based on user's tone preference.
    """
    if not fcm:
        print(f"FCM not initialized, cannot send scheduled notification to {token}")
        return
    
    try:
        print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] Sending scheduled notification to {token}")
          # Get a tone-based prompt based on user's preference
        with SessionLocal() as db:
            try:
                # Get user's tone preference from database, default to neutral (tone_id=2)
                token_pref = db.query(TokenTonePreferences).filter(TokenTonePreferences.token == token).first()
                tone_id = token_pref.tone_id if token_pref else 2  # Default to neutral
                
                prompts = db.query(TonePrompts).filter(TonePrompts.tone_id == tone_id).all()
                
                if prompts:
                    random_prompt = random.choice(prompts)
                    notification_body = random_prompt.prompt
                else:
                    # Fallback to neutral tone if no prompts found for user's tone
                    neutral_prompts = db.query(TonePrompts).filter(TonePrompts.tone_id == 2).all()
                    if neutral_prompts:
                        random_prompt = random.choice(neutral_prompts)
                        notification_body = random_prompt.prompt
                    else:
                        notification_body = f"Scheduled reminder: {title}"  # Final fallback
                    
            except Exception as db_error:
                print(f"Error fetching tone prompt: {db_error}")
                notification_body = f"Scheduled reminder: {title}"  # Fallback
        
        print(f"Title: {title}, Body: {notification_body}")
        
        result = fcm.notify(
            fcm_token=token,
            notification_title=title,
            notification_body=notification_body
        )
        
        print(f"Scheduled notification result: {result}")
        
    except Exception as e:
        print(f"Error sending scheduled notification: {e}")


@app.get("/")
async def main():
    return {"message": "Welcome to Nudger API"}

# =========== Token Registration and Management ===========

@app.post("/register-token")
async def register_token(request: TokenRegistrationRequest):
    """
    Endpoint to register a device token for notifications.
    Tokens are stored in an in-memory set.
    """
    if not request.token:
        raise HTTPException(status_code=400, detail="Token is required")
    
    registered_tokens.add(request.token)
    print(f"Registered token: {request.token}. Total tokens: {len(registered_tokens)}")
    return {"message": "Token registered successfully"}

@app.get("/registered-tokens")
async def get_registered_tokens():
    """
    Endpoint to retrieve all registered device tokens.
    Mainly for debugging and verification.
    """
    return {"tokens": list(registered_tokens)}

# =========== Notification Sending ===========

@app.post("/send-notification")
async def send_notification(notification: NotificationRequest):
    """
    Endpoint to send a push notification.
    """
    if not fcm:
        raise HTTPException(status_code=503, detail="FCM service is not initialized. Check server logs.")
    if not notification.token:
        raise HTTPException(status_code=400, detail="Token is required")
    if not notification.title:
        raise HTTPException(status_code=400, detail="Title is required")
    if not notification.body:
        raise HTTPException(status_code=400, detail="Body is required")

    try:
        print(f"Attempting to send notification to token: {notification.token}")
        print(f"Title: {notification.title}, Body: {notification.body}, Image: {notification.image_url}")
        
        result = fcm.notify(
            fcm_token=notification.token,
            notification_title=notification.title,
            notification_body=notification.body,
            notification_image=notification.image_url  # Optional: can be None or empty
        )
        
        print(f"FCM Raw Result: {result}")

        # Check for success indicators
        if result.get("message_id") or result.get("name"):
            return {"message": "Notification sent successfully", "details": result}
        if result.get("success") is True or (isinstance(result.get("success"), int) and result.get("success") > 0):
            return {"message": "Notification sent successfully", "details": result}

        # Check for failure indicators
        error_message = "Failed to send notification"
        if result.get("failure") is True or (isinstance(result.get("failure"), int) and result.get("failure") > 0):
            if result.get("results") and isinstance(result["results"], list) and result["results"]:
                error_detail = result["results"][0].get("error", "Unknown FCM error from results array")
            else:
                error_detail = result.get("error", "Unknown FCM error (failure flag/count was positive)")
            error_message = f"{error_message}: {error_detail}"
            print(f"FCM Failure: {error_detail}")
            raise HTTPException(status_code=500, detail=error_message)
        
        if result.get("error"):
            error_detail = result.get("error")
            error_message = f"{error_message}: {error_detail}"
            print(f"FCM Error: {error_detail}")
            raise HTTPException(status_code=500, detail=error_message)        # Fallback for unexpected result structure if no clear success/failure
        print(f"FCM Result (unexpected structure): {result}")
        raise HTTPException(status_code=500, detail=f"{error_message} due to an unexpected FCM response structure. Full response: {result}")

    except HTTPException: # Re-raise HTTPExceptions directly
        raise
    except Exception as e:
        print(f"Error sending notification: {e}")
        raise HTTPException(status_code=500, detail=f"An unexpected error occurred: {str(e)}")


@app.post("/schedule-notification")
async def schedule_notification(request: SchedulingRequest, db: Session = Depends(get_db)):
    """
    Endpoint to schedule a notification based on the given parameters.
    Saves to both APScheduler (for execution) and database (for persistence).
    """
    if not fcm:
        raise HTTPException(status_code=503, detail="FCM service is not initialized. Check server logs.")
    
    try:
        # Parse the time
        hour, minute = map(int, request.time.split(':'))
        
        # Generate a unique job ID
        job_id = f"scheduled_{request.token[:8]}_{uuid.uuid4().hex[:8]}"
        
        # Determine the job parameters based on frequency
        job_kwargs = {
            'func': send_scheduled_notification,
            'args': [request.token, request.title, f"Scheduled reminder: {request.title}"],
            'id': job_id,
            'jobstore': 'persistent',  # Use persistent storage
            'replace_existing': False
        }
        
        # Parse end_date if provided
        end_date = None
        if request.end_date:
            end_date = datetime.strptime(request.end_date, '%d-%m-%Y').date()
            job_kwargs['end_date'] = end_date
        
        # Schedule based on frequency type
        if request.frequency == FrequencyType.ONCE:
            # Schedule for today at the specified time, or tomorrow if time has passed
            now = datetime.now()
            target_time = datetime.combine(now.date(), time(hour, minute))
            
            if target_time <= now:
                # If the time has already passed today, schedule for tomorrow
                target_time = target_time.replace(day=target_time.day + 1)
            
            scheduler.add_job(
                trigger='date',
                run_date=target_time,
                **job_kwargs
            )
            
        elif request.frequency == FrequencyType.DAILY:
            scheduler.add_job(
                trigger='cron',
                hour=hour,
                minute=minute,
                **job_kwargs
            )
            
        elif request.frequency == FrequencyType.WEEKLY:
            if not request.day_of_week:
                raise HTTPException(status_code=400, detail="day_of_week is required for weekly frequency")
            
            # Convert 1-7 to 0-6 (Monday=0, Sunday=6)
            day_of_week = (request.day_of_week - 1) % 7
            
            scheduler.add_job(
                trigger='cron',
                day_of_week=day_of_week,
                hour=hour,
                minute=minute,
                **job_kwargs
            )
            
        elif request.frequency == FrequencyType.MONTHLY:
            if not request.day_of_month:
                raise HTTPException(status_code=400, detail="day_of_month is required for monthly frequency")
            
            scheduler.add_job(
                trigger='cron',
                day=request.day_of_month,
                hour=hour,
                minute=minute,
                **job_kwargs
            )
            
        elif request.frequency == FrequencyType.WEEKDAYS:
            scheduler.add_job(
                trigger='cron',
                day_of_week='mon-fri',
                hour=hour,
                minute=minute,
                **job_kwargs
            )
            
        elif request.frequency == FrequencyType.WEEKENDS:
            scheduler.add_job(
                trigger='cron',
                day_of_week='sat,sun',
                hour=hour,
                minute=minute,
                **job_kwargs
            )
        else:
            raise HTTPException(status_code=400, detail=f"Unsupported frequency type: {request.frequency}")
        
        # Start the scheduler if it's not already running
        if not scheduler.running:
            scheduler.start()
        
        # Save to database for persistence
        try:
            scheduled_notif = ScheduledNotification(
                token=request.token,
                title=request.title,
                time=request.time,
                frequency=request.frequency,
                days_of_week=str(request.day_of_week) if request.day_of_week else None,
                day_of_month=request.day_of_month,
                end_date=request.end_date,
                job_id=job_id
            )
            db.add(scheduled_notif)
            db.commit()
            db.refresh(scheduled_notif)
            print(f"Saved notification to database with ID: {scheduled_notif.id}")
        except Exception as db_error:
            print(f"Error saving to database: {db_error}")
            db.rollback()
            # Continue execution - APScheduler job is still scheduled
        
        print(f"Scheduled notification job '{job_id}' for token {request.token[:8]}... at {request.time}")
        print(f"Frequency: {request.frequency}, End date: {request.end_date}")
        return {
            "message": "Notification scheduled successfully",
            "job_id": job_id,
            "frequency": request.frequency,
            "time": request.time,
            "day_of_week": request.day_of_week,
            "day_of_month": request.day_of_month,
            "end_date": request.end_date
        }
    except ValueError as e:
        if "time" in str(e).lower():
            raise HTTPException(status_code=400, detail="Invalid time format. Use HH:MM format.")
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        print(f"Error scheduling notification: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to schedule notification: {str(e)}")

# =========== Job Retrieval and Management ===========

@app.get("/get-notifications/{token}")
async def get_notifications(token: str, db: Session = Depends(get_db)):
    """
    Endpoint to retrieve all scheduled notifications for a specific token.
    """
    try:
        notifications = db.query(ScheduledNotification).filter(
            ScheduledNotification.token == token
        ).order_by(ScheduledNotification.created_at.desc()).all()
        
        result = []
        for notif in notifications:
            result.append({
                "id": notif.id,
                "title": notif.title,
                "time": notif.time,
                "frequency": notif.frequency.value,
                "day_of_week": int(notif.days_of_week) if notif.days_of_week and notif.days_of_week.isdigit() else None,
                "day_of_month": notif.day_of_month,
                "end_date": notif.end_date,
                "job_id": notif.job_id,
                "created_at": notif.created_at.isoformat() if notif.created_at else None
            })
        
        return {
            "notifications": result,
            "count": len(result)
        }
        
    except Exception as e:
        print(f"Error retrieving notifications: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to retrieve notifications: {str(e)}")


@app.get("/scheduled-jobs")
async def get_scheduled_jobs():
    """
    Endpoint to retrieve all scheduled jobs.
    Mainly for debugging and verification.
    """
    try:
        jobs = scheduler.get_jobs()
        job_list = []
        
        for job in jobs:
            job_info = {
                "id": job.id,
                "name": job.name,
                "trigger": str(job.trigger),
                "next_run_time": job.next_run_time.isoformat() if job.next_run_time else None,
                "jobstore": job._jobstore_alias if hasattr(job, '_jobstore_alias') else 'unknown'
            }
            job_list.append(job_info)
        
        return {"jobs": job_list, "total_jobs": len(job_list)}
        
    except Exception as e:
        print(f"Error retrieving scheduled jobs: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to retrieve scheduled jobs: {str(e)}")


@app.delete("/scheduled-jobs/{job_id}")
async def cancel_scheduled_job(job_id: str, db: Session = Depends(get_db)):
    """
    Endpoint to cancel a scheduled job by its ID.
    Removes from both APScheduler and database.
    """
    try:
        # Remove from APScheduler
        scheduler.remove_job(job_id)
        print(f"Cancelled scheduled job from APScheduler: {job_id}")
        
        # Remove from database
        db_notification = db.query(ScheduledNotification).filter(
            ScheduledNotification.job_id == job_id
        ).first()
        
        if db_notification:
            db.delete(db_notification)
            db.commit()
            print(f"Removed notification from database: ID {db_notification.id}")
        else:
            print(f"No database record found for job_id: {job_id}")
        
        return {"message": f"Job '{job_id}' cancelled successfully"}
        
    except Exception as e:
        print(f"Error cancelling job {job_id}: {e}")
        db.rollback()
        raise HTTPException(status_code=404, detail=f"Job '{job_id}' not found or could not be cancelled")

# =========== Tone Management ===========

@app.get("/tones")
async def get_tones(db: Session = Depends(get_db)):
    """
    Endpoint to retrieve all available tones.
    """
    try:
        tones = db.query(Tones).all()
        return {
            "tones": [
                {
                    "tone_id": tone.tone_id,
                    "tone_name": tone.tone_name.value,
                    "display_name": tone.tone_name.value.capitalize(),
                    "image_url": f"/moods/{tone.tone_name.value}.png"
                } 
                for tone in tones
            ]
        }
    except Exception as e:
        print(f"Error retrieving tones: {e}")
        raise HTTPException(status_code=500, detail="Failed to retrieve tones")

@app.get("/user-tone/{token}")
async def get_user_tone(token: str, db: Session = Depends(get_db)):
    """
    Endpoint to retrieve the tone preference for a specific FCM token.
    Since we don't have users yet, we'll store tone preferences per token.
    Default to neutral tone (tone_id=2) if no preference is set.
    """
    try:
        # Check if this token has a stored preference in database
        token_pref = db.query(TokenTonePreferences).filter(TokenTonePreferences.token == token).first()
        
        if token_pref:
            # Get the tone from database by ID
            tone = db.query(Tones).filter(Tones.tone_id == token_pref.tone_id).first()
            if tone:
                return {
                    "token": token,
                    "tone": tone.tone_name.value,
                    "tone_id": tone.tone_id,
                    "is_default": False
                }
        
        # Default to neutral tone if no preference is set
        neutral_tone = db.query(Tones).filter(Tones.tone_name == ToneEnum.neutral).first()
        if not neutral_tone:
            raise HTTPException(status_code=500, detail="Default neutral tone not found in database")
        
        return {
            "token": token,
            "tone": neutral_tone.tone_name.value,
            "tone_id": neutral_tone.tone_id,
            "is_default": True
        }
    except Exception as e:
        print(f"Error retrieving user tone: {e}")
        raise HTTPException(status_code=500, detail="Failed to retrieve user tone")

@app.post("/user-tone")
async def set_user_tone(request: dict, db: Session = Depends(get_db)):
    """
    Endpoint to set the tone preference for a specific FCM token.
    Expected request body: {"token": "fcm_token", "tone_id": 1}
    """
    try:
        token = request.get("token")
        tone_id = request.get("tone_id")
        
        if not token:
            raise HTTPException(status_code=400, detail="Token is required")
        if not tone_id:
            raise HTTPException(status_code=400, detail="tone_id is required")
        
        # Validate that the tone exists
        tone = db.query(Tones).filter(Tones.tone_id == tone_id).first()
        if not tone:
            raise HTTPException(status_code=404, detail="Tone not found")
        
        # Check if preference already exists for this token
        existing_pref = db.query(TokenTonePreferences).filter(TokenTonePreferences.token == token).first()
        if existing_pref:
            # Update existing preference
            existing_pref.tone_id = tone_id
            existing_pref.updated_at = datetime.now()
        else:
            # Create new preference
            new_pref = TokenTonePreferences(token=token, tone_id=tone_id)
            db.add(new_pref)
        
        db.commit()
        
        return {
            "message": "Tone preference updated successfully",
            "token": token,
            "tone": tone.tone_name.value,
            "tone_id": tone.tone_id
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error setting user tone: {e}")
        db.rollback()
        raise HTTPException(status_code=500, detail="Failed to set user tone")

@app.get("/tone-prompts/{tone_id}")
async def get_tone_prompts(tone_id: int, db: Session = Depends(get_db)):
    """
    Endpoint to retrieve all prompts for a specific tone.
    """
    try:
        prompts = db.query(TonePrompts).filter(TonePrompts.tone_id == tone_id).all()
        if not prompts:
            raise HTTPException(status_code=404, detail="No prompts found for this tone")
        
        return {
            "tone_id": tone_id,
            "prompts": [
                {
                    "prompt_id": prompt.prompt_id,
                    "text": prompt.prompt
                }
                for prompt in prompts
            ]
        }
    except Exception as e:
        print(f"Error retrieving tone prompts: {e}")
        raise HTTPException(status_code=500, detail="Failed to retrieve tone prompts")

@app.get("/random-tone-prompt/{tone_id}")
async def get_random_tone_prompt(tone_id: int, db: Session = Depends(get_db)):
    """
    Endpoint to get a random prompt for a specific tone.
    This will be used by the notification scheduler.
    """
    try:
        prompts = db.query(TonePrompts).filter(TonePrompts.tone_id == tone_id).all()
        if not prompts:
            # Fallback to neutral tone if no prompts found
            neutral_tone = db.query(Tones).filter(Tones.tone_name == ToneEnum.neutral).first()
            if neutral_tone:
                prompts = db.query(TonePrompts).filter(TonePrompts.tone_id == neutral_tone.tone_id).all()
        
        if not prompts:
            return {"prompt": "Reminder: Check your goals today!"}  # Fallback message
        
        random_prompt = random.choice(prompts)
        return {
            "tone_id": tone_id,
            "prompt": random_prompt.prompt
        }
    except Exception as e:
        print(f"Error retrieving random tone prompt: {e}")
        return {"prompt": "Reminder: Check your goals today!"}  # Fallback message

# =========== Application Startup ===========

if __name__ == "__main__":
    # Start the scheduler when running the app
    if not scheduler.running:
        scheduler.start()
        print("APScheduler started.")
    
    uvicorn.run(app, host="0.0.0.0", port=8000)
