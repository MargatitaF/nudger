# Standard imports
import enum
import os

# Third-party imports
import sqlalchemy as sa
from sqlalchemy import (
    create_engine, MetaData, Table, Column, Integer, String, Text, Numeric,
    Time, Boolean, Date, ForeignKey, Index, UniqueConstraint, Enum as SaEnum
)
from sqlalchemy.orm import relationship, declarative_base, sessionmaker
from sqlalchemy.sql import func

# Import FrequencyType from models.py for scheduled notifications
from schema.models import FrequencyType

Base = declarative_base() # Base class for declarative models

class ToneEnum(enum.Enum):
    """Enum for tone of the notification."""
    caring = "caring"
    assertive = "assertive"
    neutral = "neutral"
    encouraging = "encouraging"

class User(Base):
    """User model for the database."""
    __tablename__ = 'users'
    user_id = Column(Integer, primary_key=True)
    first_name = Column(String(50), nullable=False)
    last_name = Column(String(50), nullable=False)
    user_notif = relationship("NotificationPreferences", backref = 'user')

    def __repr__(self):
        return f"User(user_id={self.user_id}, first_name='{self.first_name}', last_name='{self.last_name}')"

class NotificationPreferences(Base):
    """Notification preferences model for the database."""
    __tablename__ = 'notification_preferences'
    notif_id = Column(Integer, primary_key=True)
    notif_time = Column(Time, nullable=False)
    user_id = Column(Integer, ForeignKey('users.user_id'), nullable=False)

    def __repr__(self):
        return f"NotificationPreferences(notif_id={self.notif_id}, notif_time='{self.notif_time}', user_id={self.user_id})"

class Tones(Base):
    """Tone description model for the database."""
    __tablename__ = 'tones'
    tone_id = Column(Integer, primary_key=True)
    tone_name = Column(SaEnum(ToneEnum, name='tone_enum', create_type=True), nullable=False)
    def __repr__(self):
        return f"Tones(tone_id={self.tone_id}, tone_name='{self.tone_name}')"

class TokenTonePreferences(Base):
    """Token-based tone preferences model for the database."""
    __tablename__ = 'token_tone_preferences'
    
    token = Column(String(255), primary_key=True, index=True)  # FCM token
    tone_id = Column(Integer, ForeignKey('tones.tone_id'), nullable=False)
    created_at = Column(sa.DateTime(timezone=True), nullable=False, server_default=func.now())
    updated_at = Column(sa.DateTime(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now())

    def __repr__(self):
        return f"TokenTonePreferences(token='{self.token[:8]}...', tone_id={self.tone_id})"

class ScheduledNotification(Base):
    """Scheduled notification model for the database."""
    __tablename__ = 'scheduled_notifications'
    
    id = Column(Integer, primary_key=True)
    token = Column(String(255), nullable=False, index=True)  # FCM token
    title = Column(String(255), nullable=False)
    time = Column(String(10), nullable=False)  # HH:MM format
    frequency = Column(SaEnum(FrequencyType, name='frequency_type_enum', create_type=True), nullable=False)
    days_of_week = Column(String(20), nullable=True)  # Comma-separated string like "1,3,5" for Mon,Wed,Fri
    day_of_month = Column(Integer, nullable=True)  # For monthly frequency
    end_date = Column(String(20), nullable=True)  # DD-MM-YYYY format
    job_id = Column(String(255), nullable=True, unique=True)  # APScheduler job ID
    created_at = Column(sa.DateTime(timezone=True), nullable=False, server_default=func.now())
    updated_at = Column(sa.DateTime(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now())
    
    def __repr__(self):
        return f"ScheduledNotification(id={self.id}, title='{self.title}', frequency='{self.frequency.value}', token='{self.token[:8]}...')"

class TonePrompts(Base):
    """Tone prompts model for the database."""
    __tablename__ = 'tone_prompts'
    
    prompt_id = Column(Integer, primary_key=True)
    prompt = Column(Text, nullable=False)
    tone_id = Column(Integer, ForeignKey('tones.tone_id'), nullable=False)
    created_at = Column(sa.DateTime(timezone=True), nullable=False, server_default=func.now())
    updated_at = Column(sa.DateTime(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now())
    
    def __repr__(self):
        return f"TonePrompts(prompt_id={self.prompt_id}, prompt='{self.prompt[:30]}...', tone_id={self.tone_id})"

# --- Example Usage (PostgreSQL) ---
if __name__ == '__main__':

    DB_USER = os.getenv("DB_USER")
    DB_PASSWORD = os.getenv("DB_PASSWORD")
    DB_HOST = os.getenv("DB_HOST")
    DB_PORT = os.getenv("DB_PORT")
    DB_NAME = os.getenv("DB_NAME")

    DATABASE_URL = f"postgresql+psycopg2://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

    engine = create_engine(DATABASE_URL, echo=False) # Set echo=True to see SQL

    print("Connecting to PostgreSQL and creating tables/ENUM types (if they don't exist)...")
    # Issue CREATE TYPE ... AS ENUM and CREATE TABLE statements
    Base.metadata.create_all(engine)
    print("Tables and ENUM types checked/created.")

    # Example of creating a session to interact with the DB
    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    db = SessionLocal()
    try:
        # Example: Create a user and an account using the enum
        # Check if user exists
        user = db.query(User).filter(User.first_name == "Test", User.last_name == "Tester").first()
        if not user:
            print("Creating test user...")
            user = User(first_name="Test", last_name="Tester")
            db.add(user)
            db.commit() # Commit to get user_id
            db.refresh(user)
            print(f"User created with ID: {user.user_id}")
        else:
            print(f"User Test Tester already exists with ID: {user.user_id}")

        # Example: Query the number of users
        user_count = db.query(User).count()
        print(f"Number of users in the database: {user_count}")

    except Exception as e:
        print(f"An error occurred: {e}")
        db.rollback()
    finally:
        db.close()
        print("Database session closed.")

