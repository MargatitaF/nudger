from pydantic import BaseModel, Field, field_validator
from typing import Optional
from enum import Enum

class FrequencyType(str, Enum):
    """Enumeration for notification frequency types."""
    ONCE = "once"
    DAILY = "daily" 
    WEEKLY = "weekly"
    MONTHLY = "monthly"  # Occurs once per month
    WEEKDAYS = "weekdays"  # Mon-Fri
    WEEKENDS = "weekends"  # Sat-Sun

class DayOfWeek(str, Enum):
    """Enumeration for days of the week."""
    MONDAY = "monday"
    TUESDAY = "tuesday"
    WEDNESDAY = "wednesday"
    THURSDAY = "thursday"
    FRIDAY = "friday"
    SATURDAY = "saturday"
    SUNDAY = "sunday"


class NotificationRequest(BaseModel):
    """Model for sending a push notification."""
    token: str
    title: str
    body: str
    image_url: Optional[str] = None

class TokenRegistrationRequest(BaseModel):
    """Model for registering a device token."""
    token: str

class SchedulingRequest(BaseModel):
    """Model for scheduling a notification."""
    token: str
    title: str
    time: str = Field(..., description="Time in HH:MM format (24-hour)")
    frequency: FrequencyType
    day_of_week: Optional[int] = Field(None, ge=1, le=7, description="Day of week for weekly frequency (1-7)")  # For weekly frequency
    day_of_month: Optional[int] = Field(None, ge=1, le=31, description="Day of month for monthly frequency (1-31)")  # For monthly frequency
    end_date: Optional[str] = None  # End date for recurring notifications (DD-MM-YYYY format)
    
    @field_validator('time')
    def validate_time_format(cls, v):
        try:
            # Validate HH:MM format
            time_parts = v.split(':')
            if len(time_parts) != 2:
                raise ValueError('Time must be in HH:MM format')
            hour, minute = int(time_parts[0]), int(time_parts[1])
            if not (0 <= hour <= 23) or not (0 <= minute <= 59):
                raise ValueError('Invalid time values')
            return v
        except (ValueError, AttributeError):
            raise ValueError('Time must be in HH:MM format (e.g., "14:30")')
    
    @field_validator('end_date')
    def validate_end_date_format(cls, v):
        if v is None:
            return v
        try:
            from datetime import datetime
            datetime.strptime(v, '%d-%m-%Y')
            return v
        except ValueError:
            raise ValueError('End date must be in DD-MM-YYYY format (e.g., "31-12-2025")')
