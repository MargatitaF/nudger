import os
from dotenv import load_dotenv
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.jobstores.memory import MemoryJobStore
from apscheduler.jobstores.sqlalchemy import SQLAlchemyJobStore

load_dotenv()
DB_USER = os.getenv("DB_USER")
DB_PASSWORD = os.getenv("DB_PASSWORD") 
DB_HOST = os.getenv("DB_HOST")
DB_PORT = os.getenv("DB_PORT")
DB_NAME = os.getenv("DB_NAME")

# Define job stores
jobstores = {
    'default': MemoryJobStore(),  # In-memory store (no persistence)
}

# Only add persistent store if all DB variables are available
if all([DB_USER, DB_PASSWORD, DB_HOST, DB_PORT, DB_NAME]):
    try:
        jobstores['persistent'] = SQLAlchemyJobStore(url=f'postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}')
        print("PostgreSQL persistent job store configured successfully.")
    except Exception as e:
        print(f"Failed to configure PostgreSQL job store: {e}")
        print("Using only in-memory job store.")
else:
    print("Database configuration incomplete. Using only in-memory job store.")
    print(f"Missing: DB_USER={DB_USER}, DB_PASSWORD={'***' if DB_PASSWORD else None}, DB_HOST={DB_HOST}, DB_PORT={DB_PORT}, DB_NAME={DB_NAME}")

# Create scheduler with job stores configuration
scheduler = BackgroundScheduler(jobstores=jobstores)

def initialize_scheduler():
    return scheduler
